/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.cache.common.CacheKey;

import com.facebook.common.internal.VisibleForTesting;

/**
 * Memory cache producer for the encoded memory cache.
 */
public class EncodedMemoryCacheProducer extends MemoryCacheProducer<CacheKey, PooledByteBuffer> {
  @VisibleForTesting static final String PRODUCER_NAME = "EncodedMemoryCacheProducer";

  public EncodedMemoryCacheProducer(
      MemoryCache<CacheKey, PooledByteBuffer> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    super(memoryCache, cacheKeyFactory, nextProducer);
  }

  @Override
  protected CacheKey getCacheKey(ImageRequest imageRequest) {
    return mCacheKeyFactory.getEncodedCacheKey(imageRequest);
  }

  @Override
  protected boolean isResultFinal(
      CloseableReference<PooledByteBuffer> cachedResultFound) {
    return true;
  }

  @Override
  protected ImageRequest.RequestLevel getProducerRequestLevel() {
    return ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE;
  }

  @Override
  protected boolean shouldCacheReturnedValues() {
    return true;
  }

  @Override
  protected boolean shouldCacheResult(
      CloseableReference<PooledByteBuffer> result,
      CacheKey cacheKey,
      boolean isLast) {
    return isLast;
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
