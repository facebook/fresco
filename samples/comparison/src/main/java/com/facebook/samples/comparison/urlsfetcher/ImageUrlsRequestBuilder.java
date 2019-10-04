/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.urlsfetcher;

import com.facebook.common.internal.Preconditions;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds ImageUrlsRequest.
 *
 * <p>Use addImageFormat to specify what image types you are interested in
 */
public class ImageUrlsRequestBuilder {
  private final String mEndpointUrl;
  Map<ImageFormat, ImageSize> mRequestedImageFormats;

  public ImageUrlsRequestBuilder(final String endpointUrl) {
    mEndpointUrl = Preconditions.checkNotNull(endpointUrl);
    mRequestedImageFormats = new HashMap<>();
  }

  /**
   * Adds imageFormat to the set of image formats you want to download. imageSize specify
   * server-side resize options.
   */
  public ImageUrlsRequestBuilder addImageFormat(ImageFormat imageFormat, ImageSize imageSize) {
    mRequestedImageFormats.put(imageFormat, imageSize);
    return this;
  }

  public ImageUrlsRequest build() {
    ImageUrlsRequest request = new ImageUrlsRequest(mEndpointUrl, mRequestedImageFormats);
    mRequestedImageFormats = null;
    return request;
  }
}
