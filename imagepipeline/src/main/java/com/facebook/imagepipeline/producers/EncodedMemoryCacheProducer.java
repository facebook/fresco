/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.middleware.HasExtraData;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.cache.BoundedLinkedHashMap;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Memory cache producer for the encoded memory cache. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class EncodedMemoryCacheProducer implements Producer<EncodedImage> {

  public static final String PRODUCER_NAME = "EncodedMemoryCacheProducer";

  public static final String EXTRA_CACHED_VALUE_FOUND = ProducerConstants.EXTRA_CACHED_VALUE_FOUND;

  private final MemoryCache<CacheKey, PooledByteBuffer> mMemoryCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<EncodedImage> mInputProducer;
  private final @Nullable BoundedLinkedHashMap<CacheKey, String> mMetadataCache;

  public EncodedMemoryCacheProducer(
      MemoryCache<CacheKey, PooledByteBuffer> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<EncodedImage> inputProducer) {
    this(memoryCache, cacheKeyFactory, inputProducer, null);
  }

  public EncodedMemoryCacheProducer(
      MemoryCache<CacheKey, PooledByteBuffer> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<EncodedImage> inputProducer,
      @Nullable BoundedLinkedHashMap<CacheKey, String> metadataCache) {
    mMemoryCache = memoryCache;
    mCacheKeyFactory = cacheKeyFactory;
    mInputProducer = inputProducer;
    mMetadataCache = metadataCache;
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
      final boolean isEncodedCacheEnabledForRead =
          producerContext
              .getImageRequest()
              .isCacheEnabled(ImageRequest.CachesLocationsMasks.ENCODED_READ);
      CloseableReference<PooledByteBuffer> cachedReference =
          isEncodedCacheEnabledForRead ? mMemoryCache.get(cacheKey) : null;
      try {
        EncodedImage cachedEncodedImage =
            cachedReference == null ? null : new EncodedImage(cachedReference);
        if (cachedEncodedImage != null
            && !DiskCacheReadProducer.shouldSkipPartialEncodedImage(
                cachedEncodedImage, imageRequest)) {
          try {
            listener.onProducerFinishWithSuccess(
                producerContext,
                PRODUCER_NAME,
                listener.requiresExtraMap(producerContext, PRODUCER_NAME)
                    ? ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, "true")
                    : null);
            listener.onUltimateProducerReached(producerContext, PRODUCER_NAME, true);
            producerContext.putOriginExtra("memory_encoded");
            producerContext.putExtra(HasExtraData.KEY_ENCODED_SIZE, cachedEncodedImage.getSize());
            producerContext.putExtra(HasExtraData.KEY_ENCODED_WIDTH, cachedEncodedImage.getWidth());
            producerContext.putExtra(
                HasExtraData.KEY_ENCODED_HEIGHT, cachedEncodedImage.getHeight());
            maybeRestoreMetadata(cacheKey, cachedEncodedImage, producerContext);
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

        Consumer<EncodedImage> consumerOfInputProducer =
            new EncodedMemoryCacheConsumer(
                consumer,
                mMemoryCache,
                cacheKey,
                producerContext
                    .getImageRequest()
                    .isCacheEnabled(ImageRequest.CachesLocationsMasks.ENCODED_WRITE),
                producerContext.getImagePipelineConfig().getExperiments().isEncodedCacheEnabled(),
                mMetadataCache);

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

  /**
   * The encoded memory cache stores raw bytes, so extras carried by the {@link EncodedImage} that
   * produced them are lost on a hit. The cache key is a normalized URI deliberately shared by many
   * fetch URLs, so the query has to come from the cached entry and must never be recomputed from
   * the current request -- that would report a variation the cached bytes were not fetched with.
   */
  private void maybeRestoreMetadata(
      CacheKey cacheKey, EncodedImage encodedImage, ProducerContext producerContext) {
    if (mMetadataCache == null) {
      return;
    }
    String query = mMetadataCache.get(cacheKey);
    if (query != null) {
      encodedImage.putExtra(HasExtraData.KEY_SF_QUERY, query);
      producerContext.putExtra(HasExtraData.KEY_SF_QUERY, query);
    }
  }

  private static class EncodedMemoryCacheConsumer
      extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final MemoryCache<CacheKey, PooledByteBuffer> mMemoryCache;
    private final CacheKey mRequestedCacheKey;
    private final boolean mIsEncodedCacheEnabledForWrite;
    private final boolean mEncodedCacheEnabled;
    private final @Nullable BoundedLinkedHashMap<CacheKey, String> mMetadataCache;

    public EncodedMemoryCacheConsumer(
        Consumer<EncodedImage> consumer,
        MemoryCache<CacheKey, PooledByteBuffer> memoryCache,
        CacheKey requestedCacheKey,
        boolean isEncodedCacheEnabledForWrite,
        boolean encodedCacheEnabled,
        @Nullable BoundedLinkedHashMap<CacheKey, String> metadataCache) {
      super(consumer);
      mMemoryCache = memoryCache;
      mRequestedCacheKey = requestedCacheKey;
      mIsEncodedCacheEnabledForWrite = isEncodedCacheEnabledForWrite;
      mEncodedCacheEnabled = encodedCacheEnabled;
      mMetadataCache = metadataCache;
    }

    /**
     * Must run for every cache write, including when the image has no query: leaving a previous
     * entry in place would attach a stale query to the bytes that just replaced it.
     */
    private void saveMetadata(EncodedImage image) {
      if (mMetadataCache == null) {
        return;
      }
      String query = image.getExtra(HasExtraData.KEY_SF_QUERY);
      if (query != null) {
        mMetadataCache.put(mRequestedCacheKey, query);
      } else {
        mMetadataCache.remove(mRequestedCacheKey);
      }
    }

    @Override
    public void onNewResultImpl(@Nullable EncodedImage newResult, @Status int status) {
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
            if (mEncodedCacheEnabled && mIsEncodedCacheEnabledForWrite) {
              cachedResult = mMemoryCache.cache(mRequestedCacheKey, ref);
              saveMetadata(newResult);
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
