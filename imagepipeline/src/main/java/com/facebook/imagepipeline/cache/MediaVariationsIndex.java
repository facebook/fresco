/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import bolts.Task;
import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.MediaVariations;

public interface MediaVariationsIndex {

  Task<MediaVariations> getCachedVariants(
      String mediaId,
      MediaVariations.Builder mediaVariationsBuilder);

  void saveCachedVariant(
      String mediaId,
      ImageRequest.CacheChoice cacheChoice,
      CacheKey cacheKey,
      EncodedImage encodedImage);
}
