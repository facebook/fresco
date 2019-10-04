/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.urlsfetcher;

import com.facebook.common.internal.Preconditions;
import java.util.Map;

/** Encapsulates url and set of image types together with corresponding resizing options. */
public class ImageUrlsRequest {
  private final String mEndpointUrl;
  Map<ImageFormat, ImageSize> mRequestedImageFormats;

  ImageUrlsRequest(final String endpointUrl, Map<ImageFormat, ImageSize> requestedTypes) {
    mEndpointUrl = Preconditions.checkNotNull(endpointUrl);
    mRequestedImageFormats = Preconditions.checkNotNull(requestedTypes);
  }

  public String getEndpointUrl() {
    return mEndpointUrl;
  }

  public ImageSize getImageSize(ImageFormat imageFormat) {
    return mRequestedImageFormats.get(imageFormat);
  }
}
