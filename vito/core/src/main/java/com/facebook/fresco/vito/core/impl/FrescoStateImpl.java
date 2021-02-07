/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.UiThread;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoDrawable;
import com.facebook.fresco.vito.core.FrescoState;
import com.facebook.fresco.vito.core.ImageStateListener;
import com.facebook.fresco.vito.core.VitoUtils;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nonnull;

public class FrescoStateImpl implements FrescoState {

  private final FrescoContext mFrescoContext;
  private final long mId;
  private final @Nullable Uri mUri;
  private final @Nullable MultiUri mMultiUri;
  private final ImageOptions mImageOptions;
  private final @Nullable Object mCallerContext;
  private final @Nullable CacheKey mCacheKey;
  private final Resources mResources;

  // ImageListener passed as @Prop to Litho component
  private @Nullable ImageListener mImageListener;
  // Other global and ad-hoc ImageListener(s)
  private final @Nullable ImageListener mOtherListeners;
  private final @Nullable ImageStateListener mImageStateListener;
  private @Nullable ImageRequest mImageRequest;
  private @Px int mTargetWidthPx;
  private @Px int mTargetHeightPx;

  private @Nullable FrescoDrawable mFrescoDrawable;
  private @Nullable CloseableReference<CloseableImage> mCachedImage;
  private boolean mIsAttached;
  private boolean mImageFetched;

  // Experimental fields
  private @Nullable Producer<CloseableReference<CloseableImage>> mProducerSequence;
  private @Nullable SettableProducerContext mSettableProducerContext;
  private @Nullable RequestListener mRequestListener;
  private @Nullable DataSource mPrefetchDatasource;
  private @Nullable DataSource<CloseableReference<CloseableImage>> mMainFetchDatasource;
  private @Nullable Drawable mPlaceholderDrawable;

  // Experimental hierarchy fields
  private @Nullable ForwardingDrawable mActualImageWrapper;
  private @Nullable Drawable mOverlayDrawable;

  private @Nullable Object mExtras;

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

  private @Nullable Runnable mDetachRunnable;

  public FrescoStateImpl(
      long id,
      FrescoContext frescoContext,
      @Nullable Uri uri,
      @Nullable MultiUri multiUri,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      @Nullable ImageRequest imageRequest,
      @Nullable CacheKey cacheKey,
      @Nullable CloseableReference<CloseableImage> cachedImage,
      Resources resources,
      @Nullable ImageListener imageListener,
      @Nullable ImageListener otherListeners,
      @Nullable ImageStateListener imageStateListener) {
    mId = id;
    mFrescoContext = frescoContext;
    mUri = uri;
    mMultiUri = multiUri;
    mImageOptions = imageOptions;
    mCallerContext = callerContext;
    mImageRequest = imageRequest;
    mCacheKey = cacheKey;
    mCachedImage = cachedImage;
    mResources = resources;
    mImageListener = imageListener;
    mOtherListeners = otherListeners;
    mImageStateListener = imageStateListener;
  }

  @Override
  public long getId() {
    return mId;
  }

  @Override
  @UiThread
  @Nullable
  public FrescoDrawable getFrescoDrawable() {
    return mFrescoDrawable;
  }

  @Override
  @UiThread
  public void setFrescoDrawable(@Nullable FrescoDrawable frescoDrawable) {
    mFrescoDrawable = frescoDrawable;
  }

  @Override
  @UiThread
  public synchronized boolean isAttached() {
    return mIsAttached;
  }

  @Override
  @UiThread
  public synchronized void setAttached(boolean isAttached) {
    mIsAttached = isAttached;
  }

  @Override
  public ImageOptions getImageOptions() {
    return mImageOptions;
  }

  @Override
  @Nullable
  public ImageRequest getImageRequest() {
    return mImageRequest;
  }

  @Override
  @Nullable
  public CacheKey getCacheKey() {
    return mCacheKey;
  }

  @Override
  @UiThread
  public boolean isImageFetched() {
    return mImageFetched;
  }

  @Override
  @UiThread
  public void setImageFetched(boolean imageFetched) {
    mImageFetched = imageFetched;
  }

  @Override
  @Nullable
  public synchronized CloseableReference<CloseableImage> getCachedImage() {
    return CloseableReference.cloneOrNull(mCachedImage);
  }

  @Override
  public synchronized void setCachedImage(
      @Nullable CloseableReference<CloseableImage> cachedImage) {
    CloseableReference.closeSafely(mCachedImage);
    mCachedImage = CloseableReference.cloneOrNull(cachedImage);
  }

