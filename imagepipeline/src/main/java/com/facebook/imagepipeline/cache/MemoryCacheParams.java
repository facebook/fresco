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
 * Configuration for a memory cache.
 */
public class MemoryCacheParams {

  public final int maxCacheSize;
  public final int maxCacheEntries;
  public final int maxEvictionQueueSize;
  public final int maxEvictionQueueEntries;
  public final int maxCacheEntrySize;

  /**
   * Pass arguments to control the cache's behavior in the constructor.
   *
   * @param maxCacheSize The maximum size of the cache, in bytes.
   * @param maxCacheEntries The maximum number of items that can live in the cache.
   * @param maxEvictionQueueSize The eviction queue is an area of memory that stores items ready
   *                             for eviction but have not yet been deleted. This is the maximum
   *                             size of that queue in bytes.
   * @param maxEvictionQueueEntries The maximum number of entries in the eviction queue.
   * @param maxCacheEntrySize The maximum size of a single cache entry.
   */
  public MemoryCacheParams(
      int maxCacheSize,
      int maxCacheEntries,
      int maxEvictionQueueSize,
      int maxEvictionQueueEntries,
      int maxCacheEntrySize) {
    this.maxCacheSize = maxCacheSize;
    this.maxCacheEntries = maxCacheEntries;
    this.maxEvictionQueueSize = maxEvictionQueueSize;
    this.maxEvictionQueueEntries = maxEvictionQueueEntries;
    this.maxCacheEntrySize = maxCacheEntrySize;
  }
}
