/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import androidx.annotation.VisibleForTesting;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.core.DiskCachesStore;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

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
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DiskCacheWriteProducer implements Producer<EncodedImage> {
  @VisibleForTesting static final String PRODUCER_NAME = "DiskCacheWriteProducer";

  private final Supplier<DiskCachesStore> mDiskCachesStoreSupplier;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<EncodedImage> mInputProducer;

  public DiskCacheWriteProducer(
      Supplier<DiskCachesStore> diskCachesStoreSupplier,
      CacheKeyFactory cacheKeyFactory,
      Producer<EncodedImage> inputProducer) {
    mDiskCachesStoreSupplier = diskCachesStoreSupplier;
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
      final boolean isDiskCacheEnabledForWrite =
          producerContext
              .getImageRequest()
              .isCacheEnabled(ImageRequest.CachesLocationsMasks.DISK_WRITE);
      if (isDiskCacheEnabledForWrite) {
        consumer =
            new DiskCacheWriteConsumer(
                consumerOfDiskCacheWriteProducer,
                producerContext,
                mDiskCachesStoreSupplier,
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
    private final Supplier<DiskCachesStore> mDiskCachesStoreSupplier;
    private final CacheKeyFactory mCacheKeyFactory;

    private DiskCacheWriteConsumer(
        final Consumer<EncodedImage> consumer,
        final ProducerContext producerContext,
        final Supplier<DiskCachesStore> diskCachesStoreSupplier,
        final CacheKeyFactory cacheKeyFactory) {
      super(consumer);
      mProducerContext = producerContext;
      mDiskCachesStoreSupplier = diskCachesStoreSupplier;
      mCacheKeyFactory = cacheKeyFactory;
    }

    @Override
    public void onNewResultImpl(@Nullable EncodedImage newResult, @Status int status) {
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
      final DiskCachesStore diskCachesStore = mDiskCachesStoreSupplier.get();
      BufferedDiskCache bufferedDiskCache =
          DiskCacheDecision.chooseDiskCacheForRequest(
              imageRequest,
              diskCachesStore.getSmallImageBufferedDiskCache(),
              diskCachesStore.getMainBufferedDiskCache(),
              diskCachesStore.getDynamicBufferedDiskCaches());
      if (bufferedDiskCache == null) {
        mProducerContext
            .getProducerListener()
            .onProducerFinishWithFailure(
                mProducerContext,
                PRODUCER_NAME,
                new DiskCacheDecision.DiskCacheDecisionNoDiskCacheChosenException(
                    "Got no disk cache for CacheChoice: "
                        + Integer.valueOf(imageRequest.getCacheChoice().ordinal()).toString()),
                null);
        getConsumer().onNewResult(newResult, status);
        return;
      }
      bufferedDiskCache.put(cacheKey, newResult);
      mProducerContext
          .getProducerListener()
          .onProducerFinishWithSuccess(mProducerContext, PRODUCER_NAME, null);

      getConsumer().onNewResult(newResult, status);
    }
  }
}