  @Override
  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Override
  public Resources getResources() {
    return mResources;
  }

  @Override
  @Nullable
  public Uri getUri() {
    return mUri;
  }

  @Override
  @Nullable
  public MultiUri getMultiUri() {
    return mMultiUri;
  }

  @Override
  public void setProducerSequence(
      @Nullable Producer<CloseableReference<CloseableImage>> producerSequence) {
    mProducerSequence = producerSequence;
  }

  @Override
  @Nullable
  public Producer<CloseableReference<CloseableImage>> getProducerSequence() {
    return mProducerSequence;
  }

  @Override
  public void setSettableProducerContext(
      @Nullable SettableProducerContext settableProducerContext) {
    mSettableProducerContext = settableProducerContext;
  }

  @Override
  @Nullable
  public SettableProducerContext getSettableProducerContext() {
    return mSettableProducerContext;
  }

  @Override
  public void setRequestListener(@Nullable RequestListener requestListener) {
    mRequestListener = requestListener;
  }

  @Override
  @Nullable
  public RequestListener getRequestListener() {
    return mRequestListener;
  }

  @Override
  @Nullable
  public RequestListener getImageOriginListener() {
    return mImageOriginListener;
  }

  @Override
  @Nullable
  public ForwardingDrawable getActualImageWrapper() {
    return mActualImageWrapper;
  }

  @Override
  public void setActualImageWrapper(@Nullable ForwardingDrawable actualImageWrapper) {
    mActualImageWrapper = actualImageWrapper;
  }

  @Override
  @Nullable
  public Drawable getOverlayDrawable() {
    return mOverlayDrawable;
  }

  @Override
  public void setOverlayDrawable(@Nullable Drawable overlayDrawable) {
    mOverlayDrawable = overlayDrawable;
  }

  @Override
  public void setPlaceholderDrawable(@Nullable Drawable placeholderDrawable) {
    mPlaceholderDrawable = placeholderDrawable;
  }

  @Override
  @Nullable
  public Drawable getPlaceholderDrawable() {
    return mPlaceholderDrawable;
  }

  @Override
  @ImageOrigin
  public int getImageOrigin() {
    return mImageOrigin;
  }

  @Override
  public void setImageOrigin(@ImageOrigin int imageOrigin) {
    mImageOrigin = imageOrigin;
  }

  @Override
  @Nullable
  public synchronized Runnable removeDetachRunnable() {
    Runnable r = mDetachRunnable;
    mDetachRunnable = null;
    return r;
  }

  @Override
  public synchronized void setDetachRunnable(@Nullable Runnable detachRunnable) {
    mDetachRunnable = detachRunnable;
  }

  @Override
  public void onNewResult(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource) {
    mFrescoContext.getController().onNewResult(this, dataSource);
  }

  @Override
  public void onFailure(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource) {
    mFrescoContext.getController().onFailure(this, dataSource);
  }

  @Override
  public void onCancellation(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource) {
    mFrescoContext.getController().onCancellation(this, dataSource);
  }

  @Override
  public void onProgressUpdate(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource) {
    mFrescoContext.getController().onProgressUpdate(this, dataSource);
  }

