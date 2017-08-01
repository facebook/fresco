/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.cache;

import android.net.Uri;
import javax.annotation.Nullable;

/**
 * Experimental interface for a helper to extract media IDs from URLs. This can be useful in cases
 * where manually updating all image requests may carry too much overhead initially.
 *
 * <p> Implementations must be careful not to provide identical media IDs for images which will be
 * returned with different aspect ratios or cropping.
 */
public interface MediaIdExtractor {

  /**
   * Extracts the media ID, if possible, from the provided URI.
   *
   * @param uri the image request's URI
   * @return the extracted ID, or null
   */
  @Nullable
  String getMediaIdFrom(Uri uri);
}
