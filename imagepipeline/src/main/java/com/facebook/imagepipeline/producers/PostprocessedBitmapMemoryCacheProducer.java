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
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;

import com.android.internal.util.Predicate;

/**
 * Memory cache producer for the bitmap memory cache.
 */
public class PostprocessedBitmapMemoryCacheProducer
    implements Producer<CloseableReference<CloseableImage>> {

  @VisibleForTesting static final String PRODUCER_NAME = "PostprocessedBitmapMemoryCacheProducer";
  @VisibleForTesting static final String VALUE_FOUND = "cached_value_found";

  private final MemoryCache<CacheKey, CloseableImage> mMemoryCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<CloseableReference<CloseableImage>> mNextProducer;

  public PostprocessedBitmapMemoryCacheProducer(
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<CloseableReference<CloseableImage>> nextProducer) {
    mMemoryCache = memoryCache;
    mCacheKeyFactory = cacheKeyFactory;
    mNextProducer = nextProducer;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {

    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    final ImageRequest imageRequest = producerContext.getImageRequest();

    // No point continuing if there's no postprocessor attached to this request.
    final Postprocessor postprocessor = imageRequest.getPostprocessor();
    if (postprocessor == null) {
      mNextProducer.produceResults(consumer, producerContext);
      return;
    }
    listener.onProducerStart(requestId, getProducerName());

    final CacheKey postprocessorCacheKey = postprocessor.getPostprocessorCacheKey();
    final CacheKey cacheKey;
    CloseableReference<CloseableImage> cachedReference = null;
    final String postprocessorName;
    if (postprocessorCacheKey != null) {
      cacheKey = mCacheKeyFactory.getPostprocessedBitmapCacheKey(imageRequest);
      cachedReference = mMemoryCache.get(cacheKey);
      postprocessorName = postprocessor.getClass().getName();
    } else {
      cacheKey = null;
      postprocessorName = null;
    }

    if (cachedReference != null) {
      listener.onProducerFinishWithSuccess(
          requestId,
          getProducerName(),
          listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "true") : null);
      consumer.onProgressUpdate(1f);
      consumer.onNewResult(cachedReference, true);
      cachedReference.close();
    } else {
      Consumer<CloseableReference<CloseableImage>> cachedConsumer = new CachedPostprocessorConsumer(
          consumer,
          cacheKey,
          postprocessorName,
          mMemoryCache);
      listener.onProducerFinishWithSuccess(
          requestId,
          getProducerName(),
          listener.requiresExtraMap(requestId) ? ImmutableMap.of(VALUE_FOUND, "false") : null);
      mNextProducer.produceResults(cachedConsumer, producerContext);
    }
  }

  public static class CachedPostprocessorConsumer extends DelegatingConsumer<
      CloseableReference<CloseableImage>,
      CloseableReference<CloseableImage>> {

    private final CacheKey mCacheKey;
    private final String mPostprocessorName;
    private final MemoryCache<CacheKey, CloseableImage> mMemoryCache;

    public CachedPostprocessorConsumer(final Consumer<CloseableReference<CloseableImage>> consumer,
        final CacheKey cacheKey,
        final String postprocessorName,
        final MemoryCache<CacheKey, CloseableImage> memoryCache) {
      super(consumer);
      this.mCacheKey = cacheKey;
      this.mPostprocessorName = postprocessorName;
      this.mMemoryCache = memoryCache;
    }

    @Override
    protected void onNewResultImpl(CloseableReference<CloseableImage> newResult, boolean isLast) {
      // Given a null result, we just pass it on.
      if (newResult == null) {
        getConsumer().onNewResult(null, isLast);
        return;
      }

      // cache and forward the new result
      final CloseableReference<CloseableImage> newCachedResult;
      if (mCacheKey != null) {
        if (mPostprocessorName != null) {
          mMemoryCache.removeAll(
              new Predicate<CacheKey>() {
                @Override
                public boolean apply(CacheKey cacheKey) {
                  if (cacheKey instanceof BitmapMemoryCacheKey) {
                    return mPostprocessorName.equals(
                        ((BitmapMemoryCacheKey) cacheKey).getPostprocessorName());
                  }
                  return false;
                }
              });
        }
        newCachedResult = mMemoryCache.cache(mCacheKey, newResult);
      } else {
        newCachedResult = newResult;
      }
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
