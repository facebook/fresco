/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.vito.core.CombinedImageListener;
import com.facebook.fresco.vito.core.DrawableDataSubscriber;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.NopDrawable;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import javax.annotation.Nonnull;

public class FrescoDrawable2Impl extends FrescoDrawable2 {

  private static final long RELEASE_DELAY = 16 * 5; // Roughly 5 frames.
  private static final Handler sHandler = new Handler(Looper.getMainLooper());
  private static final DeferredReleaser sDeferredReleaser = DeferredReleaser.getInstance();

  private @Nullable VitoImageRequest mImageRequest;
  private @Nullable Object mCallerContext;
  private @Nullable DrawableDataSubscriber mDrawableDataSubscriber;
  private long mImageId;
  private @Nullable Object mExtras;

  private @Nullable DataSource<CloseableReference<CloseableImage>> mDataSource;
  private boolean mFetchSubmitted;

  private final CombinedImageListenerImpl mImageListener = new CombinedImageListenerImpl();

  private final Runnable mReleaseRunnable =
      new Runnable() {
        @Override
        public void run() {
          scheduleReleaseNextFrame();
        }
      };
  private boolean mDelayedReleasePending;

  private final ScaleTypeDrawable mActualImageWrapper =
      new ScaleTypeDrawable(NopDrawable.INSTANCE, ScalingUtils.ScaleType.CENTER_CROP);

  // Image perf data fields
  private final RequestListener mImageOriginListener =
      new BaseRequestListener() {
        @Override
        public void onUltimateProducerReached(
            String requestId, String producerName, boolean successful) {
          mImageOrigin = ImageOriginUtils.mapProducerNameToImageOrigin(producerName);
        }
      };

  private @ImageOrigin int mImageOrigin = ImageOrigin.UNKNOWN;

  @Override
  public @Nullable Drawable setImage(
      @Nullable Drawable imageDrawable,
      @Nullable CloseableReference<CloseableImage> imageReference) {
    cancelReleaseNextFrame();
    cancelReleaseDelayed();
    if (imageDrawable != mActualImageWrapper) {
      mActualImageWrapper.setCurrent(NopDrawable.INSTANCE);
    }
    return super.setImage(imageDrawable, imageReference);
  }

  @Override
  public ScaleTypeDrawable getActualImageWrapper() {
    return mActualImageWrapper;
  }

  @Override
  public void setDataSource(@Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {
    mDataSource = dataSource;
  }

  @Override
  public void setFetchSubmitted(boolean fetchSubmitted) {
    mFetchSubmitted = fetchSubmitted;
  }

  @Override
  public boolean isFetchSubmitted() {
    return mFetchSubmitted;
  }

  @Override
  public void setDrawableDataSubscriber(@Nullable DrawableDataSubscriber drawableDataSubscriber) {
    mDrawableDataSubscriber = drawableDataSubscriber;
  }

  @Override
  @Nullable
  public DrawableDataSubscriber getDrawableDataSubscriber() {
    return mDrawableDataSubscriber;
  }

  @Override
  public void setImageRequest(@Nullable VitoImageRequest imageRequest) {
    mImageRequest = imageRequest;
  }

  @Override
  public void setCallerContext(@Nullable Object callerContext) {
    mCallerContext = callerContext;
  }

  @Override
  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Override
  public void setImageListener(@Nullable ImageListener imageListener) {
    mImageListener.setImageListener(imageListener);
  }

  @Override
  public void setVitoImageRequestListener(@Nullable VitoImageRequestListener listener) {
    mImageListener.setVitoImageRequestListener(listener);
  }

  @Override
  public CombinedImageListener getImageListener() {
    return mImageListener;
  }

  @Override
  public RequestListener getImageOriginListener() {
    return mImageOriginListener;
  }

  @Override
  @Nullable
  public VitoImageRequest getImageRequest() {
    return mImageRequest;
  }

  @Override
  public void setImageId(long imageId) {
    mImageId = imageId;
  }

  @Override
  public long getImageId() {
    return mImageId;
  }

  @Override
  public void setImageOrigin(@ImageOrigin int imageOrigin) {
    mImageOrigin = imageOrigin;
  }

  @Override
  public @ImageOrigin int getImageOrigin() {
    return mImageOrigin;
  }

  @Override
  public void release() {
    close();
  }

  @Override
  public void reset() {
    // Close calls super.reset()
    close();
  }

  @Override
  public void close() {
    cancelReleaseNextFrame();
    cancelReleaseDelayed();
    super.close();
    super.reset();
    mDrawableDataSubscriber = null;
    if (mDataSource != null) {
      mDataSource.close();
    }
    mDataSource = null;
    mFetchSubmitted = false;
    mActualImageWrapper.setCurrent(NopDrawable.INSTANCE);
    mImageOrigin = ImageOrigin.UNKNOWN;
    mImageId = 0;
    mExtras = null;
  }

  @Override
  public void scheduleReleaseDelayed() {
    if (mDelayedReleasePending) {
      return;
    }
    sHandler.postDelayed(mReleaseRunnable, RELEASE_DELAY);
    mDelayedReleasePending = true;
  }

  @Override
  public void cancelReleaseDelayed() {
    if (mDelayedReleasePending) {
      sHandler.removeCallbacks(mReleaseRunnable);
      mDelayedReleasePending = false;
    }
  }

  @Override
  public void scheduleReleaseNextFrame() {
    cancelReleaseDelayed();
    sDeferredReleaser.scheduleDeferredRelease(this);
  }

  @Override
  public void cancelReleaseNextFrame() {
    sDeferredReleaser.cancelDeferredRelease(this);
  }

  @Override
  public void onNewResult(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != mDataSource || mImageRequest == null || mDrawableDataSubscriber == null) {
      return; // We don't care
    }
    mDrawableDataSubscriber.onNewResult(this, mImageRequest, dataSource);
  }

  @Override
  public void onFailure(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != mDataSource || mImageRequest == null || mDrawableDataSubscriber == null) {
      return; // wrong image
    }
    mDrawableDataSubscriber.onFailure(this, mImageRequest, dataSource);
  }

  @Override
  public void onCancellation(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource) {
    // no-op
  }

  @Override
  public void onProgressUpdate(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != mDataSource || mImageRequest == null || mDrawableDataSubscriber == null) {
      return; // wrong image
    }
    mDrawableDataSubscriber.onProgressUpdate(this, mImageRequest, dataSource);
  }

  @Nullable
  @Override
  public Object getExtras() {
    return mExtras;
  }

  @Override
  public void setExtras(@Nullable Object extras) {
    mExtras = extras;
  }
}
