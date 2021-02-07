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
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nonnull;

public interface FrescoState
    extends DataSubscriber<CloseableReference<CloseableImage>>,
        ImageListener,
        DeferredReleaser.Releasable {

  long getId();

  @UiThread
  @Nullable
  FrescoDrawable getFrescoDrawable();

  @UiThread
  void setFrescoDrawable(@Nullable FrescoDrawable frescoDrawable);

  @UiThread
  boolean isAttached();

  @UiThread
  void setAttached(boolean isAttached);

  ImageOptions getImageOptions();

  @Nullable
  ImageRequest getImageRequest();

  @Nullable
  CacheKey getCacheKey();

  @UiThread
  boolean isImageFetched();

  @UiThread
  void setImageFetched(boolean imageFetched);

  @Nullable
  CloseableReference<CloseableImage> getCachedImage();

  void setCachedImage(@Nullable CloseableReference<CloseableImage> cachedImage);

  @Nullable
  Object getCallerContext();

  Resources getResources();

  @Nullable
  Uri getUri();

  @Nullable
  MultiUri getMultiUri();

  void setProducerSequence(@Nullable Producer<CloseableReference<CloseableImage>> producerSequence);

  @Nullable
  Producer<CloseableReference<CloseableImage>> getProducerSequence();

  void setSettableProducerContext(@Nullable SettableProducerContext settableProducerContext);

  @Nullable
  SettableProducerContext getSettableProducerContext();

  void setRequestListener(@Nullable RequestListener requestListener);

  @Nullable
  RequestListener getRequestListener();

  @Nullable
  RequestListener getImageOriginListener();

  @Nullable
  ForwardingDrawable getActualImageWrapper();

  void setActualImageWrapper(@Nullable ForwardingDrawable actualImageWrapper);

  @Nullable
  Drawable getOverlayDrawable();

  void setOverlayDrawable(@Nullable Drawable overlayDrawable);

  void setPlaceholderDrawable(@Nullable Drawable placeholderDrawable);

  @Nullable
  Drawable getPlaceholderDrawable();

  @ImageOrigin
  int getImageOrigin();

  void setImageOrigin(@ImageOrigin int imageOrigin);

  @Nullable
  Runnable removeDetachRunnable();

  void setDetachRunnable(@Nullable Runnable detachRunnable);

  @Override
  void onNewResult(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource);

  @Override
  void onFailure(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource);

  @Override
  void onCancellation(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource);

  @Override
  void onProgressUpdate(@Nonnull DataSource<CloseableReference<CloseableImage>> dataSource);

  @Override
  void onSubmit(long id, Object callerContext);

  @Override
  void onPlaceholderSet(long id, @Nullable Drawable placeholder);

  @Override
  void onFinalImageSet(
      long id,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable Drawable drawable);

  @Override
  void onIntermediateImageSet(long id, @Nullable ImageInfo imageInfo);

  @Override
  void onIntermediateImageFailed(long id, Throwable throwable);

  @Override
  void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable);

  @Override
  void onRelease(long id);

  @Px
  int getTargetWidthPx();

  void setTargetWidthPx(@Px int targetWidthPx);

  @Px
  int getTargetHeightPx();

  void setTargetHeightPx(@Px int targetHeightPx);

  @Nullable
  ImageListener getImageListener();

  void setImageListener(@Nullable ImageListener imageListener);

  void setPrefetchDatasource(@Nullable DataSource prefetchDatasource);

  void setMainFetchDatasource(
      @Nullable DataSource<CloseableReference<CloseableImage>> mainFetchDatasource);

  @Override
  @UiThread
  void release();

  void setImageRequest(@Nullable ImageRequest imageRequest);

  @Nullable
  Object getExtras();

  void setExtras(@Nullable Object extras);

  String getStringId();

  @Override
  void onImageDrawn(String id, ImageInfo imageInfo, DimensionsInfo dimensionsInfo);
}
