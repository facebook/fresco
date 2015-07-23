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

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Factory methods for creating cache keys for the pipeline.
 */
public interface CacheKeyFactory {

  /**
   * @return {@link CacheKey} for doing bitmap cache lookups in the pipeline.
   */
  public CacheKey getBitmapCacheKey(ImageRequest request);

  /**
   * @return {@link CacheKey} for doing post-processed bitmap cache lookups in the pipeline.
   */
  public CacheKey getPostprocessedBitmapCacheKey(ImageRequest request);

  /**
   * @return {@link CacheKey} for doing encoded image lookups in the pipeline.
   */
  public CacheKey getEncodedCacheKey(ImageRequest request);

  /**
   * @return a {@link String} that unambiguously indicates the source of the image.
   */
  public Uri getCacheKeySourceUri(Uri sourceUri);
}
