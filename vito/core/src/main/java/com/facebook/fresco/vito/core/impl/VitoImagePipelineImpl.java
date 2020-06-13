/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.content.res.Resources;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoUtils;
import com.facebook.fresco.vito.core.impl.source.VitoImageSource;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

/** Vito image pipeline to fetch an image for a given VitoImageRequest. */
public class VitoImagePipelineImpl implements VitoImagePipeline {

  private final ImagePipeline mImagePipeline;
  private final ImagePipelineUtils mImagePipelineUtils;

  public VitoImagePipelineImpl(ImagePipeline imagePipeline, ImagePipelineUtils imagePipelineUtils) {
    mImagePipeline = imagePipeline;
    mImagePipelineUtils = imagePipelineUtils;
  }

  @Override
  public VitoImageRequest createImageRequest(
      Resources resources, ImageSource imageSource, @Nullable ImageOptions options) {
    if (options == null) {
      options = ImageOptions.defaults();
    }
    if (!(imageSource instanceof VitoImageSource)) {
      throw new IllegalArgumentException("ImageSource not supported: " + imageSource);
    }
    VitoImageSource vitoImageSource = (VitoImageSource) imageSource;
    CacheKey finalImageCacheKey = null;
    ImageRequest finalImageRequest =
        vitoImageSource.maybeExtractFinalImageRequest(mImagePipelineUtils, options);

    if (finalImageRequest != null) {
      finalImageCacheKey = mImagePipeline.getCacheKey(finalImageRequest, null);
    }
    return new VitoImageRequest(
        resources, imageSource, options, finalImageRequest, finalImageCacheKey);
  }

  @Override
  @Nullable
  public CloseableReference<CloseableImage> getCachedImage(VitoImageRequest imageRequest) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("VitoImagePipeline#getCachedImage");
    }
    try {
      CloseableReference<CloseableImage> cachedImageReference =
          mImagePipeline.getCachedImage(imageRequest.finalImageCacheKey);
      if (CloseableReference.isValid(cachedImageReference)) {
        return cachedImageReference;
      }
      return null;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  public DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(
      final VitoImageRequest imageRequest,
      final @Nullable Object callerContext,
      final @Nullable RequestListener requestListener,
      final long uiComponentId) {
    if (!(imageRequest.imageSource instanceof VitoImageSource)) {
      return DataSources.immediateFailedDataSource(
          new IllegalArgumentException("Unknown ImageSource " + imageRequest.imageSource));
    }
    VitoImageSource vitoImageSource = (VitoImageSource) imageRequest.imageSource;
    final String stringId = VitoUtils.getStringId(uiComponentId);
    return vitoImageSource
        .createDataSourceSupplier(
            mImagePipeline,
            mImagePipelineUtils,
            imageRequest.imageOptions,
            callerContext,
            requestListener,
            stringId)
        .get();
  }
}
