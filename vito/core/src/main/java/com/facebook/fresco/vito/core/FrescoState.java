/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.UiThread;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;

public class FrescoState
    implements DataSubscriber<CloseableReference<CloseableImage>>,
        ImageListener,
        DeferredReleaser.Releasable {

  private final FrescoContext mFrescoContext;
  private final long mId;
  private final @Nullable Uri mUri;
  private final @Nullable MultiUri mMultiUri;
  private final ImageOptions mImageOptions;
  private final @Nullable Object mCallerContext;
  private final @Nullable CacheKey mCacheKey;

  // ImageListener passed as @Prop to Litho component
  private @Nullable ImageListener mImageListener;
  // Other global and ad-hoc ImageListener(s)
  private final @Nullable ImageListener mOtherListeners;
  private @Nullable ImageRequest mImageRequest;
  private @Px int mTargetWidthPx;
  private @Px int mTargetHeightPx;
  private Resources mResources;
  private FrescoDrawable mFrescoDrawable;
  private @Nullable CloseableReference<CloseableImage> mCachedImage;
  private boolean mIsAttached;
  private boolean mImageFetched;

  // Experimental fields
  private @Nullable Producer<CloseableReference<CloseableImage>> mProducerSequence;
  private @Nullable SettableProducerContext mSettableProducerContext;
  private @Nullable RequestListener mRequestListener;
  private @Nullable DataSource<CloseableReference<CloseableImage>> mPrefetchDatasource;
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

  public FrescoState(
      long id,
      FrescoContext frescoContext,
      @Nullable Uri uri,
      @Nullable MultiUri multiUri,
      ImageOptions imageOptions,
      Object callerContext,
      @Nullable ImageRequest imageRequest,
      @Nullable CacheKey cacheKey,
      @Nullable CloseableReference<CloseableImage> cachedImage,
      Resources resources,
      @Nullable ImageListener imageListener,
      @Nullable ImageListener otherListeners) {
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
  }

  public long getId() {
    return mId;
  }

  @UiThread
  public FrescoDrawable getFrescoDrawable() {
    return mFrescoDrawable;
  }

  @UiThread
  public void setFrescoDrawable(FrescoDrawable frescoDrawable) {
    mFrescoDrawable = frescoDrawable;
  }

  @UiThread
  public boolean isAttached() {
    return mIsAttached;
  }

  @UiThread
  public void setAttached(boolean isAttached) {
    mIsAttached = isAttached;
  }

  public ImageOptions getImageOptions() {
    return mImageOptions;
  }

  @Nullable
  public ImageRequest getImageRequest() {
    return mImageRequest;
  }

  @Nullable
  public CacheKey getCacheKey() {
    return mCacheKey;
  }

  @UiThread
  public boolean isImageFetched() {
    return mImageFetched;
  }

  @UiThread
  public void setImageFetched(boolean imageFetched) {
    mImageFetched = imageFetched;
  }

  @Nullable
  public synchronized CloseableReference<CloseableImage> getCachedImage() {
    return CloseableReference.cloneOrNull(mCachedImage);
  }

  public synchronized void setCachedImage(CloseableReference<CloseableImage> cachedImage) {
    CloseableReference.closeSafely(mCachedImage);
    mCachedImage = CloseableReference.cloneOrNull(cachedImage);
  }

  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  public Resources getResources() {
    return mResources;
  }

  public void setResources(Resources resources) {
    mResources = resources;
  }

  @Nullable
  public Uri getUri() {
    return mUri;
  }

  @Nullable
  public MultiUri getMultiUri() {
    return mMultiUri;
  }

  public void setProducerSequence(
      @Nullable Producer<CloseableReference<CloseableImage>> producerSequence) {
    mProducerSequence = producerSequence;
  }

  @Nullable
  public Producer<CloseableReference<CloseableImage>> getProducerSequence() {
    return mProducerSequence;
  }

  public void setSettableProducerContext(
      @Nullable SettableProducerContext settableProducerContext) {
    mSettableProducerContext = settableProducerContext;
  }

  @Nullable
  public SettableProducerContext getSettableProducerContext() {
    return mSettableProducerContext;
  }

  public void setRequestListener(@Nullable RequestListener requestListener) {
    mRequestListener = requestListener;
  }

  @Nullable
  public RequestListener getRequestListener() {
    return mRequestListener;
  }

  @Nullable
  public RequestListener getImageOriginListener() {
    return mImageOriginListener;
  }

  @Nullable
  public ForwardingDrawable getActualImageWrapper() {
    return mActualImageWrapper;
  }

  public void setActualImageWrapper(@Nullable ForwardingDrawable actualImageWrapper) {
    mActualImageWrapper = actualImageWrapper;
  }

  @Nullable
  public Drawable getOverlayDrawable() {
    return mOverlayDrawable;
  }

  public void setOverlayDrawable(@Nullable Drawable overlayDrawable) {
    mOverlayDrawable = overlayDrawable;
  }

  public void setPlaceholderDrawable(@Nullable Drawable placeholderDrawable) {
    mPlaceholderDrawable = placeholderDrawable;
  }

  @Nullable
  public Drawable getPlaceholderDrawable() {
    return mPlaceholderDrawable;
  }

  @ImageOrigin
  public int getImageOrigin() {
    return mImageOrigin;
  }

  public void setImageOrigin(@ImageOrigin int imageOrigin) {
    mImageOrigin = imageOrigin;
  }

  @Override
  public void onNewResult(DataSource<CloseableReference<CloseableImage>> dataSource) {
    mFrescoContext.getController().onNewResult(this, dataSource);
  }

  @Override
  public void onFailure(DataSource<CloseableReference<CloseableImage>> dataSource) {
    mFrescoContext.getController().onFailure(this, dataSource);
  }

  @Override
  public void onCancellation(DataSource<CloseableReference<CloseableImage>> dataSource) {
    mFrescoContext.getController().onCancellation(this, dataSource);
  }

  @Override
  public void onProgressUpdate(DataSource<CloseableReference<CloseableImage>> dataSource) {
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
  }

  @Override
  public void onIntermediateImageSet(long id, @Nullable ImageInfo imageInfo) {
    if (mImageListener != null) {
      mImageListener.onIntermediateImageSet(id, imageInfo);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onIntermediateImageSet(id, imageInfo);
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
  }

  @Override
  public void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable) {
    if (mImageListener != null) {
      mImageListener.onFailure(id, error, throwable);
    }
    if (mOtherListeners != null) {
      mOtherListeners.onFailure(id, error, throwable);
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
  }

  public @Px int getTargetWidthPx() {
    return mTargetWidthPx;
  }

  public void setTargetWidthPx(@Px int targetWidthPx) {
    mTargetWidthPx = targetWidthPx;
  }

  public @Px int getTargetHeightPx() {
    return mTargetHeightPx;
  }

  public void setTargetHeightPx(@Px int targetHeightPx) {
    mTargetHeightPx = targetHeightPx;
  }

  public @Nullable ImageListener getImageListener() {
    return mImageListener;
  }

  public void setImageListener(ImageListener imageListener) {
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

  public void setPrefetchDatasource(
      @Nullable DataSource<CloseableReference<CloseableImage>> prefetchDatasource) {
    if (mPrefetchDatasource != null) {
      mPrefetchDatasource.close();
    }
    mPrefetchDatasource = prefetchDatasource;
  }

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

  public void setImageRequest(@Nullable ImageRequest imageRequest) {
    mImageRequest = imageRequest;
  }

  public @Nullable Object getExtras() {
    return mExtras;
  }

  public void setExtras(@Nullable Object extras) {
    mExtras = extras;
  }

  public String getStringId() {
    return getStringId(getId());
  }

  public static String getStringId(long id) {
    // Vito IDs and Drawee IDs overlap. We add a prefix to distinguish between them.
    return "v" + id;
  }
}
