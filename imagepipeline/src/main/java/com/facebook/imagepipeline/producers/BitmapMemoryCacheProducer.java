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
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Memory cache producer for the bitmap memory cache.
 */
public class BitmapMemoryCacheProducer implements Producer<CloseableReference<CloseableImage>> {

  @VisibleForTesting static final String PRODUCER_NAME = "BitmapMemoryCacheProducer";
  @VisibleForTesting static final String VALUE_FOUND = "cached_value_found";

  private final MemoryCache<CacheKey, CloseableImage> mMemoryCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<CloseableReference<CloseableImage>> mInputProducer;

  public BitmapMemoryCacheProducer(
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    mMemoryCache = memoryCache;
    mCacheKeyFactory = cacheKeyFactory;
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {

    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    listener.onProducerStart(requestId, getProducerName());
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final CacheKey cacheKey = mCacheKeyFactory.getBitmapCacheKey(imageRequest);

    CloseableReference<CloseableImage> cachedReference = mMemoryCache.get(cacheKey);

    if (cachedReference != null) {
      boolean isFinal = cachedReference.get().getQualityInfo().isOfFullQuality();
      if (isFinal) {
        listener.onProducerFinishWithSuccess(
            requestId,
            getProducerName(),
            listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "true") : null);
        consumer.onProgressUpdate(1f);
      }
      consumer.onNewResult(cachedReference, isFinal);
      cachedReference.close();
      if (isFinal) {
        return;
      }
    }

    if (producerContext.getLowestPermittedRequestLevel().getValue() >=
        ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE.getValue()) {
      listener.onProducerFinishWithSuccess(
          requestId,
          getProducerName(),
          listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "false") : null);
      consumer.onNewResult(null, true);
      return;
    }

    Consumer<CloseableReference<CloseableImage>> wrappedConsumer = wrapConsumer(consumer, cacheKey);
    listener.onProducerFinishWithSuccess(
        requestId,
        getProducerName(),
        listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "false") : null);
    mInputProducer.produceResults(wrappedConsumer, producerContext);
  }

  protected Consumer<CloseableReference<CloseableImage>> wrapConsumer(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final CacheKey cacheKey) {
    return new DelegatingConsumer<
        CloseableReference<CloseableImage>,
        CloseableReference<CloseableImage>>(consumer) {
      @Override
      public void onNewResultImpl(CloseableReference<CloseableImage> newResult, boolean isLast) {
        // ignore invalid intermediate results and forward the null result if last
        if (newResult == null) {
          if (isLast) {
            getConsumer().onNewResult(null, true);
          }
          return;
        }
        // stateful results cannot be cached and are just forwarded
        if (newResult.get().isStateful()) {
          getConsumer().onNewResult(newResult, isLast);
          return;
        }
        // if the intermediate result is not of a better quality than the cached result,
        // forward the already cached result and don't cache the new result.
        if (!isLast) {
          CloseableReference<CloseableImage> currentCachedResult = mMemoryCache.get(cacheKey);
          if (currentCachedResult != null) {
            try {
              QualityInfo newInfo = newResult.get().getQualityInfo();
              QualityInfo cachedInfo = currentCachedResult.get().getQualityInfo();
              if (cachedInfo.isOfFullQuality() || cachedInfo.getQuality() >= newInfo.getQuality()) {
                getConsumer().onNewResult(currentCachedResult, false);
                return;
              }
            } finally {
              CloseableReference.closeSafely(currentCachedResult);
            }
          }
        }
        // cache and forward the new result
        CloseableReference<CloseableImage> newCachedResult =
            mMemoryCache.cache(cacheKey, newResult);
        try {
          if (isLast) {
            getConsumer().onProgressUpdate(1f);
          }
          getConsumer().onNewResult(
              (newCachedResult != null) ? newCachedResult : newResult, isLast);
        } finally {
          CloseableReference.closeSafely(newCachedResult);
        }
      }
    };
  }

  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
