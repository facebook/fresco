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

    CountingMemoryCache.CacheTrimStrategy trimStrategy = new NativeMemoryCacheTrimStrategy();

    CountingMemoryCache<CacheKey, PooledByteBuffer> countingCache =
        new CountingMemoryCache<>(valueDescriptor, trimStrategy, encodedMemoryCacheParamsSupplier);

    memoryTrimmableRegistry.registerMemoryTrimmable(countingCache);

    return countingCache;
  }
}
