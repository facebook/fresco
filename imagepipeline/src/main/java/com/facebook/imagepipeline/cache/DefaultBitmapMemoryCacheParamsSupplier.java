/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.app.ActivityManager;
import android.os.Build;
import com.facebook.common.internal.Supplier;
import com.facebook.common.util.ByteConstants;
import java.util.concurrent.TimeUnit;

/**
 * Supplies {@link MemoryCacheParams} for the bitmap memory cache.
 */
public class DefaultBitmapMemoryCacheParamsSupplier implements Supplier<MemoryCacheParams> {
  private static final int MAX_CACHE_ENTRIES = 256;
  private static final int MAX_EVICTION_QUEUE_SIZE = Integer.MAX_VALUE;
  private static final int MAX_EVICTION_QUEUE_ENTRIES = Integer.MAX_VALUE;
  private static final int MAX_CACHE_ENTRY_SIZE = Integer.MAX_VALUE;
  private static final long PARAMS_CHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

  private final ActivityManager mActivityManager;

  public DefaultBitmapMemoryCacheParamsSupplier(ActivityManager activityManager) {
    mActivityManager = activityManager;
  }

  @Override
  public MemoryCacheParams get() {
    return new MemoryCacheParams(
        getMaxCacheSize(),
        MAX_CACHE_ENTRIES,
        MAX_EVICTION_QUEUE_SIZE,
        MAX_EVICTION_QUEUE_ENTRIES,
        MAX_CACHE_ENTRY_SIZE,
        PARAMS_CHECK_INTERVAL_MS);
  }

  private int getMaxCacheSize() {
    final int maxMemory =
        Math.min(mActivityManager.getMemoryClass() * ByteConstants.MB, Integer.MAX_VALUE);
    if (maxMemory < 32 * ByteConstants.MB) {
      return 4 * ByteConstants.MB;
    } else if (maxMemory < 64 * ByteConstants.MB) {
      return 6 * ByteConstants.MB;
    } else {
      // We don't want to use more ashmem on Gingerbread for now, since it doesn't respond well to
      // native memory pressure (doesn't throw exceptions, crashes app, crashes phone)
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
        return 8 * ByteConstants.MB;
      } else {
        return maxMemory / 4;
      }
    }
  }
}
