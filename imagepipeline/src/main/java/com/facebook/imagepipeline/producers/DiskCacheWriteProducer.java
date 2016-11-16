/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.cache.DiskCachePolicy;
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
 * <p>Disk cache interactions are delegated to a provided {@link DiskCachePolicy}.
 *
 * <p>This producer is currently used only if the media variations experiment is turned on, to
 * enable another producer to sit between cache read and write.
 */
public class DiskCacheWriteProducer implements Producer<EncodedImage> {
  private final Producer<EncodedImage> mInputProducer;
  private final DiskCachePolicy mDiskCachePolicy;

  public DiskCacheWriteProducer(
      Producer<EncodedImage> inputProducer,
      DiskCachePolicy diskCachePolicy) {
    mInputProducer = inputProducer;
    mDiskCachePolicy = diskCachePolicy;
  }

  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {
    maybeStartInputProducer(consumer, producerContext);
  }

  private void maybeStartInputProducer(
      Consumer<EncodedImage> consumerOfDiskCacheWriteProducer,
      ProducerContext producerContext) {
    if (producerContext.getLowestPermittedRequestLevel().getValue() >=
        ImageRequest.RequestLevel.DISK_CACHE.getValue()) {
      consumerOfDiskCacheWriteProducer.onNewResult(null, true);
    } else {
      Consumer<EncodedImage> consumer;
      if (producerContext.getImageRequest().isDiskCacheEnabled()) {
        consumer = new DiskCacheWriteConsumer(
            consumerOfDiskCacheWriteProducer,
            producerContext,
            mDiskCachePolicy);
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
    private final DiskCachePolicy mDiskCachePolicy;

    private DiskCacheWriteConsumer(
        final Consumer<EncodedImage> consumer,
        final ProducerContext producerContext,
        final DiskCachePolicy diskCachePolicy) {
      super(consumer);
      mProducerContext = producerContext;
      mDiskCachePolicy = diskCachePolicy;
    }

    @Override
    public void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      if (newResult != null && isLast) {
        mDiskCachePolicy.writeToCache(
            newResult,
            mProducerContext.getImageRequest(),
            mProducerContext.getCallerContext());
      }

      getConsumer().onNewResult(newResult, isLast);
    }
  }
}
