/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.cache.common.CacheKey
import com.facebook.common.internal.Predicate
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage

/**
 * A [MemoryCache] that routes entries between two underlying caches based on the value type:
 * [CloseableBitmap] instances go to [bitmapCache], everything else (e.g. `CloseableAnimatedImage`,
 * XML/SVG decodes, or any other non-bitmap [CloseableImage]) goes to [nonBitmapCache].
 *
 * Semantics:
 * * `cache()` inspects the value and writes to exactly one of the two backing caches. A value whose
 *   [CloseableReference.get] returns `null` is rejected (not cached) since it indicates a closed or
 *   invalid reference rather than a real image.
 * * `get()` / `inspect()` consult [bitmapCache] first, then fall through to [nonBitmapCache] on
 *   miss. The two backing caches are assumed to hold disjoint keys; a key written via this router
 *   only ever lives in one of them.
 * * `probe()` touches both caches because the caller doesn't know which side owns the key. Probes
 *   on a missing key are no-ops in [LruCountingMemoryCache], so this is cheap.
 * * `removeAll(predicate)`, `contains(...)`, `count`, `sizeInBytes`, and `trim(...)` fan out to
 *   both backing caches.
 *
 * Used when [ImagePipelineExperiments.useSeparateNonBitmapImageCache] is enabled, to A/B-test the
 * effect of splitting non-`CloseableBitmap` entries out of the shared bitmap memory cache.
 */
class RoutingBitmapMemoryCache(
    private val bitmapCache: MemoryCache<CacheKey, CloseableImage>,
    private val nonBitmapCache: MemoryCache<CacheKey, CloseableImage>,
) : MemoryCache<CacheKey, CloseableImage> {

  override fun cache(
      key: CacheKey,
      value: CloseableReference<CloseableImage>,
  ): CloseableReference<CloseableImage>? {
    val image = value.get() ?: return null
    return if (image is CloseableBitmap) {
      bitmapCache.cache(key, value)
    } else {
      nonBitmapCache.cache(key, value)
    }
  }

  override fun get(key: CacheKey): CloseableReference<CloseableImage>? =
      bitmapCache[key] ?: nonBitmapCache[key]

  override fun inspect(key: CacheKey): CloseableImage? =
      bitmapCache.inspect(key) ?: nonBitmapCache.inspect(key)

  override fun probe(key: CacheKey) {
    bitmapCache.probe(key)
    nonBitmapCache.probe(key)
  }

  override fun removeAll(predicate: Predicate<CacheKey>): Int =
      bitmapCache.removeAll(predicate) + nonBitmapCache.removeAll(predicate)

  override fun contains(predicate: Predicate<CacheKey>): Boolean =
      bitmapCache.contains(predicate) || nonBitmapCache.contains(predicate)

  override fun contains(key: CacheKey): Boolean =
      bitmapCache.contains(key) || nonBitmapCache.contains(key)

  override val count: Int
    get() = bitmapCache.count + nonBitmapCache.count

  override val sizeInBytes: Int
    get() = bitmapCache.sizeInBytes + nonBitmapCache.sizeInBytes

  override fun trim(trimType: MemoryTrimType) {
    bitmapCache.trim(trimType)
    nonBitmapCache.trim(trimType)
  }

  override val debugData: String?
    get() =
        "RoutingBitmapMemoryCache{bitmap=${bitmapCache.debugData}, " +
            "nonBitmap=${nonBitmapCache.debugData}}"
}
