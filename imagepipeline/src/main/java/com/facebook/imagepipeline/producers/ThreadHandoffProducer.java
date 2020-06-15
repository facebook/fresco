/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.instrumentation.FrescoInstrumenter;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

/** Uses ExecutorService to move further computation to different thread */
public class ThreadHandoffProducer<T> implements Producer<T> {

  public static final String PRODUCER_NAME = "BackgroundThreadHandoffProducer";

  private final Producer<T> mInputProducer;
  private final ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

  public ThreadHandoffProducer(
      final Producer<T> inputProducer,
      final ThreadHandoffProducerQueue inputThreadHandoffProducerQueue) {
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mThreadHandoffProducerQueue = inputThreadHandoffProducerQueue;
  }

  @Override
  public void produceResults(final Consumer<T> consumer, final ProducerContext context) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ThreadHandoffProducer#produceResults");
      }
      final ProducerListener2 producerListener = context.getProducerListener();
      final StatefulProducerRunnable<T> statefulRunnable =
          new StatefulProducerRunnable<T>(consumer, producerListener, context, PRODUCER_NAME) {
            @Override
            protected void onSuccess(T ignored) {
              producerListener.onProducerFinishWithSuccess(context, PRODUCER_NAME, null);
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

      mThreadHandoffProducerQueue.addToQueueOrExecute(
          FrescoInstrumenter.decorateRunnable(statefulRunnable, getInstrumentationTag(context)));
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Nullable
  private static String getInstrumentationTag(ProducerContext context) {
    return FrescoInstrumenter.isTracing()
        ? "ThreadHandoffProducer_produceResults_" + context.getId()
        : null;
  }
}
