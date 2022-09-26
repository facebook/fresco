/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

interface MemoryCacheTracker<K> {
  fun onCacheHit(cacheKey: K)

  fun onCacheMiss(cacheKey: K)

  fun onCachePut(cacheKey: K)
}
