/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.common.internal.Supplier;
import com.facebook.common.util.ByteConstants;

/**
 * Supplies {@link MemoryCacheParams} for the encoded image memory cache
 */
public class DefaultEncodedMemoryCacheParamsSupplier implements Supplier<MemoryCacheParams> {

  // We want memory cache to be bound only by its memory consumption
  private static final int MAX_CACHE_ENTRIES = Integer.MAX_VALUE;
  private static final int MAX_EVICTION_QUEUE_ENTRIES = MAX_CACHE_ENTRIES;

  @Override
  public MemoryCacheParams get() {
    final int maxCacheSize = getMaxCacheSize();
    final int maxCacheEntrySize = maxCacheSize / 8;
    return new MemoryCacheParams(
        maxCacheSize,
        MAX_CACHE_ENTRIES,
        maxCacheSize,
        MAX_EVICTION_QUEUE_ENTRIES,
        maxCacheEntrySize);
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
