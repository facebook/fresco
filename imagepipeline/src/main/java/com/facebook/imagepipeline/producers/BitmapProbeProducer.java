/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.BoundedLinkedHashSet;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;

/**
 * Probe producer for brobing encoded memory and disk caches on bitmap memory cache hit requests.
 */
public class BitmapProbeProducer implements Producer<CloseableReference<CloseableImage>> {

  public static final String PRODUCER_NAME = "BitmapProbeProducer";

  private final MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<CloseableReference<CloseableImage>> mInputProducer;
  private final BoundedLinkedHashSet<CacheKey> mEncodedMemoryCacheHistory;
  private final BoundedLinkedHashSet<CacheKey> mDiskCacheHistory;

  public BitmapProbeProducer(
      MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache,
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      BoundedLinkedHashSet<CacheKey> encodedMemoryCacheHistory,
      BoundedLinkedHashSet<CacheKey> diskCacheHistory,
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    mEncodedMemoryCache = encodedMemoryCache;
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mEncodedMemoryCacheHistory = encodedMemoryCacheHistory;
    mDiskCacheHistory = diskCacheHistory;
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("BitmapProbeProducer#produceResults");
      }
      final ProducerListener2 listener = producerContext.getProducerListener();
      listener.onProducerStart(producerContext, getProducerName());
      Consumer<CloseableReference<CloseableImage>> consumerOfInputProducer =
          new ProbeConsumer(
              consumer,
              producerContext,
              mEncodedMemoryCache,
              mDefaultBufferedDiskCache,
              mSmallImageBufferedDiskCache,
              mCacheKeyFactory,
              mEncodedMemoryCacheHistory,
              mDiskCacheHistory);

      listener.onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, null);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("mInputProducer.produceResult");
      }
      mInputProducer.produceResults(consumerOfInputProducer, producerContext);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  private static class ProbeConsumer
      extends DelegatingConsumer<
          CloseableReference<CloseableImage>, CloseableReference<CloseableImage>> {

    private final ProducerContext mProducerContext;
    private final MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
    private final BufferedDiskCache mDefaultBufferedDiskCache;
    private final BufferedDiskCache mSmallImageBufferedDiskCache;
    private final CacheKeyFactory mCacheKeyFactory;
    private final BoundedLinkedHashSet<CacheKey> mEncodedMemoryCacheHistory;
    private final BoundedLinkedHashSet<CacheKey> mDiskCacheHistory;

    public ProbeConsumer(
        Consumer<CloseableReference<CloseableImage>> consumer,
        ProducerContext producerContext,
        MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache,
        BufferedDiskCache defaultBufferedDiskCache,
        BufferedDiskCache smallImageBufferedDiskCache,
        CacheKeyFactory cacheKeyFactory,
        BoundedLinkedHashSet<CacheKey> encodedMemoryCacheHistory,
        BoundedLinkedHashSet<CacheKey> diskCacheHistory) {
      super(consumer);
      mProducerContext = producerContext;
      mEncodedMemoryCache = encodedMemoryCache;
      mDefaultBufferedDiskCache = defaultBufferedDiskCache;
      mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
      mCacheKeyFactory = cacheKeyFactory;
      mEncodedMemoryCacheHistory = encodedMemoryCacheHistory;
      mDiskCacheHistory = diskCacheHistory;
    }

    @Override
    public void onNewResultImpl(CloseableReference<CloseableImage> newResult, @Status int status) {
      try {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection("BitmapProbeProducer#onNewResultImpl");
        }

        // intermediate or null are not cached, so we just forward them
        if (isNotLast(status) || newResult == null || statusHasAnyFlag(status, IS_PARTIAL_RESULT)) {
          getConsumer().onNewResult(newResult, status);
          return;
        }

        final ImageRequest imageRequest = mProducerContext.getImageRequest();
        final CacheKey cacheKey =
            mCacheKeyFactory.getEncodedCacheKey(imageRequest, mProducerContext.getCallerContext());
        if (mProducerContext.getExtra(ProducerContext.ExtraKeys.ORIGIN).equals("memory_bitmap")) {
          if (mProducerContext
                  .getImagePipelineConfig()
                  .getExperiments()
                  .isEncodedMemoryCacheProbingEnabled()
              && !mEncodedMemoryCacheHistory.contains(cacheKey)) {
            mEncodedMemoryCache.probe(cacheKey);
            mEncodedMemoryCacheHistory.add(cacheKey);
          }
          if (mProducerContext.getImagePipelineConfig().getExperiments().isDiskCacheProbingEnabled()
              && !mDiskCacheHistory.contains(cacheKey)) {
            final boolean isSmallRequest =
                (imageRequest.getCacheChoice() == ImageRequest.CacheChoice.SMALL);
            final BufferedDiskCache preferredCache =
                isSmallRequest ? mSmallImageBufferedDiskCache : mDefaultBufferedDiskCache;
            preferredCache.addKeyForAsyncProbing(cacheKey);
            mDiskCacheHistory.add(cacheKey);
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

  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
