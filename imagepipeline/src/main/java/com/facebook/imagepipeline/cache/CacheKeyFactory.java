/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import java.util.List;

import android.net.Uri;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Factory methods for creating cache keys for the pipeline.
 */
public interface CacheKeyFactory {

  /**
   * @return {@link CacheKey} for doing bitmap cache lookups in the pipeline.
   */
  CacheKey getBitmapCacheKey(ImageRequest request);

  /**
   * @return {@link CacheKey} for doing post-processed bitmap cache lookups in the pipeline.
   */
  CacheKey getPostprocessedBitmapCacheKey(ImageRequest request);

  /**
   * @return {@link CacheKey} for doing encoded image lookups in the pipeline.
   * @deprecated Will be removed in the next release of Fresco.
   */
   @Deprecated
  CacheKey getEncodedCacheKey(ImageRequest request);

  /**
   * Return a series of one more cache keys that can be used to look up in the
   * encoded-memory and disk caches.
   *
   * <p>Reads will try all the keys in the list, in order. Writes will use only the
   * first key in the list.
   *
   * <p>Most implementations should return a list of length 1. Use more than one only if
   * your app is transitioning between key generation algorithms.
   *
   * <p>Avoid having duplicates in this list - they will result in extra disk reads.
   *
   * @return {@link CacheKey}s for doing encoded image lookups in the pipeline.
   */
  List<CacheKey> getEncodedCacheKeys(ImageRequest request);
}
