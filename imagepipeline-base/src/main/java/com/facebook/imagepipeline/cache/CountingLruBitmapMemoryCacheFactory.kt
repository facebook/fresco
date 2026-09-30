/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.cache.common.CacheKey
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.imagepipeline.cache.CountingMemoryCache.EntryStateObserver
import com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy
import com.facebook.imagepipeline.image.CloseableImage

class CountingLruBitmapMemoryCacheFactory : BitmapMemoryCacheFactory {

  override fun create(
      bitmapMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>,
      memoryTrimmableRegistry: MemoryTrimmableRegistry,
      trimStrategy: CacheTrimStrategy,
      storeEntrySize: Boolean,
      ignoreSizeMismatch: Boolean,
      observer: EntryStateObserver<CacheKey>?,
  ): CountingMemoryCache<CacheKey, CloseableImage> {
    val valueDescriptor =
        object : ValueDescriptor<CloseableImage> {
          override fun getSizeInBytes(value: CloseableImage): Int = value.sizeInBytes
        }

    val countingCache: CountingMemoryCache<CacheKey, CloseableImage> = LruCountingMemoryCache(
        valueDescriptor,
        trimStrategy,
        bitmapMemoryCacheParamsSupplier,
        observer,
        storeEntrySize,
        ignoreSizeMismatch,
    )

    memoryTrimmableRegistry.registerMemoryTrimmable(countingCache)

    return countingCache
  }
}
