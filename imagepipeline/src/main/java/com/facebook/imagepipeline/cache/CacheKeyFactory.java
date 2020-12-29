/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nullable;

/** Factory methods for creating cache keys for the pipeline. */
public interface CacheKeyFactory {

  /** @return {@link CacheKey} for doing bitmap cache lookups in the pipeline. */
  CacheKey getBitmapCacheKey(ImageRequest request, @Nullable Object callerContext);

  /** @return {@link CacheKey} for doing post-processed bitmap cache lookups in the pipeline. */
  CacheKey getPostprocessedBitmapCacheKey(ImageRequest request, @Nullable Object callerContext);

  /**
   * Creates a key to be used in the encoded memory and disk caches.
   *
   * <p>Implementations must return consistent values for the same request or else caches will not
   * work efficiently.
   *
   * @param request the image request to be cached or queried from cache
   * @param callerContext included for optional debugging or logging purposes only
   * @return {@link CacheKey} for doing encoded image lookups in the pipeline.
   */
  CacheKey getEncodedCacheKey(ImageRequest request, @Nullable Object callerContext);

  /**
   * Creates a key to be used in the encoded memory and disk caches.
   *
   * <p>This version of the method receives a specific URI which may differ from the one held by the
   * request. You should not consider the URI in the request.
   *
   * <p>Implementations must return consistent values for the same request or else caches will not
   * work efficiently.
   *
   * @param request the image request to be cached or queried from cache
   * @param sourceUri the URI to use for the key, which may override the one held in the request
   * @param callerContext included for optional debugging or logging purposes only
   * @return {@link CacheKey} for doing encoded image lookups in the pipeline.
   */
  CacheKey getEncodedCacheKey(ImageRequest request, Uri sourceUri, @Nullable Object callerContext);
}
