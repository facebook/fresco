/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.os.Build;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.MemoryTrimType;

/**
 * CountingMemoryCache eviction strategy appropriate for bitmap caches.
 *
 * <p>If run on KitKat or below, then this TrimStrategy behaves exactly as
 * NativeMemoryCacheTrimStrategy. If run on Lollipop, then BitmapMemoryCacheTrimStrategy will trim
 * cache in one additional case: when OnCloseToDalvikHeapLimit trim type is received, cache's
 * eviction queue will be trimmed according to OnCloseToDalvikHeapLimit's suggested trim ratio.
 */
public class BitmapMemoryCacheTrimStrategy implements MemoryCache.CacheTrimStrategy {
  private static final String TAG = "BitmapMemoryCacheTrimStrategy";

  @Override
  public double getTrimRatio(MemoryTrimType trimType) {
    switch (trimType) {
      case OnCloseToDalvikHeapLimit:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          return MemoryTrimType.OnCloseToDalvikHeapLimit.getSuggestedTrimRatio();
        } else {
          // On pre-lollipop versions we keep bitmaps on the native heap, so no need to trim here
          // as it wouldn't help Dalvik heap anyway.
          return 0;
        }
      case OnAppBackgrounded:
      case OnSystemMemoryCriticallyLowWhileAppInForeground:
      case OnSystemLowMemoryWhileAppInForeground:
      case OnSystemLowMemoryWhileAppInBackground:
        return 1;
      default:
        FLog.wtf(TAG, "unknown trim type: %s", trimType);
        return 0;
    }
  }
}
