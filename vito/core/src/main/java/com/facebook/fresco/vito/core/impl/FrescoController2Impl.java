/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.fresco.middleware.MiddlewareUtils;
import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import com.facebook.fresco.vito.core.DrawableDataSubscriber;
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.FrescoVitoConfig;
import com.facebook.fresco.vito.core.Hierarcher;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.core.VitoUtils;
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayFactory2;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.CloseableImage;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

public class FrescoController2Impl implements DrawableDataSubscriber, FrescoController2 {

  private static final Map<String, Object> COMPONENT_EXTRAS =
      ImmutableMap.<String, Object>of("component_tag", "vito2");
  private static final Map<String, Object> SHORTCUT_EXTRAS =
      ImmutableMap.<String, Object>of("origin", "memory_bitmap", "origin_sub", "shortcut");
  private static final Extras ON_SUBMIT_EXTRAS = Extras.of(COMPONENT_EXTRAS);

  private final FrescoVitoConfig mConfig;
  private final Hierarcher mHierarcher;
  private final Executor mLightweightBackgroundThreadExecutor;
  private final Executor mUiThreadExecutor;
  private final VitoImagePipeline mImagePipeline;
  private final @Nullable VitoImageRequestListener mGlobalImageListener;
  private final DebugOverlayFactory2 mDebugOverlayFactory;

  public FrescoController2Impl(
      FrescoVitoConfig config,
      Hierarcher hierarcher,
      Executor lightweightBackgroundThreadExecutor,
      Executor uiThreadExecutor,
      VitoImagePipeline imagePipeline,
      @Nullable VitoImageRequestListener globalImageListener,
      DebugOverlayFactory2 debugOverlayFactory) {
    mConfig = config;
    mHierarcher = hierarcher;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mUiThreadExecutor = uiThreadExecutor;
    mImagePipeline = imagePipeline;
    mGlobalImageListener = globalImageListener;
    mDebugOverlayFactory = debugOverlayFactory;
  }

  @Override
  public FrescoDrawable2 createDrawable() {
    return new FrescoDrawable2Impl();
  }

  @Override
  public boolean fetch(
      final FrescoDrawable2 frescoDrawable,
      final VitoImageRequest imageRequest,
      final @Nullable Object callerContext,
      final @Nullable ImageListener listener,
      final @Nullable FadeDrawable.OnFadeListener onFadeListener,
      final @Nullable Rect viewportDimensions) {
    // Save viewport dimension for future use
    frescoDrawable.setViewportDimensions(viewportDimensions);

    // Check if we already fetched the image
    if (frescoDrawable.getDrawableDataSubscriber() == this
        && frescoDrawable.isFetchSubmitted()
        && imageRequest.equals(frescoDrawable.getImageRequest())) {
      frescoDrawable.cancelReleaseNextFrame();
      frescoDrawable.cancelReleaseDelayed();
      return true; // already set
    }
    // We didn't -> Reset everything
    frescoDrawable.close();
    // Basic setup
    frescoDrawable.setDrawableDataSubscriber(this);
    frescoDrawable.setImageRequest(imageRequest);
    frescoDrawable.setCallerContext(callerContext);
    frescoDrawable.setImageListener(listener);

    frescoDrawable.setVitoImageRequestListener(mGlobalImageListener);

    frescoDrawable.setOnFadeListener(onFadeListener);

    // Set layers that are always visible
    mHierarcher.setupOverlayDrawable(
        frescoDrawable, imageRequest.resources, imageRequest.imageOptions, null);

    // We're fetching a new image, so we're updating the ID
    final long imageId = VitoUtils.generateIdentifier();
    frescoDrawable.setImageId(imageId);

    Extras extras = obtainExtras(null, null, frescoDrawable);

    // Notify listeners that we're about to fetch an image
    frescoDrawable.getImageListener().onSubmit(imageId, imageRequest, callerContext, extras);

    // Check if the image is in cache
    CloseableReference<CloseableImage> cachedImage = mImagePipeline.getCachedImage(imageRequest);
    try {
      if (CloseableReference.isValid(cachedImage)) {
        frescoDrawable.setImageOrigin(ImageOrigin.MEMORY_BITMAP_SHORTCUT);
        // Immediately display the actual image.
        setActualImage(frescoDrawable, imageRequest, cachedImage, true, null);
        frescoDrawable.setFetchSubmitted(true);
        mDebugOverlayFactory.update(frescoDrawable, extras);
        return true;
      }
    } finally {
      CloseableReference.closeSafely(cachedImage);
    }

    // The image is not in cache -> Set up layers visible until the image is available
    frescoDrawable.setProgressDrawable(
        mHierarcher.buildProgressDrawable(imageRequest.resources, imageRequest.imageOptions));
    // Immediately show the progress image and set progress to 0
    frescoDrawable.setProgress(0f);
    frescoDrawable.showProgressImmediately();
    Drawable placeholder =
        mHierarcher.buildPlaceholderDrawable(imageRequest.resources, imageRequest.imageOptions);
    frescoDrawable.setPlaceholderDrawable(placeholder);
    frescoDrawable.setImageDrawable(null);

    frescoDrawable.getImageListener().onPlaceholderSet(imageId, imageRequest, placeholder);

    // Fetch the image
    final Runnable fetchRunnable =
        new Runnable() {
          @Override
          public void run() {
            if (imageId != frescoDrawable.getImageId()) {
              return; // We're trying to load a different image -> ignore
            }
            DataSource<CloseableReference<CloseableImage>> dataSource =
                mImagePipeline.fetchDecodedImage(
                    imageRequest, callerContext, frescoDrawable.getImageOriginListener(), imageId);
            frescoDrawable.setDataSource(dataSource);
            dataSource.subscribe(frescoDrawable, mUiThreadExecutor);
          }
        };

    if (mConfig.submitFetchOnBgThread()) {
      mLightweightBackgroundThreadExecutor.execute(fetchRunnable);
    } else {
      fetchRunnable.run();
    }
    frescoDrawable.setFetchSubmitted(true);

    mDebugOverlayFactory.update(frescoDrawable, null);

    return false;
  }

