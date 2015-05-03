/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.image.CloseableImage;

public class BitmapMemoryCacheFactory {

  public static MemoryCache<CacheKey, CloseableImage> get(
    final CountingMemoryCache<CacheKey, CloseableImage> bitmapCountingMemoryCache,
    final ImageCacheStatsTracker imageCacheStatsTracker) {

    imageCacheStatsTracker.registerBitmapMemoryCache(bitmapCountingMemoryCache);

    MemoryCacheTracker memoryCacheTracker = new MemoryCacheTracker() {
      @Override
      public void onCacheHit() {
        imageCacheStatsTracker.onBitmapCacheHit();
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
