/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.concurrent.Executor;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * Uses ExecutorService to move further computation to different thread
 */
public class ThreadHandoffProducer<T> implements Producer<T> {

  @VisibleForTesting
  protected static final String PRODUCER_NAME = "BackgroundThreadHandoffProducer";

  private final Executor mExecutor;
  private final Producer<T> mInputProducer;

  public ThreadHandoffProducer(final Executor executorService, final Producer<T> inputProducer) {
    mExecutor = Preconditions.checkNotNull(executorService);
    mInputProducer = Preconditions.checkNotNull(inputProducer);
  }

  @Override
  public void produceResults(final Consumer<T> consumer, final ProducerContext context) {
    final ProducerListener producerListener = context.getListener();
    final String requestId = context.getId();
    final StatefulProducerRunnable<T> statefulRunnable = new StatefulProducerRunnable<T>(
        consumer,
        producerListener,
        PRODUCER_NAME,
        requestId) {
      @Override
      protected void onSuccess(T ignored) {
        producerListener.onProducerFinishWithSuccess(requestId, PRODUCER_NAME, null);
        mInputProducer.produceResults(consumer, context);
      }

      @Override
      protected void disposeResult(T ignored) {}

      @Override
      protected T getResult() throws Exception {
        return null;
      }
    };
    context.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            statefulRunnable.cancel();
          }
        });
    mExecutor.execute(statefulRunnable);
  }
}
