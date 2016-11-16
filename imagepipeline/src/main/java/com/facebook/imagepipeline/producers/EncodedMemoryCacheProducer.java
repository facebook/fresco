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
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.cache.common.CacheKey;

/**
 * Memory cache producer for the encoded memory cache.
 */
public class EncodedMemoryCacheProducer implements Producer<EncodedImage> {

  public static final String PRODUCER_NAME = "EncodedMemoryCacheProducer";
  public static final String EXTRA_CACHED_VALUE_FOUND = ProducerConstants.EXTRA_CACHED_VALUE_FOUND;

  private final MemoryCache<CacheKey, PooledByteBuffer> mMemoryCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<EncodedImage> mInputProducer;

  public EncodedMemoryCacheProducer(
      MemoryCache<CacheKey, PooledByteBuffer> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<EncodedImage> inputProducer) {
    mMemoryCache = memoryCache;
    mCacheKeyFactory = cacheKeyFactory;
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {

    final String requestId = producerContext.getId();
    final ProducerListener listener = producerContext.getListener();
    listener.onProducerStart(requestId, PRODUCER_NAME);
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final CacheKey cacheKey =
        mCacheKeyFactory.getEncodedCacheKey(imageRequest, producerContext.getCallerContext());

    CloseableReference<PooledByteBuffer> cachedReference = mMemoryCache.get(cacheKey);
    try {
      if (cachedReference != null) {
        EncodedImage cachedEncodedImage = new EncodedImage(cachedReference);
        cachedEncodedImage.setEncodedCacheKey(cacheKey);
        try {
          listener.onProducerFinishWithSuccess(
              requestId,
              PRODUCER_NAME,
              listener.requiresExtraMap(requestId)
                  ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "true")
                  : null);
          consumer.onProgressUpdate(1f);
          consumer.onNewResult(cachedEncodedImage, true);
          return;
        } finally {
          EncodedImage.closeSafely(cachedEncodedImage);
        }
      }

      if (producerContext.getLowestPermittedRequestLevel().getValue() >=
          ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE.getValue()) {
        listener.onProducerFinishWithSuccess(
            requestId,
            PRODUCER_NAME,
            listener.requiresExtraMap(requestId)
                ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "false")
                : null);
        consumer.onNewResult(null, true);
        return;
      }

      Consumer consumerOfInputProducer =
          new EncodedMemoryCacheConsumer(consumer, mMemoryCache, cacheKey);

      listener.onProducerFinishWithSuccess(
          requestId,
          PRODUCER_NAME,
          listener.requiresExtraMap(requestId)
              ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "false")
              : null);
      mInputProducer.produceResults(consumerOfInputProducer, producerContext);
    } finally {
      CloseableReference.closeSafely(cachedReference);
    }
  }

  private static class EncodedMemoryCacheConsumer
      extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final MemoryCache<CacheKey, PooledByteBuffer> mMemoryCache;
    private final CacheKey mRequestedCacheKey;

    public EncodedMemoryCacheConsumer(
        Consumer<EncodedImage> consumer,
        MemoryCache<CacheKey, PooledByteBuffer> memoryCache, CacheKey requestedCacheKey) {
      super(consumer);
      mMemoryCache = memoryCache;
      mRequestedCacheKey = requestedCacheKey;
    }

    @Override
    public void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      // intermediate or null results are not cached, so we just forward them
      if (!isLast || newResult == null) {
        getConsumer().onNewResult(newResult, isLast);
        return;
      }
      // cache and forward the last result
      CloseableReference<PooledByteBuffer> ref = newResult.getByteBufferRef();
      if (ref != null) {
        CloseableReference<PooledByteBuffer> cachedResult;
        try {
          final CacheKey cacheKey = newResult.getEncodedCacheKey() != null ?
              newResult.getEncodedCacheKey() : mRequestedCacheKey;
          cachedResult = mMemoryCache.cache(cacheKey, ref);
        } finally {
          CloseableReference.closeSafely(ref);
        }
        if (cachedResult != null) {
          EncodedImage cachedEncodedImage;
          try {
            cachedEncodedImage = new EncodedImage(cachedResult);
            cachedEncodedImage.copyMetaDataFrom(newResult);
          } finally {
            CloseableReference.closeSafely(cachedResult);
          }
          try {
            getConsumer().onProgressUpdate(1f);
            getConsumer().onNewResult(cachedEncodedImage, true);
            return;
          } finally {
            EncodedImage.closeSafely(cachedEncodedImage);
          }
        }
      }
      getConsumer().onNewResult(newResult, true);
    }
  }
}
