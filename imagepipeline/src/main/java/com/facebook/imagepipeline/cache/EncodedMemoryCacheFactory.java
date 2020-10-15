/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public class EncodedMemoryCacheFactory {

  public static InstrumentedMemoryCache<CacheKey, PooledByteBuffer> get(
      final MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache,
      final ImageCacheStatsTracker imageCacheStatsTracker) {

    imageCacheStatsTracker.registerEncodedMemoryCache(encodedMemoryCache);

    MemoryCacheTracker memoryCacheTracker =
        new MemoryCacheTracker<CacheKey>() {
          @Override
          public void onCacheHit(CacheKey cacheKey) {
            imageCacheStatsTracker.onMemoryCacheHit(cacheKey);
          }

          @Override
          public void onCacheMiss(CacheKey cacheKey) {
            imageCacheStatsTracker.onMemoryCacheMiss(cacheKey);
          }

          @Override
          public void onCachePut(CacheKey cacheKey) {
            imageCacheStatsTracker.onMemoryCachePut(cacheKey);
          }
        };

    return new InstrumentedMemoryCache<>(encodedMemoryCache, memoryCacheTracker);
  }
}