  @Override
  public void releaseDelayed(final FrescoDrawable2 drawable) {
    drawable.scheduleReleaseDelayed();
  }

  @Override
  public void release(final FrescoDrawable2 drawable) {
    drawable.scheduleReleaseNextFrame();
  }

  private void setActualImage(
      FrescoDrawable2 drawable,
      VitoImageRequest imageRequest,
      CloseableReference<CloseableImage> image,
      boolean isImmediate,
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {
    mHierarcher.setupActualImageWrapper(
        drawable.getActualImageWrapper(), imageRequest.imageOptions, drawable.getCallerContext());
    Drawable actualDrawable =
        mHierarcher.setupActualImageDrawable(
            drawable,
            imageRequest.resources,
            imageRequest.imageOptions,
            drawable.getCallerContext(),
            image,
            drawable.getActualImageWrapper(),
            isImmediate,
            null);
    if (imageRequest.imageOptions.shouldAutoPlay() && actualDrawable instanceof AnimatedDrawable2) {
      ((AnimatedDrawable2) actualDrawable).start();
    }
    Extras extras = obtainExtras(dataSource, image, drawable);
    drawable
        .getImageListener()
        .onFinalImageSet(
            drawable.getImageId(),
            drawable.getImageRequest(),
            drawable.getImageOrigin(),
            image.get(),
            extras,
            actualDrawable);
    float progress = 1f;
    if (dataSource != null && !dataSource.isFinished()) {
      progress = dataSource.getProgress();
    }
    drawable.setProgress(progress);
    mDebugOverlayFactory.update(drawable, extras);
  }

  @Override
  public void onNewResult(
      FrescoDrawable2 drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource == null || !dataSource.hasResult()) {
      return;
    }

    CloseableReference<CloseableImage> image = dataSource.getResult();
    try {
      if (!CloseableReference.isValid(image)) {
        onFailure(drawable, imageRequest, dataSource);
      } else {
        setActualImage(drawable, imageRequest, image, false, dataSource);
      }
    } finally {
      CloseableReference.closeSafely(image);
    }
  }

  @Override
  public void onFailure(
      FrescoDrawable2 drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    Drawable errorDrawable =
        mHierarcher.buildErrorDrawable(imageRequest.resources, imageRequest.imageOptions);
    drawable.setProgress(1f);
    drawable.setImageDrawable(errorDrawable);
    Extras extras = obtainExtras(dataSource, dataSource.getResult(), drawable);
    drawable
        .getImageListener()
        .onFailure(
            drawable.getImageId(),
            drawable.getImageRequest(),
            errorDrawable,
            dataSource.getFailureCause(),
            extras);
    mDebugOverlayFactory.update(drawable, extras);
  }

  @Override
  public void onProgressUpdate(
      FrescoDrawable2 drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    boolean isFinished = dataSource.isFinished();
    float progress = dataSource.getProgress();
    if (!isFinished) {
      drawable.setProgress(progress);
    }
  }

  @Override
  public void onRelease(final FrescoDrawable2 drawable) {
    // Notify listeners
    drawable
        .getImageListener()
        .onRelease(
            drawable.getImageId(), drawable.getImageRequest(), obtainExtras(null, null, drawable));
  }

  private static Extras obtainExtras(
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource,
      CloseableReference<CloseableImage> image,
      FrescoDrawable2 drawable) {
    Map<String, Object> imageExtras = null;
    if (image != null && image.get() != null) {
      imageExtras = image.get().getExtras();
    }
    return MiddlewareUtils.obtainExtras(
        COMPONENT_EXTRAS,
        SHORTCUT_EXTRAS,
        dataSource == null ? null : dataSource.getExtras(),
        drawable.getViewportDimensions(),
        String.valueOf(drawable.getActualImageScaleType()),
        drawable.getActualImageFocusPoint(),
        imageExtras,
        drawable.getCallerContext(),
        null);
  }
}
