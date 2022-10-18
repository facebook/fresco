/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.cache.common.CacheKey

/**
 * Interface for stats tracking for the image cache.
 *
 * An implementation of this interface, passed to
 * [com.facebook.imagepipeline.core.ImagePipelineConfig], will be notified for each of the following
 * cache events. Use this to keep cache stats for your app.
 */
interface ImageCacheStatsTracker {

  /** Called whenever decoded images are put into the bitmap cache. */
  fun onBitmapCachePut(cacheKey: CacheKey)

  /** Called on a bitmap cache hit. */
  fun onBitmapCacheHit(cacheKey: CacheKey)

  /** Called on a bitmap cache miss. */
  fun onBitmapCacheMiss(cacheKey: CacheKey)

  /** Called whenever encoded images are put into the encoded memory cache. */
  fun onMemoryCachePut(cacheKey: CacheKey)

  /** Called on an encoded memory cache hit. */
  fun onMemoryCacheHit(cacheKey: CacheKey)

  /** Called on an encoded memory cache hit. */
  fun onMemoryCacheMiss(cacheKey: CacheKey)

  /**
   * Called on an staging area hit.
   *
   * The staging area stores encoded images. It gets the images before they are written to disk
   * cache.
   */
  fun onStagingAreaHit(cacheKey: CacheKey)

  /** Called on a staging area miss hit. */
  fun onStagingAreaMiss(cacheKey: CacheKey)

  /** Called on a disk cache hit. */
  fun onDiskCacheHit(cacheKey: CacheKey)

  /** Called on a disk cache miss. */
  fun onDiskCacheMiss(cacheKey: CacheKey)

  /** Called if an exception is thrown on a disk cache read. */
  fun onDiskCacheGetFail(cacheKey: CacheKey)

  /** called whenever new files are written to disk */
  fun onDiskCachePut(cacheKey: CacheKey)

  /**
   * Registers a bitmap cache with this tracker.
   *
   * Use this method if you need access to the cache itself to compile your stats.
   */
  fun registerBitmapMemoryCache(bitmapMemoryCache: MemoryCache<*, *>)

  /**
   * Registers an encoded memory cache with this tracker.
   *
   * Use this method if you need access to the cache itself to compile your stats.
   */
  fun registerEncodedMemoryCache(encodedMemoryCache: MemoryCache<*, *>)
}
