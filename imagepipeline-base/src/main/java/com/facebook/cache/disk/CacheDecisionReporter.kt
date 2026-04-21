/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import com.facebook.cache.common.CacheKey

/**
 * Optional interface for [FileCache] implementations that wrap multiple underlying caches and can
 * report which underlying cache was used for an operation.
 *
 * After a read operation, [consumeReadDecision] returns the disk cache ID of the underlying cache
 * that served the data (e.g., "default" for a main cache hit). The decision is consumed (removed)
 * on read so that each lookup result is reported exactly once.
 *
 * For writes, [writeTargetCacheId] returns the ID of the primary write target.
 *
 * Producers use this to overwrite the `disk_cache_id` annotation on the ProducerContext with the
 * resolved underlying cache ID, giving visibility into which cache actually handled the request.
 */
interface CacheDecisionReporter {

  /**
   * Atomically reads and removes the stored read decision for the given [key].
   *
   * @return the disk cache ID of the underlying cache that served the read (e.g., "default",
   *   "offline_feed_disk_cache_config_id"), or null if no decision was recorded (cache miss or
   *   non-fallback cache).
   */
  fun consumeReadDecision(key: CacheKey): String?

  /**
   * The disk cache ID of the primary write target for this cache.
   *
   * For a fallback cache that wraps a default and offline cache, this returns the ID of the default
   * cache since writes are directed there.
   */
  val writeTargetCacheId: String
}
