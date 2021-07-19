/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.facebook.common.callercontext.ContextChain;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.fresco.middleware.MiddlewareUtils;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.FrescoVitoConfig;
import com.facebook.fresco.vito.core.NopDrawable;
import com.facebook.fresco.vito.core.VitoImagePerfListener;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayFactory2;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoController2Impl implements DrawableDataSubscriber, FrescoController2 {

  private static final Map<String, Object> COMPONENT_EXTRAS =
      ImmutableMap.<String, Object>of("component_tag", "vito2");
  private static final Map<String, Object> SHORTCUT_EXTRAS =
      ImmutableMap.<String, Object>of("origin", "memory_bitmap", "origin_sub", "shortcut");

  private final FrescoVitoConfig mConfig;
  private final Hierarcher mHierarcher;
  private final Executor mLightweightBackgroundThreadExecutor;
  private final Executor mUiThreadExecutor;
  private final VitoImagePipeline mImagePipeline;
  private final @Nullable VitoImageRequestListener mGlobalImageListener;
  private final DebugOverlayFactory2 mDebugOverlayFactory;
  private final @Nullable Supplier<ControllerListener2<ImageInfo>> mImagePerfListenerSupplier;
  private final VitoImagePerfListener mVitoImagePerfListener;

  public FrescoController2Impl(
      FrescoVitoConfig config,
      Hierarcher hierarcher,
      Executor lightweightBackgroundThreadExecutor,
      Executor uiThreadExecutor,
      VitoImagePipeline imagePipeline,
      @Nullable VitoImageRequestListener globalImageListener,
      DebugOverlayFactory2 debugOverlayFactory,
      @Nullable Supplier<ControllerListener2<ImageInfo>> imagePerfListenerSupplier,
      VitoImagePerfListener vitoImagePerfListener) {
    mConfig = config;
    mHierarcher = hierarcher;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mUiThreadExecutor = uiThreadExecutor;
    mImagePipeline = imagePipeline;
    mGlobalImageListener = globalImageListener;
    mDebugOverlayFactory = debugOverlayFactory;
    mImagePerfListenerSupplier = imagePerfListenerSupplier;
    mVitoImagePerfListener = vitoImagePerfListener;
  }

  @Override
  public FrescoDrawable2 createDrawable() {
    return new FrescoDrawable2Impl(
        mConfig.useNewReleaseCallback(),
        mImagePerfListenerSupplier == null ? null : mImagePerfListenerSupplier.get(),
        mVitoImagePerfListener);
  }

  @Override
  public boolean fetch(
      final FrescoDrawable2 drawable,
      final VitoImageRequest imageRequest,
      final @Nullable Object callerContext,
      final @Nullable ContextChain contextChain,
      final @Nullable ImageListener listener,
      final @Nullable FadeDrawable.OnFadeListener onFadeListener,
      final @Nullable Rect viewportDimensions) {
    if (!(drawable instanceof FrescoDrawable2Impl)) {
      throw new IllegalArgumentException("Drawable not supported " + drawable);
    }
    final FrescoDrawable2Impl frescoDrawable = (FrescoDrawable2Impl) drawable;
    // Save viewport dimension for future use
    drawable.setViewportDimensions(viewportDimensions);

    // Check if we already fetched the image
    if (frescoDrawable.getDrawableDataSubscriber() == this
        && frescoDrawable.isFetchSubmitted()
        && imageRequest.equals(frescoDrawable.getImageRequest())) {
      frescoDrawable.cancelReleaseNextFrame();
      frescoDrawable.cancelReleaseDelayed();
      return true; // already set
    }
    if (frescoDrawable.isFetchSubmitted()) {
      frescoDrawable.getImagePerfListener().onDrawableReconfigured(frescoDrawable);
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
    frescoDrawable.setOverlayDrawable(
        mHierarcher.buildOverlayDrawable(imageRequest.resources, imageRequest.imageOptions));
    frescoDrawable.showOverlayImmediately();

    // We're fetching a new image, so we're updating the ID
    final long imageId = VitoUtils.generateIdentifier();
    frescoDrawable.setImageId(imageId);

    Extras extras = obtainExtras(null, null, frescoDrawable);

    // Notify listeners that we're about to fetch an image
    frescoDrawable.getInternalListener().onSubmit(imageId, imageRequest, callerContext, extras);
    frescoDrawable.getImagePerfListener().onImageFetch(frescoDrawable);

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

    frescoDrawable.getInternalListener().onPlaceholderSet(imageId, imageRequest, placeholder);

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
            frescoDrawable.setDataSource(imageId, dataSource);
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
    if (!(drawable instanceof FrescoDrawable2Impl)) {
      throw new IllegalArgumentException("Drawable not supported " + drawable);
    }
    FrescoDrawable2Impl frescoDrawable = (FrescoDrawable2Impl) drawable;
    frescoDrawable.getImagePerfListener().onScheduleReleaseDelayed(frescoDrawable);
    frescoDrawable.scheduleReleaseDelayed();
  }

  @Override
  public void release(final FrescoDrawable2 drawable) {
    if (!(drawable instanceof FrescoDrawable2Impl)) {
      throw new IllegalArgumentException("Drawable not supported " + drawable);
    }
    FrescoDrawable2Impl frescoDrawable = (FrescoDrawable2Impl) drawable;
    frescoDrawable.getImagePerfListener().onScheduleReleaseNextFrame(frescoDrawable);
    frescoDrawable.scheduleReleaseNextFrame();
  }

  @Override
  public void releaseImmediately(FrescoDrawable2 drawable) {
    if (!(drawable instanceof FrescoDrawable2Impl)) {
      throw new IllegalArgumentException("Drawable not supported " + drawable);
    }
    FrescoDrawable2Impl frescoDrawable = (FrescoDrawable2Impl) drawable;
    frescoDrawable.getImagePerfListener().onReleaseImmediately(frescoDrawable);
    frescoDrawable.releaseImmediately();
  }

  private void setActualImage(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      CloseableReference<CloseableImage> image,
      boolean isImmediate,
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {

    ScaleTypeDrawable actualImageWrapperDrawable = drawable.getActualImageWrapper();
    mHierarcher.setupActualImageWrapper(
        actualImageWrapperDrawable, imageRequest.imageOptions, drawable.getCallerContext());
    Drawable actualDrawable =
        mHierarcher.buildActualImageDrawable(
            imageRequest.resources, imageRequest.imageOptions, image);
    actualImageWrapperDrawable.setCurrent(
        actualDrawable != null ? actualDrawable : NopDrawable.INSTANCE);

    drawable.setImage(actualImageWrapperDrawable, image);

    if (isImmediate || imageRequest.imageOptions.getFadeDurationMs() <= 0) {
      drawable.showImageImmediately();
    } else {
      drawable.fadeInImage(imageRequest.imageOptions.getFadeDurationMs());
    }
    if (imageRequest.imageOptions.shouldAutoPlay() && actualDrawable instanceof Animatable) {
      ((Animatable) actualDrawable).start();
    }
    Extras extras = obtainExtras(dataSource, image, drawable);
    if (notifyFinalResult(dataSource)) {
      drawable
          .getInternalListener()
          .onFinalImageSet(
              drawable.getImageId(),
              imageRequest,
              drawable.getImageOrigin(),
              image.get(),
              extras,
              actualDrawable);
    } else {
      drawable
          .getInternalListener()
          .onIntermediateImageSet(drawable.getImageId(), imageRequest, image.get());
    }
    drawable.getImagePerfListener().onImageSuccess(drawable, isImmediate);
    float progress = 1f;
    if (dataSource != null && !dataSource.isFinished()) {
      progress = dataSource.getProgress();
    }
    drawable.setProgress(progress);
    mDebugOverlayFactory.update(drawable, extras);
  }

  @Override
  public void onNewResult(
      FrescoDrawable2Impl drawable,
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
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    Drawable errorDrawable =
        mHierarcher.buildErrorDrawable(imageRequest.resources, imageRequest.imageOptions);
    drawable.setProgress(1f);
    drawable.setImageDrawable(errorDrawable);
    if (!drawable.isDefaultLayerIsOn()) {
      if (imageRequest.imageOptions.getFadeDurationMs() <= 0) {
        drawable.showImageImmediately();
      } else {
        drawable.fadeInImage(imageRequest.imageOptions.getFadeDurationMs());
      }
    } else {
      drawable.setPlaceholderDrawable(null);
      drawable.setProgressDrawable(null);
    }
    Extras extras = obtainExtras(dataSource, dataSource.getResult(), drawable);
    if (notifyFinalResult(dataSource)) {
      drawable
          .getInternalListener()
          .onFailure(
              drawable.getImageId(),
              imageRequest,
              errorDrawable,
              dataSource.getFailureCause(),
              extras);
    } else {
      drawable
          .getInternalListener()
          .onIntermediateImageFailed(
              drawable.getImageId(), imageRequest, dataSource.getFailureCause());
    }

    drawable.getImagePerfListener().onImageError(drawable);
    mDebugOverlayFactory.update(drawable, extras);
  }

  @Override
  public void onProgressUpdate(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    boolean isFinished = dataSource.isFinished();
    float progress = dataSource.getProgress();
    if (!isFinished) {
      drawable.setProgress(progress);
    }
  }

  @Override
  public void onRelease(final FrescoDrawable2Impl drawable) {
    final VitoImageRequest imageRequest = drawable.getImageRequest();
    if (imageRequest != null) {
      // Notify listeners
      drawable
          .getInternalListener()
          .onRelease(drawable.getImageId(), imageRequest, obtainExtras(null, null, drawable));
    }
    drawable.getImagePerfListener().onImageRelease(drawable);
  }

  private static Extras obtainExtras(
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource,
      @Nullable CloseableReference<CloseableImage> image,
      FrescoDrawable2 drawable) {
    Map<String, Object> imageExtras = null;
    if (image != null) {
      imageExtras = image.get().getExtras();
    }

    Uri sourceUri = null;
    VitoImageRequest vitoImageRequest = drawable.getImageRequest();
    if (vitoImageRequest != null) {
      if (vitoImageRequest.finalImageRequest != null) {
        sourceUri = vitoImageRequest.finalImageRequest.getSourceUri();
      }
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
        sourceUri);
  }

  private static boolean notifyFinalResult(
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {
    return dataSource == null || dataSource.isFinished() || dataSource.hasMultipleResults();
  }
}
