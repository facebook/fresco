// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.imagepipeline.image.CloseableImage;
import javax.annotation.Nullable;

public class CountingLruBitmapMemoryCacheFactory implements BitmapMemoryCacheFactory {

  @Override
  public CountingMemoryCache<CacheKey, CloseableImage> create(
      Supplier<MemoryCacheParams> bitmapMemoryCacheParamsSupplier,
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      MemoryCache.CacheTrimStrategy trimStrategy,
      @Nullable CountingMemoryCache.EntryStateObserver<CacheKey> observer) {

    ValueDescriptor<CloseableImage> valueDescriptor =
        new ValueDescriptor<CloseableImage>() {
          @Override
          public int getSizeInBytes(CloseableImage value) {
            return value.getSizeInBytes();
          }
        };

    CountingMemoryCache<CacheKey, CloseableImage> countingCache =
        new LruCountingMemoryCache<>(
            valueDescriptor, trimStrategy, bitmapMemoryCacheParamsSupplier, observer);

    memoryTrimmableRegistry.registerMemoryTrimmable(countingCache);

    return countingCache;
  }
}
