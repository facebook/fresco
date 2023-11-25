/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.core.CombinedImageListener;
import com.facebook.fresco.vito.core.NopDrawable;
import com.facebook.fresco.vito.core.VitoImagePerfListener;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoDrawable2Impl extends FrescoDrawable2
    implements DataSubscriber<CloseableReference<CloseableImage>> {

  private static final long RELEASE_DELAY = 16 * 5; // Roughly 5 frames.
  private static final Handler sHandler = new Handler(Looper.getMainLooper());
  private static final DeferredReleaser sDeferredReleaser = DeferredReleaser.getInstance();

  private final boolean mUseNewReleaseCallbacks;
  private @Nullable VitoImageRequest mImageRequest;
  private @Nullable Object mCallerContext;
  private @Nullable DrawableDataSubscriber mDrawableDataSubscriber;
  private long mImageId;
  private @Nullable Object mExtras;

  private @Nullable DataSource<CloseableReference<CloseableImage>> mDataSource;
  private boolean mFetchSubmitted;

  private @Nullable Runnable mRefetchRunnable;

  private final CombinedImageListener mImageListener = new CombinedImageListenerImpl();
  private final VitoImagePerfListener mImagePerfListener;

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
          mImageOrigin = mapProducerNameToImageOrigin(producerName);
        }

        private @ImageOrigin int mapProducerNameToImageOrigin(final String producerName) {
          switch (producerName) {
            case "BitmapMemoryCacheGetProducer":
            case "BitmapMemoryCacheProducer":
            case "PostprocessedBitmapMemoryCacheProducer":
              return ImageOrigin.MEMORY_BITMAP;

            case "EncodedMemoryCacheProducer":
              return ImageOrigin.MEMORY_ENCODED;

            case "DiskCacheProducer":
            case "PartialDiskCacheProducer":
              return ImageOrigin.DISK;

            case "NetworkFetchProducer":
              return ImageOrigin.NETWORK;

            case "DataFetchProducer":
            case "LocalAssetFetchProducer":
            case "LocalContentUriFetchProducer":
            case "LocalContentUriThumbnailFetchProducer":
            case "LocalFileFetchProducer":
            case "LocalResourceFetchProducer":
            case "VideoThumbnailProducer":
            case "QualifiedResourceFetchProducer":
              return ImageOrigin.LOCAL;

            default:
              return ImageOrigin.UNKNOWN;
          }
        }
      };

  private @ImageOrigin int mImageOrigin = ImageOrigin.UNKNOWN;

  @VisibleForTesting @Nullable CloseableReference<CloseableImage> mImageReference;

  private int mIntrinsicWidth = -1;
  private int mIntrinsicHeight = -1;

  public FrescoDrawable2Impl(
      boolean useNewReleaseCallbacks,
      @Nullable ControllerListener2<ImageInfo> imagePerfControllerListener,
      VitoImagePerfListener imagePerfListener) {
    mUseNewReleaseCallbacks = useNewReleaseCallbacks;
    mImageListener.setImagePerfControllerListener(imagePerfControllerListener);
    mImagePerfListener = imagePerfListener;
  }

  public @Nullable Drawable setImageDrawable(@Nullable Drawable newDrawable) {
    return setImage(newDrawable, null);
  }

  public @Nullable Drawable setImage(
      @Nullable Drawable imageDrawable,
      @Nullable CloseableReference<CloseableImage> imageReference) {
    cancelReleaseNextFrame();
    cancelReleaseDelayed();
    if (imageDrawable != mActualImageWrapper) {
      mActualImageWrapper.setCurrent(NopDrawable.INSTANCE);
    }
    CloseableReference.closeSafely(mImageReference);
    mImageReference = CloseableReference.cloneOrNull(imageReference);
    return setDrawable(IMAGE_DRAWABLE_INDEX, imageDrawable);
  }

  @Override
  public ScaleTypeDrawable getActualImageWrapper() {
    return mActualImageWrapper;
  }

  @Nullable
  @Override
  public Drawable getActualImageDrawable() {
    Drawable actual = getDrawable(IMAGE_DRAWABLE_INDEX);
    if (actual == mActualImageWrapper) {
      return mActualImageWrapper.getDrawable();
    }
    return actual;
  }

  public synchronized void setDataSource(
      long imageId, @Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (imageId != mImageId) {
      return;
    }
    if (mDataSource != null && mDataSource != dataSource) {
      mDataSource.close();
    }
    mDataSource = dataSource;
  }

  public void setFetchSubmitted(boolean fetchSubmitted) {
    this.mFetchSubmitted = fetchSubmitted;
  }

  @Override
  public boolean isFetchSubmitted() {
    return mFetchSubmitted;
  }

  public void setDrawableDataSubscriber(@Nullable DrawableDataSubscriber drawableDataSubscriber) {
    this.mDrawableDataSubscriber = drawableDataSubscriber;
  }

  @Nullable
  public DrawableDataSubscriber getDrawableDataSubscriber() {
    return mDrawableDataSubscriber;
  }

  @Override
  public void setImageRequest(@Nullable VitoImageRequest imageRequest) {
    this.mImageRequest = imageRequest;
  }

  @Override
  public void setCallerContext(@Nullable Object callerContext) {
    this.mCallerContext = callerContext;
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
  @Nullable
  public ImageListener getImageListener() {
    return mImageListener.getImageListener();
  }

  public void setVitoImageRequestListener(@Nullable VitoImageRequestListener listener) {
    mImageListener.setVitoImageRequestListener(listener);
  }

  public void setLocalVitoImageRequestListener(@Nullable VitoImageRequestListener listener) {
    mImageListener.setLocalVitoImageRequestListener(listener);
  }

  public CombinedImageListener getInternalListener() {
    return mImageListener;
  }

  public RequestListener getImageOriginListener() {
    return mImageOriginListener;
  }

  @Override
  @Nullable
  public VitoImageRequest getImageRequest() {
    return mImageRequest;
  }

  public synchronized void setImageId(long imageId) {
    this.mImageId = imageId;
  }

  @Override
  public synchronized long getImageId() {
    return mImageId;
  }

  public void setImageOrigin(@ImageOrigin int imageOrigin) {
    this.mImageOrigin = imageOrigin;
  }

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
  public synchronized void close() {
    cancelReleaseNextFrame();
    cancelReleaseDelayed();
    if (mUseNewReleaseCallbacks && mFetchSubmitted && mDrawableDataSubscriber != null) {
      mDrawableDataSubscriber.onRelease(this);
    }
    setImageId(0);
    super.close();
    super.reset();
    mActualImageWrapper.setCurrent(NopDrawable.INSTANCE);
    CloseableReference.closeSafely(mImageReference);
    mImageReference = null;
    mDrawableDataSubscriber = null;
    if (mDataSource != null) {
      mDataSource.close();
    }
    mDataSource = null;
    mFetchSubmitted = false;
    mImageRequest = null;

    mImageOrigin = ImageOrigin.UNKNOWN;
    mExtras = null;
    setOnFadeListener(null);
    mImageListener.onReset();
  }

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

  public void scheduleReleaseNextFrame() {
    cancelReleaseDelayed();
    sDeferredReleaser.scheduleDeferredRelease(this);
    if (!mUseNewReleaseCallbacks && mDrawableDataSubscriber != null) {
      mDrawableDataSubscriber.onRelease(this);
    }
  }

  public void releaseImmediately() {
    if (!mUseNewReleaseCallbacks && mDrawableDataSubscriber != null) {
      mDrawableDataSubscriber.onRelease(this);
    }
    close();
  }

  @Override
  public void cancelReleaseNextFrame() {
    sDeferredReleaser.cancelDeferredRelease(this);
  }

  @Override
  public void onNewResult(DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != mDataSource || mImageRequest == null || mDrawableDataSubscriber == null) {
      getImagePerfListener().onIgnoreResult(this);
      return; // We don't care
    }
    mDrawableDataSubscriber.onNewResult(this, mImageRequest, dataSource);
  }

  @Override
  public void onFailure(DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != mDataSource || mImageRequest == null || mDrawableDataSubscriber == null) {
      getImagePerfListener().onIgnoreFailure(this);
      return; // wrong image
    }
    mDrawableDataSubscriber.onFailure(this, mImageRequest, dataSource);
  }

  @Override
  public void onCancellation(DataSource<CloseableReference<CloseableImage>> dataSource) {
    // no-op
  }

  @Override
  public void onProgressUpdate(DataSource<CloseableReference<CloseableImage>> dataSource) {
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
    this.mExtras = extras;
  }

  @Nullable
  @Override
  public Runnable getRefetchRunnable() {
    return mRefetchRunnable;
  }

  @Override
  public void setRefetchRunnable(@Nullable Runnable refetchRunnable) {
    this.mRefetchRunnable = refetchRunnable;
  }

  @Override
  public VitoImagePerfListener getImagePerfListener() {
    return mImagePerfListener;
  }

  /** @return the width of the underlying actual image or -1 if unset */
  @Override
  public int getActualImageWidthPx() {
    if (CloseableReference.isValid(mImageReference)) {
      return mImageReference.get().getWidth();
    }
    return -1;
  }

  /** @return the width of the underlying actual image or -1 if unset */
  @Override
  public int getActualImageHeightPx() {
    if (CloseableReference.isValid(mImageReference)) {
      return mImageReference.get().getHeight();
    }
    return -1;
  }

  @Override
  public void setIntrinsicSize(int width, int height) {
    mIntrinsicWidth = width;
    mIntrinsicHeight = height;
  }

  @Override
  public int getIntrinsicWidth() {
    if (mIntrinsicWidth != -1) {
      return mIntrinsicWidth;
    }
    return super.getIntrinsicWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    if (mIntrinsicHeight != -1) {
      return mIntrinsicHeight;
    }
    return super.getIntrinsicHeight();
  }

  @Nullable
  @Override
  public ControllerListener2<ImageInfo> getImagePerfControllerListener() {
    return mImageListener.getImagePerfControllerListener();
  }
}
