/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.util.Pair;
import com.facebook.common.internal.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import javax.annotation.concurrent.GuardedBy;

/**
 * Only permits a configurable number of requests to be kicked off simultaneously. If that number is
 * exceeded, then requests are queued up and kicked off once other requests complete.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ThrottlingProducer<T> implements Producer<T> {

  public static final String PRODUCER_NAME = "ThrottlingProducer";

  private final Producer<T> mInputProducer;
  private final int mMaxSimultaneousRequests;

  @GuardedBy("this")
  private int mNumCurrentRequests;

  @GuardedBy("this")
  private final ConcurrentLinkedQueue<Pair<Consumer<T>, ProducerContext>> mPendingRequests;

  private final Executor mExecutor;

  public ThrottlingProducer(
      int maxSimultaneousRequests, Executor executor, final Producer<T> inputProducer) {
    mMaxSimultaneousRequests = maxSimultaneousRequests;
    mExecutor = Preconditions.checkNotNull(executor);
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mPendingRequests = new ConcurrentLinkedQueue<Pair<Consumer<T>, ProducerContext>>();
    mNumCurrentRequests = 0;
  }

  @Override
  public void produceResults(final Consumer<T> consumer, final ProducerContext producerContext) {
    final ProducerListener2 producerListener = producerContext.getProducerListener();
    producerListener.onProducerStart(producerContext, PRODUCER_NAME);

    boolean delayRequest;
    synchronized (this) {
      if (mNumCurrentRequests >= mMaxSimultaneousRequests) {
        mPendingRequests.add(Pair.create(consumer, producerContext));
        delayRequest = true;
      } else {
        mNumCurrentRequests++;
        delayRequest = false;
      }
    }

    if (!delayRequest) {
      produceResultsInternal(consumer, producerContext);
    }
  }

  void produceResultsInternal(Consumer<T> consumer, ProducerContext producerContext) {
    ProducerListener2 producerListener = producerContext.getProducerListener();
    producerListener.onProducerFinishWithSuccess(producerContext, PRODUCER_NAME, null);
    mInputProducer.produceResults(new ThrottlerConsumer(consumer), producerContext);
  }

  private class ThrottlerConsumer extends DelegatingConsumer<T, T> {

    private ThrottlerConsumer(Consumer<T> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(T newResult, @Status int status) {
      getConsumer().onNewResult(newResult, status);
      if (isLast(status)) {
        onRequestFinished();
      }
    }

    @Override
    protected void onFailureImpl(Throwable t) {
      getConsumer().onFailure(t);
      onRequestFinished();
    }

    @Override
    protected void onCancellationImpl() {
      getConsumer().onCancellation();
      onRequestFinished();
    }

    private void onRequestFinished() {
      final Pair<Consumer<T>, ProducerContext> nextRequestPair;
      synchronized (ThrottlingProducer.this) {
        nextRequestPair = mPendingRequests.poll();
        if (nextRequestPair == null) {
          mNumCurrentRequests--;
        }
      }

      if (nextRequestPair != null) {
        mExecutor.execute(
            new Runnable() {
              @Override
              public void run() {
                produceResultsInternal(nextRequestPair.first, nextRequestPair.second);
              }
            });
      }
    }
  }
}
