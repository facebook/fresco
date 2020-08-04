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
import com.facebook.common.memory.PooledByteBuffer;

public class EncodedCountingMemoryCacheFactory {

  public static CountingMemoryCache<CacheKey, PooledByteBuffer> get(
      Supplier<MemoryCacheParams> encodedMemoryCacheParamsSupplier,
      MemoryTrimmableRegistry memoryTrimmableRegistry) {

    ValueDescriptor<PooledByteBuffer> valueDescriptor =
        new ValueDescriptor<PooledByteBuffer>() {
          @Override
          public int getSizeInBytes(PooledByteBuffer value) {
            return value.size();
          }
        };

    MemoryCache.CacheTrimStrategy trimStrategy = new NativeMemoryCacheTrimStrategy();

    CountingMemoryCache<CacheKey, PooledByteBuffer> countingCache =
        new LruCountingMemoryCache<>(
            valueDescriptor, trimStrategy, encodedMemoryCacheParamsSupplier, null);

    memoryTrimmableRegistry.registerMemoryTrimmable(countingCache);

    return countingCache;
  }
}
