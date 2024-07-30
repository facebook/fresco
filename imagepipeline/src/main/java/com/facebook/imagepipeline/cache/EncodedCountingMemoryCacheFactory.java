/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class EncodedCountingMemoryCacheFactory {

  public static CountingMemoryCache<CacheKey, PooledByteBuffer> get(
      Supplier<MemoryCacheParams> encodedMemoryCacheParamsSupplier,
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      MemoryCache.CacheTrimStrategy cacheTrimStrategy) {

    ValueDescriptor<PooledByteBuffer> valueDescriptor =
        new ValueDescriptor<PooledByteBuffer>() {
          @Override
          public int getSizeInBytes(PooledByteBuffer value) {
            return value.size();
          }
        };

    CountingMemoryCache<CacheKey, PooledByteBuffer> countingCache =
        new LruCountingMemoryCache<>(
            valueDescriptor,
            cacheTrimStrategy,
            encodedMemoryCacheParamsSupplier,
            null,
            false,
            false);

    memoryTrimmableRegistry.registerMemoryTrimmable(countingCache);

    return countingCache;
  }
}
