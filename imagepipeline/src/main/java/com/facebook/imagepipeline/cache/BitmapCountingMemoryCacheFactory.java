/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.imagepipeline.image.CloseableImage;

public class BitmapCountingMemoryCacheFactory {
  public static CountingMemoryCache<CacheKey, CloseableImage> get(
      Supplier<MemoryCacheParams> bitmapMemoryCacheParamsSupplier,
      MemoryTrimmableRegistry memoryTrimmableRegistry) {

    ValueDescriptor<CloseableImage> valueDescriptor =
        new ValueDescriptor<CloseableImage>() {
          @Override
          public int getSizeInBytes(CloseableImage value) {
            return value.getSizeInBytes();
          }
        };

    CountingMemoryCache.CacheTrimStrategy trimStrategy = new BitmapMemoryCacheTrimStrategy();

    CountingMemoryCache<CacheKey, CloseableImage> countingCache =
        new CountingMemoryCache<>(valueDescriptor, trimStrategy, bitmapMemoryCacheParamsSupplier);

     memoryTrimmableRegistry.registerMemoryTrimmable(countingCache);

    return countingCache;
  }
}
