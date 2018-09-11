/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.memory.PooledByteBuffer;

public class EncodedMemoryCacheFactory {

  public static InstrumentedMemoryCache<CacheKey, PooledByteBuffer> get(
      final CountingMemoryCache<CacheKey, PooledByteBuffer> encodedCountingMemoryCache,
      final ImageCacheStatsTracker imageCacheStatsTracker) {

    imageCacheStatsTracker.registerEncodedMemoryCache(encodedCountingMemoryCache);

    MemoryCacheTracker memoryCacheTracker = new MemoryCacheTracker<CacheKey>() {
      @Override
      public void onCacheHit(CacheKey cacheKey) {
        imageCacheStatsTracker.onMemoryCacheHit(cacheKey);
      }

      @Override
      public void onCacheMiss() {
        imageCacheStatsTracker.onMemoryCacheMiss();
      }

      @Override
      public void onCachePut() {
        imageCacheStatsTracker.onMemoryCachePut();
      }
    };

    return new InstrumentedMemoryCache<>(encodedCountingMemoryCache, memoryCacheTracker);
  }
}
