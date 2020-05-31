/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.content.res.Resources;
import android.net.Uri;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

/** Vito image pipeline to fetch an image for a given VitoImageRequest. */
public class VitoImagePipelineImpl implements VitoImagePipeline {

  public static final NullPointerException NO_REQUEST_EXCEPTION =
      new NullPointerException("No image request was specified!");

  private final ImagePipeline mImagePipeline;
  private final ImagePipelineUtils mImagePipelineUtils;

  public VitoImagePipelineImpl(ImagePipeline imagePipeline, ImagePipelineUtils imagePipelineUtils) {
    mImagePipeline = imagePipeline;
    mImagePipelineUtils = imagePipelineUtils;
  }

  @Override
  public VitoImageRequest createImageRequest(
      Resources resources,
      @Nullable Uri uri,
      @Nullable MultiUri multiUri,
      @Nullable ImageOptions options,
      @Nullable Object callerContext) {
    if (options == null) {
      options = ImageOptions.defaults();
    }
    ImageRequest imageRequest;

    if (multiUri != null) {
      // For multi URI, we can consider the high res image request as the final image request.
      // If the user specified a first available list of image requests, we just skip this section.
      imageRequest = multiUri.getHighResImageRequest();
    } else {
      imageRequest = mImagePipelineUtils.buildImageRequest(uri, options);
    }
    return new VitoImageRequest(
        resources,
        uri,
        multiUri,
        options,
        imageRequest,
        mImagePipeline.getCacheKey(imageRequest, callerContext));
  }

  @Override
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

  @Override
  public DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable RequestListener requestListener,
      long uiComponentId) {
    if (imageRequest.multiUri != null) {
      return ImagePipelineMultiUriHelper.getMultiUriDatasourceSupplier(
              mImagePipeline,
              imageRequest.multiUri,
              callerContext,
              requestListener, // TODO: CHECK IF THIS IS CORRECT
              VitoUtils.getStringId(uiComponentId))
          .get();
    } else if (imageRequest.imageRequest != null) {
      return mImagePipeline.fetchDecodedImage(
          imageRequest.imageRequest,
          callerContext,
          ImageRequest.RequestLevel.FULL_FETCH,
          requestListener, // TODO: Check if this is correct
          VitoUtils.getStringId(uiComponentId));
    } else {
      return DataSources.immediateFailedDataSource(NO_REQUEST_EXCEPTION);
    }
  }
}
