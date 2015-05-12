/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.common.references.CloseableReference;

/**
 * Bitmap memory cache producer that is read-only.
 */
public class BitmapMemoryCacheGetProducer extends BitmapMemoryCacheProducer {

  @VisibleForTesting static final String PRODUCER_NAME = "BitmapMemoryCacheGetProducer";

  public BitmapMemoryCacheGetProducer(
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<CloseableReference<CloseableImage>> nextProducer) {
    super(memoryCache, cacheKeyFactory, nextProducer);
  }

  @Override
  protected Consumer<CloseableReference<CloseableImage>> wrapConsumer(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final CacheKey cacheKey) {
    // since this cache is read-only, we can pass our consumer directly to the next producer
    return consumer;
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
