/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.image.EncodedImage;

/**
 * Add image transform meta data producer
 *
 * <p>Extracts meta data from the results passed down from the next producer, and adds it to the
 * result that it returns to the consumer.
 */
public class AddImageTransformMetaDataProducer implements Producer<EncodedImage> {
  private final Producer<EncodedImage> mInputProducer;

  public AddImageTransformMetaDataProducer(Producer<EncodedImage> inputProducer) {
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(Consumer<EncodedImage> consumer, ProducerContext context) {
    mInputProducer.produceResults(new AddImageTransformMetaDataConsumer(consumer), context);
  }

  private static class AddImageTransformMetaDataConsumer extends DelegatingConsumer<
      EncodedImage, EncodedImage> {

    private AddImageTransformMetaDataConsumer(Consumer<EncodedImage> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(EncodedImage newResult, @Status int status) {
      if (newResult == null) {
        getConsumer().onNewResult(null, status);
        return;
      }
      if (!EncodedImage.isMetaDataAvailable(newResult)) {
        newResult.parseMetaData();
      }
      getConsumer().onNewResult(newResult, status);
    }
  }
}
