/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import android.os.Build
import com.facebook.common.logging.FLog
import com.facebook.common.memory.MemoryTrimType
import com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy

/**
 * CountingMemoryCache eviction strategy appropriate for bitmap caches.
 *
 * If run on KitKat or below, then this TrimStrategy behaves exactly as
 * NativeMemoryCacheTrimStrategy. If run on Lollipop, then BitmapMemoryCacheTrimStrategy will trim
 * cache in one additional case: when OnCloseToDalvikHeapLimit trim type is received, cache's
 * eviction queue will be trimmed according to OnCloseToDalvikHeapLimit's suggested trim ratio.
 */
class BitmapMemoryCacheTrimStrategy : CacheTrimStrategy {

  override fun getTrimRatio(trimType: MemoryTrimType): Double =
      when (trimType) {
        MemoryTrimType.OnCloseToDalvikHeapLimit ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
              MemoryTrimType.OnCloseToDalvikHeapLimit.suggestedTrimRatio
            } else {
              // On pre-lollipop versions we keep bitmaps on the native heap, so no need to trim
              // here
              // as it wouldn't help Dalvik heap anyway.
              0.0
            }
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
