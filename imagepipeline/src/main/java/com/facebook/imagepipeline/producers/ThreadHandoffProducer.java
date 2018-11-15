/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Preconditions;
import javax.annotation.Nullable;

/**
 * Uses ExecutorService to move further computation to different thread
 */
public class ThreadHandoffProducer<T> implements Producer<T> {

  public static final String PRODUCER_NAME = "BackgroundThreadHandoffProducer";

  private final Producer<T> mInputProducer;
  private final ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

  public ThreadHandoffProducer(final Producer<T> inputProducer,
                               final  ThreadHandoffProducerQueue inputThreadHandoffProducerQueue) {
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mThreadHandoffProducerQueue = inputThreadHandoffProducerQueue;
  }

  @Override
  public void produceResults(final Consumer<T> consumer, final ProducerContext context) {
    final ProducerListener producerListener = context.getListener();
    final String requestId = context.getId();
    final StatefulProducerRunnable<T> statefulRunnable =
        new StatefulProducerRunnable<T>(consumer, producerListener, PRODUCER_NAME, requestId) {
          @Override
          protected void onSuccess(T ignored) {
            producerListener.onProducerFinishWithSuccess(requestId, PRODUCER_NAME, null);
            mInputProducer.produceResults(consumer, context);
          }

          @Override
          protected void disposeResult(T ignored) {}

          @Override
          protected @Nullable T getResult() throws Exception {
            return null;
          }
        };
    context.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            statefulRunnable.cancel();
            mThreadHandoffProducerQueue.remove(statefulRunnable);
          }
        });
    mThreadHandoffProducerQueue.addToQueueOrExecute(statefulRunnable);
  }
}
