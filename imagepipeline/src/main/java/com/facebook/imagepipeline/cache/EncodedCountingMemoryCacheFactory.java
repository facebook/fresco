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
import com.facebook.imagepipeline.memory.PooledByteBuffer;

public class EncodedCountingMemoryCacheFactory {

  public static CountingMemoryCache<CacheKey, PooledByteBuffer, Void> get(
      Supplier<MemoryCacheParams> encodedMemoryCacheParamsSupplier,
      MemoryTrimmableRegistry memoryTrimmableRegistry) {
    MemoryCacheIndex<CacheKey, PooledByteBuffer, Void> memoryCacheIndex =
        new SimpleMemoryCacheIndex<CacheKey, PooledByteBuffer>();

    CountingMemoryCache.ValueInfoCallback<PooledByteBuffer> valueTypeDescriptor =
        new CountingMemoryCache.ValueInfoCallback<PooledByteBuffer>() {
          @Override
          public long getSizeInBytes(PooledByteBuffer value) {
            return value.size();
          }
        };

    CountingMemoryCache.CacheTrimStrategy trimStrategy = new NativeMemoryCacheTrimStrategy();

    CountingMemoryCache<CacheKey, PooledByteBuffer, Void> countingCache =
        new CountingMemoryCache<CacheKey, PooledByteBuffer, Void>(
            memoryCacheIndex,
            valueTypeDescriptor,
            trimStrategy,
            encodedMemoryCacheParamsSupplier);

    memoryTrimmableRegistry.registerMemoryTrimmable(countingCache);

    return countingCache;
  }
}
