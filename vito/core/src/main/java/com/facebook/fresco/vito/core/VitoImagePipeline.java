/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import android.net.Uri;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

/** Vito image pipeline to fetch an image for a given VitoImageRequest. */
public class VitoImagePipeline {

  private final ImagePipeline mImagePipeline;
  private final ImagePipelineUtils mImagePipelineUtils;

  public VitoImagePipeline(ImagePipeline imagePipeline, ImagePipelineUtils imagePipelineUtils) {
    mImagePipeline = imagePipeline;
    mImagePipelineUtils = imagePipelineUtils;
  }

  public VitoImageRequest createImageRequest(
      Resources resources,
      @Nullable Uri uri,
      @Nullable MultiUri multiUri,
      @Nullable ImageOptions options,
      @Nullable Object callerContext) {
    if (options == null) {
      options = ImageOptions.defaults();
    }
    ImageRequest imageRequest = mImagePipelineUtils.buildImageRequest(uri, options);
    return new VitoImageRequest(
        resources,
        uri,
        multiUri,
        options,
        imageRequest,
        mImagePipeline.getCacheKey(imageRequest, callerContext));
  }

  @Nullable
  public CloseableReference<CloseableImage> getCachedImage(VitoImageRequest imageRequest) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("VitoImagePipeline#getCachedImage");
    }
    try {
      CloseableReference<CloseableImage> cachedImageReference =
          mImagePipeline.getCachedImage(imageRequest.cacheKey);
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

  public DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable RequestListener requestListener,
      @Nullable long uiComponentId) {
    return mImagePipeline.fetchDecodedImage(
        imageRequest.imageRequest,
        callerContext,
        ImageRequest.RequestLevel.FULL_FETCH,
        mImagePipeline.getRequestListenerForRequest(imageRequest.imageRequest, requestListener),
        VitoUtils.getStringId(uiComponentId));
  }
}
