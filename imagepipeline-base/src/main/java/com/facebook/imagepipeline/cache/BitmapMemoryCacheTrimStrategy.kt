/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.common.logging.FLog
import com.facebook.common.memory.MemoryTrimType
import com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy

/**
 * CountingMemoryCache eviction strategy appropriate for bitmap caches.
 *
 * BitmapMemoryCacheTrimStrategy will trim cache when OnCloseToDalvikHeapLimit trim type is
 * received, with the cache's eviction queue trimmed according to OnCloseToDalvikHeapLimit's
 * suggested trim ratio.
 */
class BitmapMemoryCacheTrimStrategy : CacheTrimStrategy {

  override fun getTrimRatio(trimType: MemoryTrimType): Double =
      when (trimType) {
        MemoryTrimType.OnCloseToDalvikHeapLimit ->
            MemoryTrimType.OnCloseToDalvikHeapLimit.suggestedTrimRatio
        MemoryTrimType.OnAppBackgrounded,
        MemoryTrimType.OnSystemMemoryCriticallyLowWhileAppInForeground,
        MemoryTrimType.OnSystemLowMemoryWhileAppInForeground,
        MemoryTrimType.OnSystemLowMemoryWhileAppInBackgroundLowSeverity -> 1.0
        else -> {
          FLog.wtf(TAG, "unknown trim type: %s", trimType)
          0.0
        }
      }

  companion object {
    private const val TAG = "BitmapMemoryCacheTrimStrategy"
  }
}
