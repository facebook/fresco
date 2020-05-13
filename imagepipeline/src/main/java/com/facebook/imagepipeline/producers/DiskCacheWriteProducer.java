/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Disk cache write producer.
 *
 * <p>This producer passes through to the next producer in the sequence, as long as the permitted
 * request level reaches beyond the disk cache. Otherwise this is a passive producer.
 *
 * <p>The final result passed to the consumer put into the disk cache as well as being passed on.
 *
 * <p>This implementation delegates disk cache requests to BufferedDiskCache.
 *
 * <p>This producer is currently used only if the media variations experiment is turned on, to
 * enable another producer to sit between cache read and write.
 */
public class DiskCacheWriteProducer implements Producer<EncodedImage> {
  @VisibleForTesting static final String PRODUCER_NAME = "DiskCacheWriteProducer";

  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<EncodedImage> mInputProducer;

  public DiskCacheWriteProducer(
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<EncodedImage> inputProducer) {
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mInputProducer = inputProducer;
  }

  public void produceResults(
      final Consumer<EncodedImage> consumer, final ProducerContext producerContext) {
    maybeStartInputProducer(consumer, producerContext);
  }

  private void maybeStartInputProducer(
      Consumer<EncodedImage> consumerOfDiskCacheWriteProducer, ProducerContext producerContext) {
    if (producerContext.getLowestPermittedRequestLevel().getValue()
        >= ImageRequest.RequestLevel.DISK_CACHE.getValue()) {
      producerContext.putOriginExtra("disk", "nil-result_write");
      consumerOfDiskCacheWriteProducer.onNewResult(null, Consumer.IS_LAST);
    } else {
      Consumer<EncodedImage> consumer;
      if (producerContext.getImageRequest().isDiskCacheEnabled()) {
        consumer =
            new DiskCacheWriteConsumer(
                consumerOfDiskCacheWriteProducer,
                producerContext,
                mDefaultBufferedDiskCache,
                mSmallImageBufferedDiskCache,
                mCacheKeyFactory);
      } else {
        consumer = consumerOfDiskCacheWriteProducer;
      }

      mInputProducer.produceResults(consumer, producerContext);
    }
  }

  /**
   * Consumer that consumes results from next producer in the sequence.
   *
   * <p>The consumer puts the last result received into disk cache, and passes all results (success
   * or failure) down to the next consumer.
   */
  private static class DiskCacheWriteConsumer
      extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final ProducerContext mProducerContext;
    private final BufferedDiskCache mDefaultBufferedDiskCache;
    private final BufferedDiskCache mSmallImageBufferedDiskCache;
    private final CacheKeyFactory mCacheKeyFactory;

    private DiskCacheWriteConsumer(
        final Consumer<EncodedImage> consumer,
        final ProducerContext producerContext,
        final BufferedDiskCache defaultBufferedDiskCache,
        final BufferedDiskCache smallImageBufferedDiskCache,
        final CacheKeyFactory cacheKeyFactory) {
      super(consumer);
      mProducerContext = producerContext;
      mDefaultBufferedDiskCache = defaultBufferedDiskCache;
      mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
      mCacheKeyFactory = cacheKeyFactory;
    }

    @Override
    public void onNewResultImpl(EncodedImage newResult, @Status int status) {
      mProducerContext.getProducerListener().onProducerStart(mProducerContext, PRODUCER_NAME);
      // intermediate, null or uncacheable results are not cached, so we just forward them
      // as well as the images with unknown format which could be html response from the server
      if (isNotLast(status)
          || newResult == null
          || statusHasAnyFlag(status, DO_NOT_CACHE_ENCODED | IS_PARTIAL_RESULT)
          || newResult.getImageFormat() == ImageFormat.UNKNOWN) {
        mProducerContext
            .getProducerListener()
            .onProducerFinishWithSuccess(mProducerContext, PRODUCER_NAME, null);
        getConsumer().onNewResult(newResult, status);
        return;
      }

      final ImageRequest imageRequest = mProducerContext.getImageRequest();
      final CacheKey cacheKey =
          mCacheKeyFactory.getEncodedCacheKey(imageRequest, mProducerContext.getCallerContext());

      if (imageRequest.getCacheChoice() == ImageRequest.CacheChoice.SMALL) {
        mSmallImageBufferedDiskCache.put(cacheKey, newResult);
      } else {
        mDefaultBufferedDiskCache.put(cacheKey, newResult);
      }
      mProducerContext
          .getProducerListener()
          .onProducerFinishWithSuccess(mProducerContext, PRODUCER_NAME, null);

      getConsumer().onNewResult(newResult, status);
    }
  }
}
