/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;

/**
 * Remove image transform meta data producer
 *
 * <p>Remove the {@link ImageTransformMetaData} object from the results passed down from the next
 * producer, and adds it to the result that it returns to the consumer.
 */
public class RemoveImageTransformMetaDataProducer
    implements Producer<CloseableReference<PooledByteBuffer>> {
  private final Producer<EncodedImage> mNextProducer;

  public RemoveImageTransformMetaDataProducer(
      Producer<EncodedImage> nextProducer) {
    mNextProducer = nextProducer;
  }

  @Override
  public void produceResults(
      Consumer<CloseableReference<PooledByteBuffer>> consumer,
      ProducerContext context) {
    mNextProducer.produceResults(new RemoveImageTransformMetaDataConsumer(consumer), context);
  }

  private class RemoveImageTransformMetaDataConsumer extends DelegatingConsumer<EncodedImage,
          CloseableReference<PooledByteBuffer>> {

    private RemoveImageTransformMetaDataConsumer(
        Consumer<CloseableReference<PooledByteBuffer>> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      CloseableReference<PooledByteBuffer> ret = null;
      try {
        if (EncodedImage.isValid(newResult)) {
          ret = newResult.getByteBufferRef();
        }
        getConsumer().onNewResult(ret, isLast);
      } finally {
        CloseableReference.closeSafely(ret);
      }
    }
  }
}
