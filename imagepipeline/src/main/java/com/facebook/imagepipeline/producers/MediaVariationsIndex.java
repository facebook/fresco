/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.List;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.MediaVariations;

import bolts.Task;

public interface MediaVariationsIndex {

  Task<List<MediaVariations.Variant>> getCachedVariants(String mediaId);

  void saveCachedVariant(
      String mediaId,
      CacheKey cacheKey,
      EncodedImage encodedImage);
}
