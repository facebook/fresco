/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
  private final Producer<EncodedImage> mNextProducer;

  public AddImageTransformMetaDataProducer(Producer<EncodedImage> nextProducer) {
    mNextProducer = nextProducer;
  }

  @Override
  public void produceResults(Consumer<EncodedImage> consumer, ProducerContext context) {
    mNextProducer.produceResults(new AddImageTransformMetaDataConsumer(consumer), context);
  }

  private static class AddImageTransformMetaDataConsumer extends DelegatingConsumer<
      EncodedImage, EncodedImage> {

    private AddImageTransformMetaDataConsumer(Consumer<EncodedImage> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      if (newResult == null) {
        getConsumer().onNewResult(null, isLast);
        return;
      }
      if (!EncodedImage.isMetaDataAvailable(newResult)) {
        newResult.parseMetaData();
      }
      getConsumer().onNewResult(newResult, isLast);
    }
  }
}
