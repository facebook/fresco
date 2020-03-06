/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.drawable.Drawable;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.CloseableImage;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

public class FrescoController2Impl implements DrawableDataSubscriber, FrescoController2 {

  private final Hierarcher mHierarcher;
  private final Executor mLightweightBackgroundThreadExecutor;
  private final Executor mUiThreadExecutor;
  private final VitoImagePipeline mImagePipeline;
  private final @Nullable VitoImageRequestListener mGlobalImageListener;

  public FrescoController2Impl(
      Hierarcher hierarcher,
      Executor lightweightBackgroundThreadExecutor,
      Executor uiThreadExecutor,
      VitoImagePipeline imagePipeline,
      @Nullable VitoImageRequestListener globalImageListener) {
    mHierarcher = hierarcher;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mUiThreadExecutor = uiThreadExecutor;
    mImagePipeline = imagePipeline;
    mGlobalImageListener = globalImageListener;
  }

  @Override
  public boolean fetch(
      final FrescoDrawable2 frescoDrawable,
      final VitoImageRequest imageRequest,
      final @Nullable Object callerContext,
      final @Nullable ImageListener listener) {
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

    // Set layers that are always visible
    frescoDrawable.setOverlayDrawable(
        mHierarcher.buildOverlayDrawable(imageRequest.resources, imageRequest.imageOptions));

    // We're fetching a new image, so we're updating the ID
    final long imageId = VitoUtils.generateIdentifier();
    frescoDrawable.setImageId(imageId);

    // Notify listeners that we're about to fetch an image
    frescoDrawable.getImageListener().onSubmit(imageId, imageRequest, callerContext);

    // Check if the image is in cache
    CloseableReference<CloseableImage> cachedImage = mImagePipeline.getCachedImage(imageRequest);
    try {
      if (CloseableReference.isValid(cachedImage)) {
        frescoDrawable.setImageOrigin(ImageOrigin.MEMORY_BITMAP);
        // Immediately display the actual image.
        setActualImage(frescoDrawable, imageRequest, cachedImage, true);
        return true;
      }
    } finally {
      CloseableReference.closeSafely(cachedImage);
    }

    // The image is not in cache -> Set up layers visible until the image is available
    frescoDrawable.setProgressDrawable(
        mHierarcher.buildProgressDrawable(imageRequest.resources, imageRequest.imageOptions));
    Drawable placeholder =
        mHierarcher.buildPlaceholderDrawable(imageRequest.resources, imageRequest.imageOptions);
    frescoDrawable.setPlaceholderDrawable(placeholder);
    frescoDrawable.setImageDrawable(null);

    frescoDrawable.getImageListener().onPlaceholderSet(imageId, imageRequest, placeholder);

    // Fetch the image
    mLightweightBackgroundThreadExecutor.execute(
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
        });
    frescoDrawable.setFetchSubmitted(true);
    return false;
  }

  @Override
  public void releaseDelayed(final FrescoDrawable2 drawable) {
    drawable.scheduleReleaseDelayed();
  }

  @Override
  public void release(final FrescoDrawable2 drawable) {
    drawable.scheduleReleaseNextFrame();
    drawable.getImageListener().onRelease(drawable.getImageId(), drawable.getImageRequest());
  }

  private void setActualImage(
      FrescoDrawable2 drawable,
      VitoImageRequest imageRequest,
      CloseableReference<CloseableImage> image,
      boolean isImmediate) {
    mHierarcher.setupActualImageWrapper(
        drawable.getActualImageWrapper(), imageRequest.imageOptions);
    Drawable actualDrawable =
        mHierarcher.setupActualImageDrawable(
            drawable,
            imageRequest.resources,
            imageRequest.imageOptions,
            image,
            drawable.getActualImageWrapper(),
            isImmediate,
            null);
    drawable
        .getImageListener()
        .onFinalImageSet(
            drawable.getImageId(),
            drawable.getImageRequest(),
            drawable.getImageOrigin(),
            image.get(),
            actualDrawable);
    drawable.setProgressDrawable(null);
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
        setActualImage(drawable, imageRequest, image, false);
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
    drawable.setProgressDrawable(null);
    drawable.setImageDrawable(errorDrawable);
    drawable
        .getImageListener()
        .onFailure(
            drawable.getImageId(),
            drawable.getImageRequest(),
            errorDrawable,
            dataSource.getFailureCause());
  }

  @Override
  public void onProgressUpdate(
      FrescoDrawable2 drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    // TODO: implement
  }
}