  @Override
  public void onSubmit(long id, Object callerContext) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoState#onSubmit");
    }
    if (mImageListener != null) {
      mImageListener.onSubmit(id, callerContext);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onSubmit(id, callerContext);
    }
    if (mImageStateListener != null) {
      mImageStateListener.onSubmit(this, callerContext);
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  @Override
  public void onPlaceholderSet(long id, @Nullable Drawable placeholder) {
    if (mImageListener != null) {
      mImageListener.onPlaceholderSet(id, placeholder);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onPlaceholderSet(id, placeholder);
    }
    if (mImageStateListener != null) {
      mImageStateListener.onPlaceholderSet(this, placeholder);
    }
  }

  @Override
  public void onFinalImageSet(
      long id,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable Drawable drawable) {
    if (mImageListener != null) {
      mImageListener.onFinalImageSet(id, imageOrigin, imageInfo, drawable);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onFinalImageSet(id, imageOrigin, imageInfo, drawable);
    }
    if (mImageStateListener != null) {
      mImageStateListener.onFinalImageSet(this, imageOrigin, imageInfo, drawable);
    }
  }

  @Override
  public void onIntermediateImageSet(long id, @Nullable ImageInfo imageInfo) {
    if (mImageListener != null) {
      mImageListener.onIntermediateImageSet(id, imageInfo);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onIntermediateImageSet(id, imageInfo);
    }
    if (mImageStateListener != null) {
      mImageStateListener.onIntermediateImageSet(this, imageInfo);
    }
  }

  @Override
  public void onIntermediateImageFailed(long id, Throwable throwable) {
    if (mImageListener != null) {
      mImageListener.onIntermediateImageFailed(id, throwable);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onIntermediateImageFailed(id, throwable);
    }
    if (mImageStateListener != null) {
      mImageStateListener.onIntermediateImageFailed(this, throwable);
    }
  }

  @Override
  public void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable) {
    if (mImageListener != null) {
      mImageListener.onFailure(id, error, throwable);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onFailure(id, error, throwable);
    }
    if (mImageStateListener != null) {
      mImageStateListener.onFailure(this, error, throwable);
    }
  }

  @Override
  public void onRelease(long id) {
    if (mImageListener != null) {
      mImageListener.onRelease(id);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onRelease(id);
    }
    if (mImageStateListener != null) {
      mImageStateListener.onRelease(this);
    }
  }

  @Override
  public @Px int getTargetWidthPx() {
    return mTargetWidthPx;
  }

  @Override
  public void setTargetWidthPx(@Px int targetWidthPx) {
    mTargetWidthPx = targetWidthPx;
  }

  @Override
  public @Px int getTargetHeightPx() {
    return mTargetHeightPx;
  }

  @Override
  public void setTargetHeightPx(@Px int targetHeightPx) {
    mTargetHeightPx = targetHeightPx;
  }

  @Override
  public @Nullable ImageListener getImageListener() {
    return mImageListener;
  }

  @Override
  public void setImageListener(@Nullable ImageListener imageListener) {
    mImageListener = imageListener;
  }

  @Override
  public String toString() {
    return "FrescoState{"
        + "mFrescoContext="
        + mFrescoContext
        + ", mId="
        + mId
        + ", mUri="
        + mUri
        + ", mMultiUri="
        + mMultiUri
        + ", mImageOptions="
        + mImageOptions
        + ", mCallerContext="
        + mCallerContext
        + ", mImageRequest="
        + mImageRequest
        + ", mResources="
        + mResources
        + ", mCachedImage="
        + mCachedImage
        + ", mIsAttached="
        + mIsAttached
        + ", mImageFetched="
        + mImageFetched
        + ", mTargetWidthPx="
        + mTargetWidthPx
        + ", mTargetHeightPx="
        + mTargetHeightPx
        + '}';
  }

  @Override
  public void setPrefetchDatasource(@Nullable DataSource prefetchDatasource) {
    if (mPrefetchDatasource != null) {
      mPrefetchDatasource.close();
    }
    mPrefetchDatasource = prefetchDatasource;
  }

  @Override
  public void setMainFetchDatasource(
      @Nullable DataSource<CloseableReference<CloseableImage>> mainFetchDatasource) {
    if (mMainFetchDatasource != null) {
      mMainFetchDatasource.close();
    }
    mMainFetchDatasource = mainFetchDatasource;
  }

  @Override
  @UiThread
  public void release() {
    DataSource dataSource;
    if ((dataSource = mMainFetchDatasource) != null) {
      dataSource.close();
    }
    if ((dataSource = mPrefetchDatasource) != null) {
      dataSource.close();
    }

    CloseableReference.closeSafely(mCachedImage);

    if (mFrescoContext.getExperiments().resetState()) {
      mPrefetchDatasource = null;
      mMainFetchDatasource = null;
      mActualImageWrapper = null;
    }
  }

  @Override
  public void setImageRequest(@Nullable ImageRequest imageRequest) {
    mImageRequest = imageRequest;
  }

  @Override
  public @Nullable Object getExtras() {
    return mExtras;
  }

  @Override
  public void setExtras(@Nullable Object extras) {
    mExtras = extras;
  }

  @Override
  public String getStringId() {
    return VitoUtils.getStringId(getId());
  }

  @Override
  public void onImageDrawn(String id, ImageInfo imageInfo, DimensionsInfo dimensionsInfo) {
    if (mImageListener != null) {
      mImageListener.onImageDrawn(id, imageInfo, dimensionsInfo);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onImageDrawn(id, imageInfo, dimensionsInfo);
    }
    if (mImageStateListener != null) {
      mImageStateListener.onImageDrawn(id, imageInfo, dimensionsInfo);
    }
  }
}
