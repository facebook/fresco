/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;

/** Memory cache producer for the encoded memory cache. */
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
      final Consumer<EncodedImage> consumer, final ProducerContext producerContext) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("EncodedMemoryCacheProducer#produceResults");
      }
      final ProducerListener2 listener = producerContext.getProducerListener();
      listener.onProducerStart(producerContext, PRODUCER_NAME);
      final ImageRequest imageRequest = producerContext.getImageRequest();
      final CacheKey cacheKey =
          mCacheKeyFactory.getEncodedCacheKey(imageRequest, producerContext.getCallerContext());

      CloseableReference<PooledByteBuffer> cachedReference = mMemoryCache.get(cacheKey);
      try {
        if (cachedReference != null) {
          EncodedImage cachedEncodedImage = new EncodedImage(cachedReference);
          try {
            listener.onProducerFinishWithSuccess(
                producerContext,
                PRODUCER_NAME,
                listener.requiresExtraMap(producerContext, PRODUCER_NAME)
                    ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "true")
                    : null);
            listener.onUltimateProducerReached(producerContext, PRODUCER_NAME, true);
            producerContext.putOriginExtra("memory_encoded");
            consumer.onProgressUpdate(1f);
            consumer.onNewResult(cachedEncodedImage, Consumer.IS_LAST);
            return;
          } finally {
            EncodedImage.closeSafely(cachedEncodedImage);
          }
        }

        if (producerContext.getLowestPermittedRequestLevel().getValue()
            >= ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE.getValue()) {
          listener.onProducerFinishWithSuccess(
              producerContext,
              PRODUCER_NAME,
              listener.requiresExtraMap(producerContext, PRODUCER_NAME)
                  ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "false")
                  : null);
          listener.onUltimateProducerReached(producerContext, PRODUCER_NAME, false);
          producerContext.putOriginExtra("memory_encoded", "nil-result");
          consumer.onNewResult(null, Consumer.IS_LAST);
          return;
        }

        final boolean isMemoryCacheEnabled =
            producerContext.getImageRequest().isMemoryCacheEnabled();
        Consumer consumerOfInputProducer =
            new EncodedMemoryCacheConsumer(
                consumer,
                mMemoryCache,
                cacheKey,
                isMemoryCacheEnabled,
                producerContext.getImagePipelineConfig().getExperiments().isEncodedCacheEnabled());

        listener.onProducerFinishWithSuccess(
            producerContext,
            PRODUCER_NAME,
            listener.requiresExtraMap(producerContext, PRODUCER_NAME)
                ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "false")
                : null);
        mInputProducer.produceResults(consumerOfInputProducer, producerContext);
      } finally {
        CloseableReference.closeSafely(cachedReference);
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  private static class EncodedMemoryCacheConsumer
      extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final MemoryCache<CacheKey, PooledByteBuffer> mMemoryCache;
    private final CacheKey mRequestedCacheKey;
    private final boolean mIsMemoryCacheEnabled;
    private final boolean mEncodedCacheEnabled;

    public EncodedMemoryCacheConsumer(
        Consumer<EncodedImage> consumer,
        MemoryCache<CacheKey, PooledByteBuffer> memoryCache,
        CacheKey requestedCacheKey,
        boolean isMemoryCacheEnabled,
        boolean encodedCacheEnabled) {
      super(consumer);
      mMemoryCache = memoryCache;
      mRequestedCacheKey = requestedCacheKey;
      mIsMemoryCacheEnabled = isMemoryCacheEnabled;
      mEncodedCacheEnabled = encodedCacheEnabled;
    }

    @Override
    public void onNewResultImpl(EncodedImage newResult, @Status int status) {
      try {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection("EncodedMemoryCacheProducer#onNewResultImpl");
        }
        // intermediate, null or uncacheable results are not cached, so we just forward them
        // as well as the images with unknown format which could be html response from the server
        if (isNotLast(status)
            || newResult == null
            || statusHasAnyFlag(status, DO_NOT_CACHE_ENCODED | IS_PARTIAL_RESULT)
            || newResult.getImageFormat() == ImageFormat.UNKNOWN) {
          getConsumer().onNewResult(newResult, status);
          return;
        }

        // cache and forward the last result
        CloseableReference<PooledByteBuffer> ref = newResult.getByteBufferRef();
        if (ref != null) {
          CloseableReference<PooledByteBuffer> cachedResult = null;
          try {
            if (mEncodedCacheEnabled && mIsMemoryCacheEnabled) {
              cachedResult = mMemoryCache.cache(mRequestedCacheKey, ref);
            }
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
              getConsumer().onNewResult(cachedEncodedImage, status);
              return;
            } finally {
              EncodedImage.closeSafely(cachedEncodedImage);
            }
          }
        }
        getConsumer().onNewResult(newResult, status);
      } finally {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
      }
    }
  }
}
