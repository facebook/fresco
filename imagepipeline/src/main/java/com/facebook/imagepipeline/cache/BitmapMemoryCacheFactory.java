/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.image.CloseableImage;

public class BitmapMemoryCacheFactory {

  public static InstrumentedMemoryCache<CacheKey, CloseableImage> get(
      final CountingMemoryCache<CacheKey, CloseableImage> bitmapCountingMemoryCache,
      final ImageCacheStatsTracker imageCacheStatsTracker) {

    imageCacheStatsTracker.registerBitmapMemoryCache(bitmapCountingMemoryCache);

    MemoryCacheTracker memoryCacheTracker = new MemoryCacheTracker<CacheKey>() {
      @Override
      public void onCacheHit(CacheKey cacheKey) {
        imageCacheStatsTracker.onBitmapCacheHit(cacheKey);
      }

      @Override
      public void onCacheMiss() {
        imageCacheStatsTracker.onBitmapCacheMiss();
      }

      @Override
      public void onCachePut() {
        imageCacheStatsTracker.onBitmapCachePut();
      }
    };

    return new InstrumentedMemoryCache<>(bitmapCountingMemoryCache, memoryCacheTracker);
  }
}
