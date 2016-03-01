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
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.imagepipeline.request.RepeatedPostprocessor;

/**
 * Memory cache producer for the bitmap memory cache.
 */
public class PostprocessedBitmapMemoryCacheProducer
    implements Producer<CloseableReference<CloseableImage>> {

  @VisibleForTesting static final String PRODUCER_NAME = "PostprocessedBitmapMemoryCacheProducer";
  @VisibleForTesting static final String VALUE_FOUND = "cached_value_found";

  private final MemoryCache<CacheKey, CloseableImage> mMemoryCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<CloseableReference<CloseableImage>> mInputProducer;

  public PostprocessedBitmapMemoryCacheProducer(
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
    final ImageRequest imageRequest = producerContext.getImageRequest();

    // If there's no postprocessor or the postprocessor doesn't require caching, forward results.
    final Postprocessor postprocessor = imageRequest.getPostprocessor();
    if (postprocessor == null || postprocessor.getPostprocessorCacheKey() == null) {
      mInputProducer.produceResults(consumer, producerContext);
      return;
    }
    listener.onProducerStart(requestId, getProducerName());
    final CacheKey cacheKey = mCacheKeyFactory.getPostprocessedBitmapCacheKey(imageRequest);
    CloseableReference<CloseableImage> cachedReference = mMemoryCache.get(cacheKey);
    if (cachedReference != null) {
      listener.onProducerFinishWithSuccess(
          requestId,
          getProducerName(),
          listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "true") : null);
      consumer.onProgressUpdate(1.0f);
      consumer.onNewResult(cachedReference, true);
      cachedReference.close();
    } else {
      final boolean isRepeatedProcessor = postprocessor instanceof RepeatedPostprocessor;
      Consumer<CloseableReference<CloseableImage>> cachedConsumer = new CachedPostprocessorConsumer(
          consumer,
          cacheKey,
          isRepeatedProcessor,
          mMemoryCache);
      listener.onProducerFinishWithSuccess(
          requestId,
          getProducerName(),
          listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "false") : null);
      mInputProducer.produceResults(cachedConsumer, producerContext);
    }
  }

  public static class CachedPostprocessorConsumer extends DelegatingConsumer<
      CloseableReference<CloseableImage>,
      CloseableReference<CloseableImage>> {

    private final CacheKey mCacheKey;
    private final boolean mIsRepeatedProcessor;
    private final MemoryCache<CacheKey, CloseableImage> mMemoryCache;

    public CachedPostprocessorConsumer(final Consumer<CloseableReference<CloseableImage>> consumer,
        final CacheKey cacheKey,
        final boolean isRepeatedProcessor,
        final MemoryCache<CacheKey, CloseableImage> memoryCache) {
      super(consumer);
      this.mCacheKey = cacheKey;
      this.mIsRepeatedProcessor = isRepeatedProcessor;
      this.mMemoryCache = memoryCache;
    }

    @Override
    protected void onNewResultImpl(CloseableReference<CloseableImage> newResult, boolean isLast) {
      // ignore invalid intermediate results and forward the null result if last
      if (newResult == null) {
        if (isLast) {
          getConsumer().onNewResult(null, true);
        }
        return;
      }
      // ignore intermediate results for non-repeated postprocessors
      if (!isLast && !mIsRepeatedProcessor) {
        return;
      }
      // cache and forward the new result
      CloseableReference<CloseableImage> newCachedResult =
          mMemoryCache.cache(mCacheKey, newResult);
      try {
        getConsumer().onProgressUpdate(1f);
        getConsumer().onNewResult(
            (newCachedResult != null) ? newCachedResult : newResult, isLast);
      } finally {
        CloseableReference.closeSafely(newCachedResult);
      }
    }
  }

  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
