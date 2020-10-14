/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.common.internal.Supplier;
import com.facebook.common.util.ByteConstants;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.TimeUnit;

/** Supplies {@link MemoryCacheParams} for the encoded image memory cache */
@Nullsafe(Nullsafe.Mode.STRICT)
public class DefaultEncodedMemoryCacheParamsSupplier implements Supplier<MemoryCacheParams> {

  // We want memory cache to be bound only by its memory consumption
  private static final int MAX_CACHE_ENTRIES = Integer.MAX_VALUE;
  private static final int MAX_EVICTION_QUEUE_ENTRIES = MAX_CACHE_ENTRIES;
  private static final long PARAMS_CHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

  @Override
  public MemoryCacheParams get() {
    final int maxCacheSize = getMaxCacheSize();
    final int maxCacheEntrySize = maxCacheSize / 8;
    return new MemoryCacheParams(
        maxCacheSize,
        MAX_CACHE_ENTRIES,
        maxCacheSize,
        MAX_EVICTION_QUEUE_ENTRIES,
        maxCacheEntrySize,
        PARAMS_CHECK_INTERVAL_MS);
  }

  private int getMaxCacheSize() {
    final int maxMemory = (int) Math.min(Runtime.getRuntime().maxMemory(), Integer.MAX_VALUE);
    if (maxMemory < 16 * ByteConstants.MB) {
      return 1 * ByteConstants.MB;
    } else if (maxMemory < 32 * ByteConstants.MB) {
      return 2 * ByteConstants.MB;
    } else {
      return 4 * ByteConstants.MB;
    }
  }
}
