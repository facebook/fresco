/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.common.logging.FLog;
import com.facebook.common.memory.MemoryTrimType;

/**
 * CountingMemoryCache eviction strategy appropriate for caches that store resources off the Dalvik
 * heap.
 *
 * <p>In case of OnCloseToDalvikHeapLimit nothing will be done. In case of other trim types
 * eviction queue of the cache will be cleared.
 */
public class NativeMemoryCacheTrimStrategy implements CountingMemoryCache.CacheTrimStrategy {
  private static final String TAG = "NativeMemoryCacheTrimStrategy";

  public NativeMemoryCacheTrimStrategy() {}

  @Override
  public double getTrimRatio(MemoryTrimType trimType) {
    switch (trimType) {
      case OnCloseToDalvikHeapLimit:
        // Resources cached on native heap do not consume Dalvik heap, so no trimming here.
        return 0;
      case OnAppBackgrounded:
      case OnSystemLowMemoryWhileAppInForeground:
      case OnSystemLowMemoryWhileAppInBackground:
        return 1;
      default:
        FLog.wtf(TAG, "unknown trim type: %s", trimType);
        return 0;
    }
  }
}
