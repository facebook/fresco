/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

/**
 * Swallow result producer.
 *
 * <p>This producer just inserts a consumer that swallows results into the stack of consumers.
 */
public class SwallowResultProducer<T> implements Producer<Void> {
  private final Producer<T> mNextProducer;

  public SwallowResultProducer(Producer<T> nextProducer) {
    mNextProducer = nextProducer;
  }

  @Override
  public void produceResults(Consumer<Void> consumer, ProducerContext producerContext) {
    DelegatingConsumer<T, Void> swallowResultConsumer = new DelegatingConsumer<T, Void>(consumer) {
      @Override
      protected void onNewResultImpl(T newResult, boolean isLast) {
        if (isLast) {
          getConsumer().onNewResult(null, isLast);
        }
      }
    };
    mNextProducer.produceResults(swallowResultConsumer, producerContext);
  }
}
