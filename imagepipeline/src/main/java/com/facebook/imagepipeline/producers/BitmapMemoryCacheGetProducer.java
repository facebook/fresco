/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;

/**
 * Bitmap memory cache producer that is read-only.
 */
public class BitmapMemoryCacheGetProducer extends BitmapMemoryCacheProducer {

  public static final String PRODUCER_NAME = "BitmapMemoryCacheGetProducer";

  public BitmapMemoryCacheGetProducer(
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    super(memoryCache, cacheKeyFactory, inputProducer);
  }

  @Override
  protected Consumer<CloseableReference<CloseableImage>> wrapConsumer(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final CacheKey cacheKey,
      boolean isMemoryCacheEnabled) {
    // since this cache is read-only, we can pass our consumer directly to the next producer
    return consumer;
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
