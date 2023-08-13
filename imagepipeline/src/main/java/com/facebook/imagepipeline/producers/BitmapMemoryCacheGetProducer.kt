/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.cache.common.CacheKey
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.cache.CacheKeyFactory
import com.facebook.imagepipeline.cache.MemoryCache
import com.facebook.imagepipeline.image.CloseableImage

/** Bitmap memory cache producer that is read-only. */
class BitmapMemoryCacheGetProducer(
    memoryCache: MemoryCache<CacheKey, CloseableImage>,
    cacheKeyFactory: CacheKeyFactory,
    inputProducer: Producer<CloseableReference<CloseableImage>>
) : BitmapMemoryCacheProducer(memoryCache, cacheKeyFactory, inputProducer) {

  override fun wrapConsumer(
      consumer: Consumer<CloseableReference<CloseableImage>>,
      cacheKey: CacheKey,
      isMemoryCacheEnabled: Boolean
  ): Consumer<CloseableReference<CloseableImage>> =
      // since this cache is read-only, we can pass our consumer directly to the next producer
      consumer

  override fun getProducerName(): String = PRODUCER_NAME

  override fun getOriginSubcategory(): String = ORIGIN_SUBCATEGORY

  companion object {
    const val PRODUCER_NAME = "BitmapMemoryCacheGetProducer"
    private const val ORIGIN_SUBCATEGORY = "pipe_ui"
  }
}
