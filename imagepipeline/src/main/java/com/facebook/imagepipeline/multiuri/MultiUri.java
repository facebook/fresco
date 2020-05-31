/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.multiuri;

import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nullable;

/**
 * Data class to enable using functionality of {@link
 * com.facebook.datasource.IncreasingQualityDataSourceSupplier} and/or {@link
 * com.facebook.datasource.FirstAvailableDataSourceSupplier} with Vito
 */
public class MultiUri {
  private @Nullable ImageRequest mLowResImageRequest;
  private @Nullable ImageRequest[] mMultiImageRequests;
  private @Nullable ImageRequest mHighResImageRequest;

  private MultiUri(Builder builder) {
    mLowResImageRequest = builder.mLowResImageRequest;
    mHighResImageRequest = builder.mHighResImageRequest;
    mMultiImageRequests = builder.mMultiImageRequests;
  }

  @Nullable
  public ImageRequest getLowResImageRequest() {
    return mLowResImageRequest;
  }

  @Nullable
  public ImageRequest getHighResImageRequest() {
    return mHighResImageRequest;
  }

  @Nullable
  public ImageRequest[] getMultiImageRequests() {
    return mMultiImageRequests;
  }

  public static MultiUri.Builder create() {
    return new Builder();
  }

  public static class Builder {
    private @Nullable ImageRequest mLowResImageRequest;
    private @Nullable ImageRequest mHighResImageRequest;
    private @Nullable ImageRequest[] mMultiImageRequests;

    private Builder() {}

    public MultiUri build() {
      return new MultiUri(this);
    }

    public Builder setLowResImageRequest(@Nullable ImageRequest lowResImageRequest) {
      mLowResImageRequest = lowResImageRequest;
      return this;
    }

    public Builder setHighResImageRequest(@Nullable ImageRequest highResImageRequest) {
      mHighResImageRequest = highResImageRequest;
      return this;
    }

    public Builder setImageRequests(@Nullable ImageRequest... multiImageRequests) {
      mMultiImageRequests = multiImageRequests;
      return this;
    }
  }
}
