/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.cache.common.CacheKey
import com.facebook.common.internal.Predicate
import com.facebook.common.references.CloseableReference

/** Shared encoded-memory-cache boundary for pipeline-specific adapters. */
class UnifiedEncodedMemoryCache<V>(
    private val backingCache: MemoryCache<CacheKey, V>,
) {

  operator fun get(cacheKey: CacheKey): CloseableReference<V>? = backingCache[cacheKey]

  fun put(cacheKey: CacheKey, value: CloseableReference<V>): CloseableReference<V>? =
      backingCache.cache(cacheKey, value)

  @Suppress("UNCHECKED_CAST")
  fun remove(cacheKey: CacheKey): Int {
    val exactKeyCache = backingCache as? MemoryCacheWithExactKeyRemoval<CacheKey>
    return exactKeyCache?.remove(cacheKey)
        ?: backingCache.removeAll(Predicate { candidateKey -> candidateKey == cacheKey })
  }

  fun removeAll(predicate: Predicate<CacheKey>): Int = backingCache.removeAll(predicate)
}
