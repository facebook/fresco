/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.cache.common.CacheKey;

public class EncodedMemoryCacheFactory {

  public static MemoryCache<CacheKey, PooledByteBuffer> get(
      final CountingMemoryCache<CacheKey, PooledByteBuffer> encodedCountingMemoryCache,
      final ImageCacheStatsTracker imageCacheStatsTracker) {

    imageCacheStatsTracker.registerEncodedMemoryCache(encodedCountingMemoryCache);

    MemoryCacheTracker memoryCacheTracker = new MemoryCacheTracker() {
      @Override
      public void onCacheHit() {
        imageCacheStatsTracker.onMemoryCacheHit();
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
