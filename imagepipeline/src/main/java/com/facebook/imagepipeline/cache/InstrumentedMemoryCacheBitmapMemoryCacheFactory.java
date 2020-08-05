/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.image.CloseableImage;

public class InstrumentedMemoryCacheBitmapMemoryCacheFactory {

  public static InstrumentedMemoryCache<CacheKey, CloseableImage> get(
      final MemoryCache<CacheKey, CloseableImage> bitmapMemoryCache,
      final ImageCacheStatsTracker imageCacheStatsTracker) {

    imageCacheStatsTracker.registerBitmapMemoryCache(bitmapMemoryCache);

    MemoryCacheTracker memoryCacheTracker =
        new MemoryCacheTracker<CacheKey>() {
          @Override
          public void onCacheHit(CacheKey cacheKey) {
            imageCacheStatsTracker.onBitmapCacheHit(cacheKey);
          }

          @Override
          public void onCacheMiss(CacheKey cacheKey) {
            imageCacheStatsTracker.onBitmapCacheMiss(cacheKey);
          }

          @Override
          public void onCachePut(CacheKey cacheKey) {
            imageCacheStatsTracker.onBitmapCachePut(cacheKey);
          }
        };

    return new InstrumentedMemoryCache<>(bitmapMemoryCache, memoryCacheTracker);
  }
}
