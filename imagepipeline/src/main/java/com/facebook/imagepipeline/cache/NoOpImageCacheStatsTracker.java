/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

/**
 * Class that does no stats tracking at all
 */
public class NoOpImageCacheStatsTracker implements ImageCacheStatsTracker {
  private static NoOpImageCacheStatsTracker sInstance = null;

  private NoOpImageCacheStatsTracker() {
  }

  public static synchronized NoOpImageCacheStatsTracker getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpImageCacheStatsTracker();
    }
    return sInstance;
  }

  @Override
  public void onBitmapCachePut() {
  }

  @Override
  public void onBitmapCacheHit() {
  }

  @Override
  public void onBitmapCacheMiss() {
  }

  @Override
  public void onMemoryCachePut() {
  }

  @Override
  public void onMemoryCacheHit() {
  }

  @Override
  public void onMemoryCacheMiss() {
  }

  @Override
  public void onStagingAreaHit() {
  }

  @Override
  public void onStagingAreaMiss() {
  }

  @Override
  public void onDiskCacheHit() {
  }

  @Override
  public void onDiskCacheMiss() {
  }

  @Override
  public void onDiskCacheGetFail() {
  }

  @Override
  public void registerBitmapMemoryCache(CountingMemoryCache<?, ?> bitmapMemoryCache) {
  }

  @Override
  public void registerEncodedMemoryCache(CountingMemoryCache<?, ?> encodedMemoryCache) {
  }
}
