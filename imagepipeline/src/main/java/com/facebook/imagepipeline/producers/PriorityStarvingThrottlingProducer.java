/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Executor;
import javax.annotation.concurrent.GuardedBy;

/**
 * Time based, priority starving throttling producer.
 *
 * <p>This means for any # of elements, all of the higher priority items will be run before any of
 * the lower priority items. Within the groups, send order is the same as order they were given to
 * the class. (based on nano time).
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class PriorityStarvingThrottlingProducer<T> implements Producer<T> {

  public static final String PRODUCER_NAME = "PriorityStarvingThrottlingProducer";

  private final Producer<T> mInputProducer;
  private final int mMaxSimultaneousRequests;

  @GuardedBy("this")
  private final Queue<Item<T>> mPendingRequests;

  private final Executor mExecutor;

  @GuardedBy("this")
  private int mNumCurrentRequests;

  public PriorityStarvingThrottlingProducer(
      int maxSimultaneousRequests, Executor executor, final Producer<T> inputProducer) {
    mMaxSimultaneousRequests = maxSimultaneousRequests;
    mExecutor = Preconditions.checkNotNull(executor);
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mPendingRequests = new PriorityQueue<>(11, new PriorityComparator<T>());
    mNumCurrentRequests = 0;
  }

  @Override
  public void produceResults(final Consumer<T> consumer, final ProducerContext producerContext) {
    final long time = System.nanoTime();
    final ProducerListener2 producerListener = producerContext.getProducerListener();
    producerListener.onProducerStart(producerContext, PRODUCER_NAME);
    boolean delayRequest;
    synchronized (this) {
      if (mNumCurrentRequests >= mMaxSimultaneousRequests) {
        mPendingRequests.add(new Item<>(consumer, producerContext, time));
        delayRequest = true;
      } else {
        mNumCurrentRequests++;
        delayRequest = false;
      }
    }

    if (!delayRequest) {
      produceResultsInternal(new Item<>(consumer, producerContext, time));
    }
  }

  private void produceResultsInternal(Item<T> item) {
    ProducerListener2 producerListener = item.producerContext.getProducerListener();
    producerListener.onProducerFinishWithSuccess(item.producerContext, PRODUCER_NAME, null);
    mInputProducer.produceResults(new ThrottlerConsumer(item.consumer), item.producerContext);
  }

  static class Item<T> {
    final Consumer<T> consumer;
    final ProducerContext producerContext;
    final long time;

    Item(Consumer<T> consumer, ProducerContext producerContext, long time) {
      this.consumer = consumer;
      this.producerContext = producerContext;
      this.time = time;
    }
  }

  static class PriorityComparator<T> implements Comparator<Item<T>> {
    @Override
    public int compare(Item<T> o1, Item<T> o2) {

      Priority p1 = o1.producerContext.getPriority();
      Priority p2 = o2.producerContext.getPriority();

      if (p1 == p2) {
        // lower time wins in this case, so minimum value
        return Double.compare(o1.time, o2.time);
      } else {
        // priority ordering is via ordinals
        if (p1.ordinal() > p2.ordinal()) {
          // higher priority gets minimum value (pq is min-heap based)
          return -1;
        } else {
          return 1;
        }
      }
    }
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
      final Item<T> nextRequest;
      synchronized (PriorityStarvingThrottlingProducer.this) {
        nextRequest = mPendingRequests.poll();
        if (nextRequest == null) {
          mNumCurrentRequests--;
        }
      }

      if (nextRequest != null) {
        mExecutor.execute(
            new Runnable() {
              @Override
              public void run() {
                produceResultsInternal(nextRequest);
              }
            });
      }
    }
  }
}
