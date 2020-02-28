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
import com.facebook.imagepipeline.image.CloseableImage;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

public class FrescoController2Impl implements DrawableDataSubscriber, FrescoController2 {

  private final Hierarcher mHierarcher;
  private final Executor mLightweightBackgroundThreadExecutor;
  private final Executor mUiThreadExecutor;
  private final VitoImagePipeline mImagePipeline;

  public FrescoController2Impl(
      Hierarcher hierarcher,
      Executor lightweightBackgroundThreadExecutor,
      Executor uiThreadExecutor,
      VitoImagePipeline imagePipeline) {
    mHierarcher = hierarcher;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mUiThreadExecutor = uiThreadExecutor;
    mImagePipeline = imagePipeline;
  }

  @Override
  public boolean fetch(
      final FrescoDrawable2 frescoDrawable,
      final VitoImageRequest imageRequest,
      final @Nullable Object callerContext) {
    // Check if we already fetched the image
    if (frescoDrawable.getDrawableDataSubscriber() == this
        && frescoDrawable.isFetchSubmitted()
        && imageRequest.equals(frescoDrawable.getImageRequest())) {
      return true; // already set
    }
    // We didn't -> Reset everything
    frescoDrawable.close();
    // Basic setup
    frescoDrawable.setDrawableDataSubscriber(this);
    frescoDrawable.setImageRequest(imageRequest);
    frescoDrawable.setCallerContext(callerContext);

    // Set layers that are always visible
    frescoDrawable.setOverlayDrawable(
        mHierarcher.buildOverlayDrawable(imageRequest.resources, imageRequest.imageOptions));

    // Check if the image is in cache
    CloseableReference<CloseableImage> cachedImage = mImagePipeline.getCachedImage(imageRequest);
    try {
      if (CloseableReference.isValid(cachedImage)) {
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
    frescoDrawable.setPlaceholderDrawable(
        mHierarcher.buildPlaceholderDrawable(imageRequest.resources, imageRequest.imageOptions));
    frescoDrawable.setImageDrawable(null);

    // Fetch the image
    mLightweightBackgroundThreadExecutor.execute(
        new Runnable() {
          @Override
          public void run() {
            DataSource<CloseableReference<CloseableImage>> dataSource =
                mImagePipeline.fetchDecodedImage(imageRequest, callerContext);
            frescoDrawable.setImageId(FrescoContext.generateIdentifier());
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
  }

  private void setActualImage(
      FrescoDrawable2 drawable,
      VitoImageRequest imageRequest,
      CloseableReference<CloseableImage> image,
      boolean isImmediate) {
    mHierarcher.setupActualImageWrapper(
        drawable.getActualImageWrapper(), imageRequest.imageOptions);
    mHierarcher.setupActualImageDrawable(
        drawable,
        imageRequest.resources,
        imageRequest.imageOptions,
        image,
        drawable.getActualImageWrapper(),
        isImmediate,
        null);
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
  }

  @Override
  public void onProgressUpdate(
      FrescoDrawable2 drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    // TODO: implement
  }
}
