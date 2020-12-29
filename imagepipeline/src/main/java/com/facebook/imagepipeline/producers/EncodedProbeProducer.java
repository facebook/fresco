/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.cache.BoundedLinkedHashSet;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;

/** Probe producer for brobing disk cache on encoded memory cache hit requests. */
public class EncodedProbeProducer implements Producer<EncodedImage> {

  public static final String PRODUCER_NAME = "EncodedProbeProducer";

  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<EncodedImage> mInputProducer;
  private final BoundedLinkedHashSet<CacheKey> mEncodedMemoryCacheHistory;
  private final BoundedLinkedHashSet<CacheKey> mDiskCacheHistory;

  public EncodedProbeProducer(
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      BoundedLinkedHashSet encodedMemoryCacheHistory,
      BoundedLinkedHashSet diskCacheHistory,
      Producer<EncodedImage> inputProducer) {
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mEncodedMemoryCacheHistory = encodedMemoryCacheHistory;
    mDiskCacheHistory = diskCacheHistory;
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer, final ProducerContext producerContext) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("EncodedProbeProducer#produceResults");
      }
      final ProducerListener2 listener = producerContext.getProducerListener();
      listener.onProducerStart(producerContext, getProducerName());
      Consumer<EncodedImage> consumerOfInputProducer =
          new ProbeConsumer(
              consumer,
              producerContext,
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

  private static class ProbeConsumer extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final ProducerContext mProducerContext;
    private final BufferedDiskCache mDefaultBufferedDiskCache;
    private final BufferedDiskCache mSmallImageBufferedDiskCache;
    private final CacheKeyFactory mCacheKeyFactory;
    private final BoundedLinkedHashSet<CacheKey> mEncodedMemoryCacheHistory;
    private final BoundedLinkedHashSet<CacheKey> mDiskCacheHistory;

    public ProbeConsumer(
        Consumer<EncodedImage> consumer,
        ProducerContext producerContext,
        BufferedDiskCache defaultBufferedDiskCache,
        BufferedDiskCache smallImageBufferedDiskCache,
        CacheKeyFactory cacheKeyFactory,
        BoundedLinkedHashSet<CacheKey> encodedMemoryCacheHistory,
        BoundedLinkedHashSet<CacheKey> diskCacheHistory) {
      super(consumer);
      mProducerContext = producerContext;
      mDefaultBufferedDiskCache = defaultBufferedDiskCache;
      mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
      mCacheKeyFactory = cacheKeyFactory;
      mEncodedMemoryCacheHistory = encodedMemoryCacheHistory;
      mDiskCacheHistory = diskCacheHistory;
    }

    @Override
    public void onNewResultImpl(EncodedImage newResult, @Status int status) {
      try {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection("EncodedProbeProducer#onNewResultImpl");
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

        final ImageRequest imageRequest = mProducerContext.getImageRequest();
        final CacheKey cacheKey =
            mCacheKeyFactory.getEncodedCacheKey(imageRequest, mProducerContext.getCallerContext());

        mEncodedMemoryCacheHistory.add(cacheKey);
        if (mProducerContext.getExtra(ProducerContext.ExtraKeys.ORIGIN).equals("memory_encoded")) {
          if (!mDiskCacheHistory.contains(cacheKey)) {
            final boolean isSmallRequest =
                (imageRequest.getCacheChoice() == ImageRequest.CacheChoice.SMALL);
            final BufferedDiskCache preferredCache =
                isSmallRequest ? mSmallImageBufferedDiskCache : mDefaultBufferedDiskCache;
            preferredCache.addKeyForAsyncProbing(cacheKey);
            mDiskCacheHistory.add(cacheKey);
          }
        } else if (mProducerContext.getExtra(ProducerContext.ExtraKeys.ORIGIN).equals("disk")) {
          // image was fetched from disk cache, therefore it was probed in disk cache by default
          mDiskCacheHistory.add(cacheKey);
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
