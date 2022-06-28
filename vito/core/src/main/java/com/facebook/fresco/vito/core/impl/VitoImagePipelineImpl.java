/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.middleware.Dimensions;
import com.facebook.fresco.middleware.UriModifier;
import com.facebook.fresco.ui.common.VitoUtils;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.fresco.vito.source.SingleImageSource;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Vito image pipeline to fetch an image for a given VitoImageRequest. */
@Nullsafe(Nullsafe.Mode.LOCAL)
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
    return createImageRequest(resources, imageSource, options, null);
  }

  @Override
  public VitoImageRequest createImageRequest(
      Resources resources,
      ImageSource imageSource,
      @Nullable ImageOptions options,
      @Nullable Rect viewport) {
    if (options == null) {
      options = ImageOptions.defaults();
    }

    ImageSource finalImageSource = imageSource;
    if (options.getExperimentalDynamicSize() && imageSource instanceof SingleImageSource) {
      Uri uri = ((SingleImageSource) imageSource).getUri();
      Uri maybeModifiedUri =
          UriModifier.INSTANCE.modifyUri(
              uri,
              viewport == null ? null : new Dimensions(viewport.width(), viewport.height()),
              options.getActualImageScaleType());
      if (maybeModifiedUri != uri) {
        finalImageSource = ImageSourceProvider.forUri(maybeModifiedUri);
      }
    }

    CacheKey finalImageCacheKey = null;
    ImageRequest finalImageRequest =
        ImageSourceToImagePipelineAdapter.maybeExtractFinalImageRequest(
            finalImageSource, mImagePipelineUtils, options);

    if (finalImageRequest != null) {
      finalImageCacheKey = mImagePipeline.getCacheKey(finalImageRequest, null);
    }
    return new VitoImageRequest(
        resources, finalImageSource, options, finalImageRequest, finalImageCacheKey);
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
    return ImageSourceToImagePipelineAdapter.createDataSourceSupplier(
            imageRequest.imageSource,
            mImagePipeline,
            mImagePipelineUtils,
            imageRequest.imageOptions,
            callerContext,
            requestListener,
            VitoUtils.getStringId(uiComponentId))
        .get();
  }
}
