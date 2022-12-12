/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import java.util.concurrent.TimeUnit
import kotlin.jvm.JvmField

/** Configuration for a memory cache. */
class MemoryCacheParams
/**
 * Pass arguments to control the cache's behavior in the constructor.
 *
 * @param maxCacheSize The maximum size of the cache, in bytes.
 * @param maxCacheEntries The maximum number of items that can live in the cache.
 * @param maxEvictionQueueSize The eviction queue is an area of memory that stores items ready for
 *   eviction but have not yet been deleted. This is the maximum size of that queue in bytes.
 * @param maxEvictionQueueEntries The maximum number of entries in the eviction queue.
 * @param maxCacheEntrySize The maximum size of a single cache entry.
 */
@JvmOverloads
constructor(
    @JvmField val maxCacheSize: Int,
    @JvmField val maxCacheEntries: Int,
    @JvmField val maxEvictionQueueSize: Int,
    @JvmField val maxEvictionQueueEntries: Int,
    @JvmField val maxCacheEntrySize: Int,
    @JvmField val paramsCheckIntervalMs: Long = TimeUnit.MINUTES.toMillis(5)
) {
  /**
   * Pass arguments to control the cache's behavior in the constructor.
   *
   * @param maxCacheSize The maximum size of the cache, in bytes.
   * @param maxCacheEntries The maximum number of items that can live in the cache.
   * @param maxEvictionQueueSize The eviction queue is an area of memory that stores items ready for
   *   eviction but have not yet been deleted. This is the maximum size of that queue in bytes.
   * @param maxEvictionQueueEntries The maximum number of entries in the eviction queue.
   * @param maxCacheEntrySize The maximum size of a single cache entry.
   * @param paramsCheckIntervalMs Interval between checking parameters for updated values in ms.
   */
}
