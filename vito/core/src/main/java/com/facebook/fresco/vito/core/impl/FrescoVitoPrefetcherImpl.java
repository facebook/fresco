/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.net.Uri;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.PrefetchTarget;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.CancellationException;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoVitoPrefetcherImpl implements FrescoVitoPrefetcher {

  private static final Throwable NULL_IMAGE_MESSAGE =
      new NullPointerException("No image to prefetch.");

  private final ImagePipeline mImagePipeline;
  private final ImagePipelineUtils mImagePipelineUtils;
  private final @Nullable CallerContextVerifier mCallerContextVerifier;

  public FrescoVitoPrefetcherImpl(
      ImagePipeline imagePipeline,
      ImagePipelineUtils imagePipelineUtils,
      @Nullable CallerContextVerifier callerContextVerifier) {
    mImagePipeline = imagePipeline;
    mImagePipelineUtils = imagePipelineUtils;
    mCallerContextVerifier = callerContextVerifier;
  }

  @Override
  public DataSource<Void> prefetch(
      PrefetchTarget prefetchTarget,
      final Uri uri,
      final @Nullable ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final String callsite) {
    switch (prefetchTarget) {
      case MEMORY_DECODED:
        return prefetchToBitmapCache(uri, imageOptions, callerContext, callsite);
      case MEMORY_ENCODED:
        return prefetchToEncodedCache(uri, imageOptions, callerContext, callsite);
      case DISK:
        return prefetchToDiskCache(uri, imageOptions, callerContext, callsite);
    }
    return DataSources.immediateFailedDataSource(
        new CancellationException("Prefetching is not enabled"));
  }

  @Override
  public DataSource<Void> prefetchToBitmapCache(
      final Uri uri,
      final @Nullable DecodedImageOptions imageOptions,
      final @Nullable Object callerContext,
      final String callsite) {
    final ImageRequest imageRequest =
        mImagePipelineUtils.buildImageRequest(
            uri, imageOptions != null ? imageOptions : ImageOptions.defaults());
    return prefetch(PrefetchTarget.MEMORY_DECODED, imageRequest, callerContext, null);
  }

  @Override
  public DataSource<Void> prefetchToEncodedCache(
      final Uri uri,
      final @Nullable EncodedImageOptions imageOptions,
      final @Nullable Object callerContext,
      final String callsite) {
    final ImageRequest imageRequest =
        mImagePipelineUtils.buildEncodedImageRequest(
            uri, imageOptions != null ? imageOptions : ImageOptions.defaults());
    return prefetch(PrefetchTarget.MEMORY_ENCODED, imageRequest, callerContext, null);
  }

  @Override
  public DataSource<Void> prefetchToDiskCache(
      final Uri uri,
      final @Nullable ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final String callsite) {
    final ImageRequest imageRequest =
        mImagePipelineUtils.buildEncodedImageRequest(
            uri, imageOptions != null ? imageOptions : ImageOptions.defaults());
    return prefetch(PrefetchTarget.DISK, imageRequest, callerContext, null);
  }

  @Override
  public DataSource<Void> prefetch(
      final PrefetchTarget prefetchTarget,
      final VitoImageRequest imageRequest,
      @Nullable final Object callerContext,
      @Nullable final RequestListener requestListener,
      final String callsite) {
    return prefetch(prefetchTarget, imageRequest.finalImageRequest, callerContext, requestListener);
  }

  private DataSource<Void> prefetch(
      final PrefetchTarget prefetchTarget,
      @Nullable final ImageRequest imageRequest,
      @Nullable final Object callerContext,
      @Nullable final RequestListener requestListener) {
    if (mCallerContextVerifier != null) {
      mCallerContextVerifier.verifyCallerContext(callerContext, false);
    }
    if (imageRequest == null) {
      return DataSources.immediateFailedDataSource(NULL_IMAGE_MESSAGE);
    }
    switch (prefetchTarget) {
      case MEMORY_DECODED:
        return mImagePipeline.prefetchToBitmapCache(imageRequest, callerContext, requestListener);
      case MEMORY_ENCODED:
        return mImagePipeline.prefetchToEncodedCache(imageRequest, callerContext, requestListener);
      case DISK:
        return mImagePipeline.prefetchToDiskCache(imageRequest, callerContext, requestListener);
    }
    return DataSources.immediateFailedDataSource(
        new CancellationException("Prefetching is not enabled"));
  }
}
