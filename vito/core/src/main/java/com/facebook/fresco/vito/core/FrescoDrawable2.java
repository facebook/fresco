/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.imagepipeline.image.CloseableImage;
import javax.annotation.Nonnull;

public class FrescoDrawable2 extends BaseFrescoDrawable
    implements DeferredReleaser.Releasable, DataSubscriber<CloseableReference<CloseableImage>> {

  private static final long RELEASE_DELAY = 16 * 5; // Roughly 5 frames.
  private static final Handler sHandler = new Handler(Looper.getMainLooper());
  private static final DeferredReleaser sDeferredReleaser = DeferredReleaser.getInstance();

  private @Nullable VitoImageRequest mImageRequest;
  private @Nullable Object mCallerContext;
  private @Nullable DrawableDataSubscriber mDrawableDataSubscriber;
  private long mImageId;

  private @Nullable DataSource<CloseableReference<CloseableImage>> mDataSource;
  private boolean mFetchSubmitted;

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

  public FrescoDrawable2() {
    super(true);
  }

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

  public ScaleTypeDrawable getActualImageWrapper() {
    return mActualImageWrapper;
  }

  public void setDataSource(@Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {
    mDataSource = dataSource;
  }

  public void setFetchSubmitted(boolean fetchSubmitted) {
    mFetchSubmitted = fetchSubmitted;
  }

  public boolean isFetchSubmitted() {
    return mFetchSubmitted;
  }

  public void setDrawableDataSubscriber(@Nullable DrawableDataSubscriber drawableDataSubscriber) {
    mDrawableDataSubscriber = drawableDataSubscriber;
  }

  @Nullable
  public DrawableDataSubscriber getDrawableDataSubscriber() {
    return mDrawableDataSubscriber;
  }

  public void setImageRequest(@Nullable VitoImageRequest imageRequest) {
    mImageRequest = imageRequest;
  }

  public void setCallerContext(@Nullable Object callerContext) {
    mCallerContext = callerContext;
  }

  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Nullable
  public VitoImageRequest getImageRequest() {
    return mImageRequest;
  }

  public void setImageId(long imageId) {
    mImageId = imageId;
  }

  public long getImageId() {
    return mImageId;
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
  }

  public void scheduleReleaseDelayed() {
    if (mDelayedReleasePending) {
      return;
    }
    sHandler.postDelayed(mReleaseRunnable, RELEASE_DELAY);
    mDelayedReleasePending = true;
  }

  public void cancelReleaseDelayed() {
    if (mDelayedReleasePending) {
      sHandler.removeCallbacks(mReleaseRunnable);
      mDelayedReleasePending = false;
    }
  }

  public void scheduleReleaseNextFrame() {
    cancelReleaseDelayed();
    sDeferredReleaser.scheduleDeferredRelease(this);
  }

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
}
