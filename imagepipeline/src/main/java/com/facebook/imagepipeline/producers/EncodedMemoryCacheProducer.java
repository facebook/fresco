/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.ImmutableMap;
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
public class EncodedMemoryCacheProducer implements Producer<CloseableReference<PooledByteBuffer>> {
  @VisibleForTesting static final String PRODUCER_NAME = "EncodedMemoryCacheProducer";
  @VisibleForTesting static final String VALUE_FOUND = "cached_value_found";

  private final MemoryCache<CacheKey, PooledByteBuffer> mMemoryCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<CloseableReference<PooledByteBuffer>> mNextProducer;

  public EncodedMemoryCacheProducer(
      MemoryCache<CacheKey, PooledByteBuffer> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    mMemoryCache = memoryCache;
    mCacheKeyFactory = cacheKeyFactory;
    mNextProducer = nextProducer;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<PooledByteBuffer>> consumer,
      final ProducerContext producerContext) {

    final String requestId = producerContext.getId();
    final ProducerListener listener = producerContext.getListener();
    listener.onProducerStart(requestId, PRODUCER_NAME);
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest);

    CloseableReference<PooledByteBuffer> cachedReference = mMemoryCache.get(cacheKey);

    if (cachedReference != null) {
      listener.onProducerFinishWithSuccess(
          requestId,
          PRODUCER_NAME,
          listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "true") : null);
      consumer.onProgressUpdate(1f);
      consumer.onNewResult(cachedReference, true);
      cachedReference.close();
      return;
    }

    if (producerContext.getLowestPermittedRequestLevel().getValue() >=
        ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE.getValue()) {
      listener.onProducerFinishWithSuccess(
          requestId,
          PRODUCER_NAME,
          listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "false") : null);
      consumer.onNewResult(null, true);
      return;
    }

    Consumer<CloseableReference<PooledByteBuffer>> consumerOfNextProducer = new DelegatingConsumer<
        CloseableReference<PooledByteBuffer>,
        CloseableReference<PooledByteBuffer>>(consumer) {
      @Override
      public void onNewResultImpl(CloseableReference<PooledByteBuffer> newResult, boolean isLast) {
        // intermediate or null results are not cached, so we just forward them
        if (!isLast || newResult == null) {
          getConsumer().onNewResult(newResult, isLast);
          return;
        }
        // cache and forward the last result
        CloseableReference<PooledByteBuffer> cachedResult = mMemoryCache.cache(cacheKey, newResult);
        try {
          getConsumer().onProgressUpdate(1f);
          getConsumer().onNewResult((cachedResult != null) ? cachedResult : newResult, true);
        } finally {
          CloseableReference.closeSafely(cachedResult);
        }
      }
    };

    listener.onProducerFinishWithSuccess(
        requestId,
        PRODUCER_NAME,
        listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "false") : null);
    mNextProducer.produceResults(consumerOfNextProducer, producerContext);
  }
}
