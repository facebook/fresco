/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;

/**
 * Remove image transform meta data producer
 *
 * <p>Remove the {@link ImageTransformMetaData} object from the results passed down from the next
 * producer, and adds it to the result that it returns to the consumer.
 */
public class RemoveImageTransformMetaDataProducer
    implements Producer<CloseableReference<PooledByteBuffer>> {
  private final Producer<EncodedImage> mInputProducer;

  public RemoveImageTransformMetaDataProducer(
      Producer<EncodedImage> inputProducer) {
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(
      Consumer<CloseableReference<PooledByteBuffer>> consumer,
      ProducerContext context) {
    mInputProducer.produceResults(new RemoveImageTransformMetaDataConsumer(consumer), context);
  }

  private class RemoveImageTransformMetaDataConsumer extends DelegatingConsumer<EncodedImage,
          CloseableReference<PooledByteBuffer>> {

    private RemoveImageTransformMetaDataConsumer(
        Consumer<CloseableReference<PooledByteBuffer>> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(EncodedImage newResult, @Status int status) {
      CloseableReference<PooledByteBuffer> ret = null;
      try {
        if (EncodedImage.isValid(newResult)) {
          ret = newResult.getByteBufferRef();
        }
        getConsumer().onNewResult(ret, status);
      } finally {
        CloseableReference.closeSafely(ret);
      }
    }
  }
}
