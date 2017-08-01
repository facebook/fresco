/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import bolts.Task;
import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.MediaVariations;

public class NoOpMediaVariationsIndex implements MediaVariationsIndex {

  @Override
  public Task<MediaVariations> getCachedVariants(
      String mediaId,
      MediaVariations.Builder mediaVariationsBuilder) {
    return Task.forResult(null);
  }

  @Override
  public void saveCachedVariant(
      String mediaId,
      ImageRequest.CacheChoice cacheChoice,
      CacheKey cacheKey,
      EncodedImage encodedImage) {
    // no-op
  }
}
