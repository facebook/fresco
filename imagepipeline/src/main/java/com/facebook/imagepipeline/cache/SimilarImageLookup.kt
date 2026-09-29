/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

/**
 * Enables producers to find larger cached variants of the same image content to avoid redundant
 * network fetches. Implementations track known cache keys grouped by content identity (groupKey)
 * and find the smallest entry that is still larger than the requested dimensions.
 *
 * All gating logic (e.g. which image types or caller modules are eligible) is the responsibility of
 * the implementation — producers simply call [findLargerMemoryKey]/[findLargerDiskKey] and act on
 * the result.
 */
interface SimilarImageLookup {

  /**
   * Find a larger variant of the same image in the memory cache key tracker.
   *
   * Producer contract: the consuming producer must NOT call this for cache-only requests (e.g.
   * [ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE]). Cache-only callers expect exact-match
   * semantics — returning a resized approximate would violate that contract. The returned result is
   * a raw cache key; the producer is responsible for any resize or re-cache of the larger bitmap.
   *
   * @param cacheKeyString the exact cache key string of the requested image
   * @param groupKey content identity key grouping different resolutions of the same image. This is
   *   typically the base cache key string without dimensions (e.g., normalized URI or
   *   server-provided content ID). All cached variants of the same media share the same groupKey.
   *   Pass null if grouping is not available — implementations should return null in that case.
   * @param width requested image width
   * @param height requested image height
   * @param callerContext app-specific caller context (e.g., IG passes IgCallerContext).
   *   Implementations downcast to their app's type and return null for unrecognized types.
   * @return the cache key of a larger variant, or null if none found or lookup is disabled
   */
  fun findLargerMemoryCacheKey(
      cacheKeyString: String,
      groupKey: String?,
      width: Int,
      height: Int,
      callerContext: Any?,
  ): SimilarImageResult?

  /**
   * Track a memory cache key for future similarity lookups. Called on cache insertion.
   *
   * Producers must only call this after a successful decode — not for intermediate or failed
   * results.
   *
   * @param cacheKeyString the exact cache key string of the decoded image
   * @param groupKey content identity key (same as [findLargerMemoryCacheKey]). Pass null if not
   *   available.
   * @param width decoded image width
   * @param height decoded image height
   */
  fun trackMemoryCacheKey(cacheKeyString: String, groupKey: String?, width: Int, height: Int)

  /** Remove a tracked memory key. Called on cache eviction. */
  fun removeMemoryCacheKey(cacheKeyString: String, groupKey: String?, width: Int, height: Int)
}

/** Result of a similar-image lookup — the cache key of a larger cached variant. */
data class SimilarImageResult(
    val cacheKeyString: String,
    val groupKey: String?,
    val width: Int,
    val height: Int,
)
