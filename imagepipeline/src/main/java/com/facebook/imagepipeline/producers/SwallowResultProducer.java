/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Swallow result producer.
 *
 * <p>This producer just inserts a consumer that swallows results into the stack of consumers.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class SwallowResultProducer<T> implements Producer<Void> {
  private final Producer<T> mInputProducer;

  public SwallowResultProducer(Producer<T> inputProducer) {
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(Consumer<Void> consumer, ProducerContext producerContext) {
    DelegatingConsumer<T, Void> swallowResultConsumer =
        new DelegatingConsumer<T, Void>(consumer) {
          @Override
          protected void onNewResultImpl(@Nullable T newResult, @Status int status) {
            if (isLast(status)) {
              getConsumer().onNewResult(null, status);
            }
          }
        };
    mInputProducer.produceResults(swallowResultConsumer, producerContext);
  }
}
