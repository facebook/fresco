/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.util.Pair;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Sets;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Producer for combining multiple identical requests into a single request.
 *
 * <p>Requests using the same key will be combined into a single request. This request is only
 * cancelled when all underlying requests are cancelled, and returns values to all underlying
 * consumers. If the request has already return one or more results but has not finished, then
 * any requests with the same key will have the most recent result returned to them immediately.
 *
 * @param <K> type of the key
 * @param <T> type of the closeable reference result that is returned to this producer
 */
@ThreadSafe
public abstract class MultiplexProducer<K, T extends Closeable> implements Producer<T> {

  /**
   * Map of multiplexers guarded by "this" lock. The lock should be used only to synchronize
   * accesses to this map. In particular, no callbacks or third party code should be run under
   * "this" lock.
   *
   * <p> The map might contain entries in progress, entries in progress for which cancellation
   * has been requested and ignored, or cancelled entries for which onCancellation has not been
   * called yet.
   */
  @GuardedBy("this")
  @VisibleForTesting final Map<K, Multiplexer> mMultiplexers;
  private final Producer<T> mInputProducer;

  protected MultiplexProducer(Producer<T> inputProducer) {
    mInputProducer = inputProducer;
    mMultiplexers = new HashMap<>();
  }

  @Override
  public void produceResults(Consumer<T> consumer, ProducerContext context) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("MultiplexProducer#produceResults");
      }
      K key = getKey(context);
      Multiplexer multiplexer;
      boolean createdNewMultiplexer;
      // We do want to limit scope of this lock to guard only accesses to mMultiplexers map.
      // However what we would like to do here is to atomically lookup mMultiplexers, add new
      // consumer to consumers set associated with the map's entry and call consumer's callback with
      // last intermediate result. We should not do all of those things under this lock.
      do {
        createdNewMultiplexer = false;
        synchronized (this) {
          multiplexer = getExistingMultiplexer(key);
          if (multiplexer == null) {
            multiplexer = createAndPutNewMultiplexer(key);
            createdNewMultiplexer = true;
          }
        }
        // addNewConsumer may call consumer's onNewResult method immediately. For this reason
        // we release "this" lock. If multiplexer is removed from mMultiplexers in the meantime,
        // which is not very probable, then addNewConsumer will fail and we will be able to retry.
      } while (!multiplexer.addNewConsumer(consumer, context));

      if (createdNewMultiplexer) {
        multiplexer.startInputProducerIfHasAttachedConsumers();
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  private synchronized Multiplexer getExistingMultiplexer(K key) {
    return mMultiplexers.get(key);
  }

  private synchronized Multiplexer createAndPutNewMultiplexer(K key) {
    Multiplexer multiplexer = new Multiplexer(key);
    mMultiplexers.put(key, multiplexer);
    return multiplexer;
  }

  private synchronized void removeMultiplexer(K key, Multiplexer multiplexer) {
    if (mMultiplexers.get(key) == multiplexer) {
      mMultiplexers.remove(key);
    }
  }

  protected abstract K getKey(ProducerContext producerContext);

  protected abstract T cloneOrNull(T object);

  /**
   * Multiplexes same requests - passes the same result to multiple consumers, manages cancellation
   * and maintains last intermediate result.
   *
   * <p> Multiplexed computation might be in one of 3 states:
   * <ul>
   *   <li> in progress </li>
   *   <li> in progress after requesting cancellation (cancellation has been denied) </li>
   *   <li> cancelled, but without onCancellation method being called yet </li>
   * </ul>
   *
   * <p> In last case new consumers may be added before onCancellation is called. When it is, the
   * Multiplexer has to check if it is the case and start next producer once again if so.
   */
  @VisibleForTesting class Multiplexer {
    private final K mKey;

    /**
     * Set of consumer-context pairs participating in multiplexing. Cancelled pairs
     * are removed from the set.
     *
     * <p> Following invariant is maintained: if mConsumerContextPairs is not empty, then this
     * instance of Multiplexer is present in mMultiplexers map. This way all ongoing multiplexed
     * requests might be attached to by other requests
     *
     * <p> A Multiplexer is removed from the map only if
     * <ul>
     *   <li> final result is received </li>
     *   <li> error is received </li>
     *   <li> cancellation notification is received and mConsumerContextPairs is empty </li>
     * </ul>
     */
    private final CopyOnWriteArraySet<Pair<Consumer<T>, ProducerContext>> mConsumerContextPairs;

    @GuardedBy("Multiplexer.this")
    @Nullable
    private T mLastIntermediateResult;
    @GuardedBy("Multiplexer.this")
    private float mLastProgress;
    @GuardedBy("Multiplexer.this")
    private @Consumer.Status int mLastStatus;

    /**
     * Producer context used for cancelling producers below MultiplexProducers, and for setting
     * whether the request is a prefetch or not.
     *
     * <p> If not null, then underlying computation has been started, and no onCancellation callback
     * has been received yet.
     */
    @GuardedBy("Multiplexer.this")
    @Nullable
    private BaseProducerContext mMultiplexProducerContext;

    /**
     * Currently used consumer of next producer.
     *
     * <p> The same Multiplexer might call mInputProducer.produceResults multiple times when
     * cancellation happens. This field is used to guard against late callbacks.
     *
     * <p>  If not null, then underlying computation has been started, and no onCancellation
     * callback has been received yet.
     */
    @GuardedBy("Multiplexer.this")
    @Nullable
    private ForwardingConsumer mForwardingConsumer;

    public Multiplexer(K key) {
      mConsumerContextPairs = Sets.newCopyOnWriteArraySet();
      mKey = key;
    }

    /**
     * Tries to add consumer to set of consumers participating in multiplexing. If successful and
     * appropriate intermediate result is already known, then it will be passed to the consumer.
     *
     * <p> This function will fail and return false if the multiplexer is not present in
     * mMultiplexers map.
     *
     * @return true if consumer was added successfully
     */
    public boolean addNewConsumer(
        final Consumer<T> consumer,
        final ProducerContext producerContext) {
      final Pair<Consumer<T>, ProducerContext> consumerContextPair =
          Pair.create(consumer, producerContext);
      T lastIntermediateResult;
      final List<ProducerContextCallbacks> prefetchCallbacks;
      final List<ProducerContextCallbacks> priorityCallbacks;
      final List<ProducerContextCallbacks> intermediateResultsCallbacks;
      final float lastProgress;
      final int lastStatus;

      // Check if Multiplexer is still in mMultiplexers map, and if so add new consumer.
      // Also store current intermediate result - we will notify consumer after acquiring
      // appropriate lock.
      synchronized (Multiplexer.this) {
        if (getExistingMultiplexer(mKey) != this) {
          return false;
        }
        mConsumerContextPairs.add(consumerContextPair);
        prefetchCallbacks = updateIsPrefetch();
        priorityCallbacks = updatePriority();
        intermediateResultsCallbacks = updateIsIntermediateResultExpected();
        lastIntermediateResult = mLastIntermediateResult;
        lastProgress = mLastProgress;
        lastStatus = mLastStatus;
      }

      BaseProducerContext.callOnIsPrefetchChanged(prefetchCallbacks);
      BaseProducerContext.callOnPriorityChanged(priorityCallbacks);
      BaseProducerContext.callOnIsIntermediateResultExpectedChanged(intermediateResultsCallbacks);

      synchronized (consumerContextPair) {
        // check if last result changed in the mean time. In such case we should not propagate it
        synchronized (Multiplexer.this) {
          if (lastIntermediateResult != mLastIntermediateResult) {
            lastIntermediateResult = null;
          } else if (lastIntermediateResult != null) {
            lastIntermediateResult = cloneOrNull(lastIntermediateResult);
          }
        }

        if (lastIntermediateResult != null) {
          if (lastProgress > 0) {
            consumer.onProgressUpdate(lastProgress);
          }
          consumer.onNewResult(lastIntermediateResult, lastStatus);
          closeSafely(lastIntermediateResult);
        }
      }

      addCallbacks(consumerContextPair, producerContext);
      return true;
    }

    /**
     * Register callbacks to be called when cancellation of consumer is requested, or if the
     * prefetch status of the consumer changes.
     */
    private void addCallbacks(
        final Pair<Consumer<T>, ProducerContext> consumerContextPair,
        final ProducerContext producerContext) {
      producerContext.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onCancellationRequested() {
              BaseProducerContext contextToCancel = null;
              List<ProducerContextCallbacks> isPrefetchCallbacks = null;
              List<ProducerContextCallbacks> priorityCallbacks = null;
              List<ProducerContextCallbacks> isIntermediateResultExpectedCallbacks = null;
              final boolean pairWasRemoved;

              synchronized (Multiplexer.this) {
                pairWasRemoved = mConsumerContextPairs.remove(consumerContextPair);
                if (pairWasRemoved) {
                  if (mConsumerContextPairs.isEmpty()) {
                    contextToCancel = mMultiplexProducerContext;
                  } else {
                    isPrefetchCallbacks = updateIsPrefetch();
                    priorityCallbacks = updatePriority();
                    isIntermediateResultExpectedCallbacks = updateIsIntermediateResultExpected();
                  }
                }
              }

              BaseProducerContext.callOnIsPrefetchChanged(isPrefetchCallbacks);
              BaseProducerContext.callOnPriorityChanged(priorityCallbacks);
              BaseProducerContext.callOnIsIntermediateResultExpectedChanged(
                  isIntermediateResultExpectedCallbacks);

              if (contextToCancel != null) {
                contextToCancel.cancel();
              }
              if (pairWasRemoved) {
                consumerContextPair.first.onCancellation();
              }
            }

            @Override
            public void onIsPrefetchChanged() {
              BaseProducerContext.callOnIsPrefetchChanged(updateIsPrefetch());
            }

            @Override
            public void onIsIntermediateResultExpectedChanged() {
              BaseProducerContext.callOnIsIntermediateResultExpectedChanged(
                  updateIsIntermediateResultExpected());
            }

            @Override
            public void onPriorityChanged() {
              BaseProducerContext.callOnPriorityChanged(updatePriority());
            }
          });
    }

    /**
     * Starts next producer if it is not started yet and there is at least one Consumer waiting for
     * the data. If all consumers are cancelled, then this multiplexer is removed from mRequest
     * map to clean up.
     */
    private void startInputProducerIfHasAttachedConsumers() {
      BaseProducerContext multiplexProducerContext;
      ForwardingConsumer forwardingConsumer;
      synchronized (Multiplexer.this) {
        Preconditions.checkArgument(mMultiplexProducerContext == null);
        Preconditions.checkArgument(mForwardingConsumer == null);

        // Cleanup if all consumers have been cancelled before this method was called
        if (mConsumerContextPairs.isEmpty()) {
          removeMultiplexer(mKey, this);
          return;
        }

        ProducerContext producerContext = mConsumerContextPairs.iterator().next().second;
        mMultiplexProducerContext = new BaseProducerContext(
            producerContext.getImageRequest(),
            producerContext.getId(),
            producerContext.getListener(),
            producerContext.getCallerContext(),
            producerContext.getLowestPermittedRequestLevel(),
            computeIsPrefetch(),
            computeIsIntermediateResultExpected(),
            computePriority());

        mForwardingConsumer = new ForwardingConsumer();
        multiplexProducerContext = mMultiplexProducerContext;
        forwardingConsumer = mForwardingConsumer;
      }
      mInputProducer.produceResults(
          forwardingConsumer,
          multiplexProducerContext);
    }

    @Nullable
    private synchronized List<ProducerContextCallbacks> updateIsPrefetch() {
      if (mMultiplexProducerContext == null) {
        return null;
      }
      return mMultiplexProducerContext.setIsPrefetchNoCallbacks(computeIsPrefetch());
    }

    private synchronized boolean computeIsPrefetch() {
      for (Pair<Consumer<T>, ProducerContext> pair : mConsumerContextPairs) {
        if (!pair.second.isPrefetch()) {
          return false;
        }
      }
      return true;
    }

    @Nullable
    private synchronized List<ProducerContextCallbacks> updateIsIntermediateResultExpected() {
      if (mMultiplexProducerContext == null) {
        return null;
      }
      return mMultiplexProducerContext.setIsIntermediateResultExpectedNoCallbacks(
          computeIsIntermediateResultExpected());
    }

    private synchronized boolean computeIsIntermediateResultExpected() {
      for (Pair<Consumer<T>, ProducerContext> pair : mConsumerContextPairs) {
        if (pair.second.isIntermediateResultExpected()) {
          return true;
        }
      }
      return false;
    }

    @Nullable
    private synchronized List<ProducerContextCallbacks> updatePriority() {
      if (mMultiplexProducerContext == null) {
        return null;
      }
      return mMultiplexProducerContext.setPriorityNoCallbacks(computePriority());
    }

    private synchronized Priority computePriority() {
      Priority priority = Priority.LOW;
      for (Pair<Consumer<T>, ProducerContext> pair : mConsumerContextPairs) {
        priority = Priority.getHigherPriority(priority, pair.second.getPriority());
      }
      return priority;
    }

    public void onFailure(final ForwardingConsumer consumer, final Throwable t) {
      Iterator<Pair<Consumer<T>, ProducerContext>> iterator;
      synchronized (Multiplexer.this) {
        // check for late callbacks
        if (mForwardingConsumer != consumer) {
          return;
        }

        iterator = mConsumerContextPairs.iterator();

        mConsumerContextPairs.clear();
        removeMultiplexer(mKey, this);
        closeSafely(mLastIntermediateResult);
        mLastIntermediateResult = null;
      }

      while (iterator.hasNext()) {
        Pair<Consumer<T>, ProducerContext> pair = iterator.next();
        synchronized (pair) {
          pair.first.onFailure(t);
        }
      }
    }

    public void onNextResult(
        final ForwardingConsumer consumer,
        final T closeableObject,
        @Consumer.Status final int status) {
      Iterator<Pair<Consumer<T>, ProducerContext>> iterator;
      synchronized (Multiplexer.this) {
        // check for late callbacks
        if (mForwardingConsumer != consumer) {
          return;
        }

        closeSafely(mLastIntermediateResult);
        mLastIntermediateResult = null;

        iterator = mConsumerContextPairs.iterator();
        if (BaseConsumer.isNotLast(status)) {
          mLastIntermediateResult = cloneOrNull(closeableObject);
          mLastStatus = status;
        } else {
          mConsumerContextPairs.clear();
          removeMultiplexer(mKey, this);
        }
      }

      while (iterator.hasNext()) {
        Pair<Consumer<T>, ProducerContext> pair = iterator.next();
        synchronized (pair) {
          pair.first.onNewResult(closeableObject, status);
        }
      }
    }

    public void onCancelled(final ForwardingConsumer forwardingConsumer) {
      synchronized (Multiplexer.this) {
        // check for late callbacks
        if (mForwardingConsumer != forwardingConsumer) {
          return;
        }

        mForwardingConsumer = null;
        mMultiplexProducerContext = null;
        closeSafely(mLastIntermediateResult);
        mLastIntermediateResult = null;
      }

      startInputProducerIfHasAttachedConsumers();
    }

    public void onProgressUpdate(ForwardingConsumer forwardingConsumer, float progress) {
      Iterator<Pair<Consumer<T>, ProducerContext>> iterator;
      synchronized (Multiplexer.this) {
        // check for late callbacks
        if (mForwardingConsumer != forwardingConsumer) {
          return;
        }

        mLastProgress = progress;
        iterator = mConsumerContextPairs.iterator();
      }

      while (iterator.hasNext()) {
        Pair<Consumer<T>, ProducerContext> pair = iterator.next();
        synchronized (pair) {
          pair.first.onProgressUpdate(progress);
        }
      }
    }

    private void closeSafely(Closeable obj) {
      try {
        if (obj != null) {
          obj.close();
        }
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

    /**
     * Forwards {@link Consumer} methods to Multiplexer.
     */
    private class ForwardingConsumer extends BaseConsumer<T> {
      @Override
      protected void onNewResultImpl(T newResult, @Status int status) {
        try {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.beginSection("MultiplexProducer#onNewResult");
          }
          Multiplexer.this.onNextResult(this, newResult, status);
        } finally {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.endSection();
          }
        }
      }

      @Override
      protected void onFailureImpl(Throwable t) {
        try {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.beginSection("MultiplexProducer#onFailure");
          }
          Multiplexer.this.onFailure(this, t);
        } finally {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.endSection();
          }
        }
      }

      @Override
      protected void onCancellationImpl() {
        try {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.beginSection("MultiplexProducer#onCancellation");
          }
          Multiplexer.this.onCancelled(this);
        } finally {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.endSection();
          }
        }
      }

      @Override
      protected void onProgressUpdateImpl(float progress) {
        try {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.beginSection("MultiplexProducer#onProgressUpdate");
          }
          Multiplexer.this.onProgressUpdate(this, progress);
        } finally {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.endSection();
          }
        }
      }
    }
  }
}
