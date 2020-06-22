/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.net.Uri;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.PrefetchTarget;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.concurrent.CancellationException;
import javax.annotation.Nullable;

public class FrescoVitoPrefetcherImpl implements FrescoVitoPrefetcher {

  private final FrescoContext mFrescoContext;

  public FrescoVitoPrefetcherImpl(FrescoContext frescoContext) {
    mFrescoContext = frescoContext;
  }

  /**
   * Prefetch an image to the given {@link PrefetchTarget}
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param prefetchTarget the target to prefetch to
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @return a DataSource that can safely be ignored.
   */
  public DataSource<Void> prefetch(
      PrefetchTarget prefetchTarget,
      final Uri uri,
      final @Nullable ImageOptions imageOptions,
      final @Nullable Object callerContext) {
    switch (prefetchTarget) {
      case MEMORY_DECODED:
        return prefetchToBitmapCache(uri, imageOptions, callerContext);
      case MEMORY_ENCODED:
        return prefetchToEncodedCache(uri, imageOptions, callerContext);
      case DISK:
        return prefetchToDiskCache(uri, imageOptions, callerContext);
    }
    return DataSources.immediateFailedDataSource(
        new CancellationException("Prefetching is not enabled"));
  }

  /**
   * Prefetch an image to the bitmap memory cache (for decoded images). In order to cancel the
   * prefetch, close the {@link DataSource} returned by this method.
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @return a DataSource that can safely be ignored.
   */
  public DataSource<Void> prefetchToBitmapCache(
      final Uri uri,
      final @Nullable DecodedImageOptions imageOptions,
      final @Nullable Object callerContext) {
    mFrescoContext.verifyCallerContext(callerContext);
    ImageRequest imageRequest =
        mFrescoContext
            .getImagePipelineUtils()
            .buildImageRequest(uri, imageOptions != null ? imageOptions : ImageOptions.defaults());
    return mFrescoContext.getImagePipeline().prefetchToBitmapCache(imageRequest, callerContext);
  }

  /**
   * Prefetch an image to the encoded memory cache. In order to cancel the prefetch, close the
   * {@link DataSource} returned by this method.
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @return a DataSource that can safely be ignored.
   */
  public DataSource<Void> prefetchToEncodedCache(
      final Uri uri,
      final @Nullable EncodedImageOptions imageOptions,
      final @Nullable Object callerContext) {
    mFrescoContext.verifyCallerContext(callerContext);
    ImageRequest imageRequest =
        mFrescoContext
            .getImagePipelineUtils()
            .buildEncodedImageRequest(
                uri, imageOptions != null ? imageOptions : ImageOptions.defaults());
    return mFrescoContext.getImagePipeline().prefetchToEncodedCache(imageRequest, callerContext);
  }

  /**
   * Prefetch an image to the disk cache. In order to cancel the prefetch, close the {@link
   * DataSource} returned by this method.
   *
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   *
   * @param uri the image URI to prefetch
   * @param imageOptions the image options used to display the image
   * @param callerContext the caller context for the given image
   * @return a DataSource that can safely be ignored.
   */
  public DataSource<Void> prefetchToDiskCache(
      final Uri uri,
      final @Nullable ImageOptions imageOptions,
      final @Nullable Object callerContext) {
    mFrescoContext.verifyCallerContext(callerContext);
    ImageRequest imageRequest =
        mFrescoContext
            .getImagePipelineUtils()
            .buildEncodedImageRequest(
                uri, imageOptions != null ? imageOptions : ImageOptions.defaults());
    return mFrescoContext.getImagePipeline().prefetchToDiskCache(imageRequest, callerContext);
  }

  @Nullable
  public DataSource<Void> prefetch(
      final PrefetchTarget prefetchTarget,
      final VitoImageRequest imageRequest,
      @Nullable final Object callerContext) {
    final ImageRequest finalImageRequest = imageRequest.finalImageRequest;
    if (finalImageRequest == null) {
      return null;
    }
    mFrescoContext.verifyCallerContext(callerContext);
    final ImagePipeline pipeline = mFrescoContext.getImagePipeline();
    switch (prefetchTarget) {
      case MEMORY_DECODED:
        return pipeline.prefetchToBitmapCache(finalImageRequest, callerContext);
      case MEMORY_ENCODED:
        return pipeline.prefetchToEncodedCache(finalImageRequest, callerContext);
      case DISK:
        return pipeline.prefetchToDiskCache(finalImageRequest, callerContext);
    }
    return DataSources.immediateFailedDataSource(
        new CancellationException("Prefetching is not enabled"));
  }
}
