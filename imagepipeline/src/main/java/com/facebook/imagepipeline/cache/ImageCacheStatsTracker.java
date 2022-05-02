/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.infer.annotation.Nullsafe;

/**
 * Interface for stats tracking for the image cache.
 *
 * <p>An implementation of this interface, passed to {@link
 * com.facebook.imagepipeline.core.ImagePipelineConfig}, will be notified for each of the following
 * cache events. Use this to keep cache stats for your app.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface ImageCacheStatsTracker {

  /** Called whenever decoded images are put into the bitmap cache. */
  void onBitmapCachePut(CacheKey cacheKey);

  /** Called on a bitmap cache hit. */
  void onBitmapCacheHit(CacheKey cacheKey);

  /** Called on a bitmap cache miss. */
  void onBitmapCacheMiss(CacheKey cacheKey);

  /** Called whenever encoded images are put into the encoded memory cache. */
  void onMemoryCachePut(CacheKey cacheKey);

  /** Called on an encoded memory cache hit. */
  void onMemoryCacheHit(CacheKey cacheKey);

  /** Called on an encoded memory cache hit. */
  void onMemoryCacheMiss(CacheKey cacheKey);

  /**
   * Called on an staging area hit.
   *
   * <p>The staging area stores encoded images. It gets the images before they are written to disk
   * cache.
   */
  void onStagingAreaHit(CacheKey cacheKey);

  /** Called on a staging area miss hit. */
  void onStagingAreaMiss(CacheKey cacheKey);

  /** Called on a disk cache hit. */
  void onDiskCacheHit(CacheKey cacheKey);

  /** Called on a disk cache miss. */
  void onDiskCacheMiss(CacheKey cacheKey);

  /** Called if an exception is thrown on a disk cache read. */
  void onDiskCacheGetFail(CacheKey cacheKey);

  /** called whenever new files are written to disk */
  void onDiskCachePut(CacheKey cacheKey);

  /**
   * Registers a bitmap cache with this tracker.
   *
   * <p>Use this method if you need access to the cache itself to compile your stats.
   */
  void registerBitmapMemoryCache(MemoryCache<?, ?> bitmapMemoryCache);

  /**
   * Registers an encoded memory cache with this tracker.
   *
   * <p>Use this method if you need access to the cache itself to compile your stats.
   */
  void registerEncodedMemoryCache(MemoryCache<?, ?> encodedMemoryCache);
}
