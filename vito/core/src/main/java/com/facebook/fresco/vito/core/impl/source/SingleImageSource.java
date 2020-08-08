/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.source;

import androidx.annotation.Nullable;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class SingleImageSource implements VitoImageSource {

  private final ImageRequest mMainImageRequest;
  private final ImageRequest.RequestLevel mRequestLevelForFetch;

  protected SingleImageSource(ImageRequest mainImageRequest) {
    this(mainImageRequest, ImageRequest.RequestLevel.FULL_FETCH);
  }

  protected SingleImageSource(
      ImageRequest mainImageRequest, ImageRequest.RequestLevel requestLevelForFetch) {
    mMainImageRequest = mainImageRequest;
    mRequestLevelForFetch = requestLevelForFetch;
  }

  public ImageRequest getImageRequest() {
    return mMainImageRequest;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    SingleImageSource other = (SingleImageSource) obj;
    return mMainImageRequest.equals(other.mMainImageRequest);
  }

  @Override
  public int hashCode() {
    return mMainImageRequest.hashCode();
  }

  @Override
  @Nullable
  public ImageRequest maybeExtractFinalImageRequest(
      ImagePipelineUtils imagePipelineUtils, ImageOptions imageOptions) {
    return imagePipelineUtils.wrapDecodedImageRequest(mMainImageRequest, imageOptions);
  }

  @Override
  public Supplier<DataSource<CloseableReference<CloseableImage>>> createDataSourceSupplier(
      final ImagePipeline imagePipeline,
      final ImagePipelineUtils imagePipelineUtils,
      final ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final @Nullable RequestListener requestListener,
      final String uiComponentId) {
    return new Supplier<DataSource<CloseableReference<CloseableImage>>>() {
      @Override
      public DataSource<CloseableReference<CloseableImage>> get() {
        final ImageRequest imageRequest =
            maybeExtractFinalImageRequest(imagePipelineUtils, imageOptions);
        if (imageRequest != null) {
          return imagePipeline.fetchDecodedImage(
              imageRequest,
              callerContext,
              mRequestLevelForFetch,
              requestListener, // TODO: Check if this is correct !!
              uiComponentId);
        } else {
          return DataSources.immediateFailedDataSource(
              new NullPointerException(
                  "Could not extract image request from: " + SingleImageSource.this));
        }
      }
    };
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("imageRequest", mMainImageRequest).toString();
  }
}
