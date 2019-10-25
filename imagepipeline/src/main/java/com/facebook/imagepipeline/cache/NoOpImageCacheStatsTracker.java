/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;

/** Class that does no stats tracking at all */
public class NoOpImageCacheStatsTracker implements ImageCacheStatsTracker {
  private static NoOpImageCacheStatsTracker sInstance = null;

  private NoOpImageCacheStatsTracker() {}

  public static synchronized NoOpImageCacheStatsTracker getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpImageCacheStatsTracker();
    }
    return sInstance;
  }

  @Override
  public void onBitmapCachePut(CacheKey cacheKey) {}

  @Override
  public void onBitmapCacheHit(CacheKey cacheKey) {}

  @Override
  public void onBitmapCacheMiss(CacheKey cacheKey) {}

  @Override
  public void onMemoryCachePut(CacheKey cacheKey) {}

  @Override
  public void onMemoryCacheHit(CacheKey cacheKey) {}

  @Override
  public void onMemoryCacheMiss(CacheKey cacheKey) {}

  @Override
  public void onStagingAreaHit(CacheKey cacheKey) {}

  @Override
  public void onStagingAreaMiss(CacheKey cacheKey) {}

  @Override
  public void onDiskCacheHit(CacheKey cacheKey) {}

  @Override
  public void onDiskCacheMiss(CacheKey cacheKey) {}

  @Override
  public void onDiskCacheGetFail(CacheKey cacheKey) {}

  @Override
  public void onDiskCachePut(CacheKey cacheKey) {}

  @Override
  public void registerBitmapMemoryCache(MemoryCache<?, ?> bitmapMemoryCache) {}

  @Override
  public void registerEncodedMemoryCache(MemoryCache<?, ?> encodedMemoryCache) {}
}
