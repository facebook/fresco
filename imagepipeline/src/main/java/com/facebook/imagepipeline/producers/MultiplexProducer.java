/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import android.util.Pair;

import com.facebook.common.internal.Maps;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Sets;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.common.Priority;

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
public abstract class MultiplexProducer<K, T> implements Producer<CloseableReference<T>> {

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
  private final Producer<CloseableReference<T>> mNextProducer;

  protected MultiplexProducer(Producer nextProducer) {
    mNextProducer = nextProducer;
    mMultiplexers = Maps.newHashMap();
  }

  @Override
  public void produceResults(Consumer<CloseableReference<T>> consumer, ProducerContext context) {
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
      multiplexer.startNextProducerIfHasAttachedConsumers();
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
    private final CopyOnWriteArraySet<Pair<Consumer<CloseableReference<T>>, ProducerContext>>
        mConsumerContextPairs;

    @GuardedBy("Multiplexer.this")
    @Nullable
    private CloseableReference<T> mLastIntermediateResult;
    @GuardedBy("Multiplexer.this")
    private float mLastProgress;

    /**
     * Producer context used for cancelling producers below MultiplexProducers, and for setting
     * whether the request is a prefetch or not.
     *
     * <p> If not null, then underlying computation has been started, and no onCancellation callback
     * has been received yet.
     */
    @GuardedBy("Multiplexer.this")
    @Nullable
    private SettableProducerContext mMultiplexProducerContext;

    /**
     * Currently used consumer of next producer.
     *
     * <p> The same Multiplexer might call mNextProducer.produceResults multiple times when
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
        final Consumer<CloseableReference<T>> consumer,
        final ProducerContext producerContext) {
      final Pair<Consumer<CloseableReference<T>>, ProducerContext> consumerContextPair =
          Pair.create(consumer, producerContext);
      CloseableReference<T> lastIntermediateResult;
      float lastProgress;

      // Check if Multiplexer is still in mMultiplexers map, and if so add new consumer.
      // Also store current intermediate result - we will notify consumer after acquiring
      // appropriate lock.
      synchronized (Multiplexer.this) {
        if (getExistingMultiplexer(mKey) != this) {
          return false;
        }
        mConsumerContextPairs.add(consumerContextPair);
        if (mMultiplexProducerContext != null) {
          if (mMultiplexProducerContext.isPrefetch()) {
            mMultiplexProducerContext.setIsPrefetch(consumerContextPair.second.isPrefetch());
          }
          if (!mMultiplexProducerContext.isIntermediateResultExpected()) {
            mMultiplexProducerContext.setIsIntermediateResultExpected(
                consumerContextPair.second.isIntermediateResultExpected());
          }
          mMultiplexProducerContext.setPriority(
              Priority.getHigherPriority(
                  mMultiplexProducerContext.getPriority(),
                  consumerContextPair.second.getPriority()));
        }
        lastIntermediateResult = mLastIntermediateResult;
        lastProgress = mLastProgress;
      }

      synchronized (consumerContextPair) {
        // check if last result changed in the mean time. In such case we should not propagate it
        synchronized (Multiplexer.this) {
          if (lastIntermediateResult != mLastIntermediateResult) {
            lastIntermediateResult = null;
          } else if (lastIntermediateResult != null) {
            lastIntermediateResult = lastIntermediateResult.clone();
          }
        }

        if (lastIntermediateResult != null) {
          if (lastProgress > 0) {
            consumer.onProgressUpdate(lastProgress);
          }
          consumer.onNewResult(lastIntermediateResult, false);
          lastIntermediateResult.close();
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
        final Pair<Consumer<CloseableReference<T>>, ProducerContext> consumerContextPair,
        final ProducerContext producerContext) {
      producerContext.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onCancellationRequested() {
              SettableProducerContext contextToCancel = null;
              boolean pairWasRemoved = false;
              synchronized (Multiplexer.this) {
                pairWasRemoved = mConsumerContextPairs.remove(consumerContextPair);
                if (pairWasRemoved) {
                  if (mConsumerContextPairs.isEmpty()) {
                    contextToCancel = mMultiplexProducerContext;
                  } else if (mMultiplexProducerContext != null) {
                    if (!mMultiplexProducerContext.isPrefetch() &&
                        !consumerContextPair.second.isPrefetch()) {
                      mMultiplexProducerContext.setIsPrefetch(isPrefetch());
                    }
                    if (consumerContextPair.second.isIntermediateResultExpected()) {
                      mMultiplexProducerContext.setIsIntermediateResultExpected(
                          isIntermediateResultExpected());
                    }
                    if (mMultiplexProducerContext.getPriority().equals(
                        consumerContextPair.second.getPriority())) {
                      mMultiplexProducerContext.setPriority(getPriority());
                    }
                  }
                }
              }
              if (contextToCancel != null) {
                contextToCancel.cancel();
              }
              if (pairWasRemoved) {
                consumerContextPair.first.onCancellation();
              }
            }

            @Override
            public void onIsPrefetchChanged() {
              synchronized (Multiplexer.this) {
                if (mMultiplexProducerContext != null) {
                  if (mMultiplexProducerContext.isPrefetch()) {
                    mMultiplexProducerContext.setIsPrefetch(
                        consumerContextPair.second.isPrefetch());
                  } else if (consumerContextPair.second.isPrefetch()) {
                    mMultiplexProducerContext.setIsPrefetch(isPrefetch());
                  }
                }
              }
            }

            @Override
            public void onIsIntermediateResultExpectedChanged() {
              synchronized (Multiplexer.this) {
                if (mMultiplexProducerContext != null) {
                  if (consumerContextPair.second.isIntermediateResultExpected()) {
                    mMultiplexProducerContext.setIsIntermediateResultExpected(true);
                  } else if (mMultiplexProducerContext.isIntermediateResultExpected()) {
                    mMultiplexProducerContext.setIsIntermediateResultExpected(
                        isIntermediateResultExpected());
                  }
                }
              }
            }

            @Override
            public void onPriorityChanged() {
              synchronized (Multiplexer.this) {
                if (mMultiplexProducerContext != null) {
                  Priority newPriority = consumerContextPair.second.getPriority();
                  if (Priority.getHigherPriority(
                          mMultiplexProducerContext.getPriority(),
                          newPriority).equals(newPriority)) {
                    mMultiplexProducerContext.setPriority(newPriority);
                  } else {
                    mMultiplexProducerContext.setPriority(getPriority());
                  }
                }
              }
            }
          });
    }

    /**
     * Starts next producer if it is not started yet and there is at least one Consumer waiting for
     * the data. If all consumers are cancelled, then this multiplexer is removed from mRequest
     * map to clean up.
     */
    private void startNextProducerIfHasAttachedConsumers() {
      SettableProducerContext multiplexProducerContext;
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
        mMultiplexProducerContext = new SettableProducerContext(
            producerContext.getImageRequest(),
            producerContext.getId(),
            producerContext.getListener(),
            producerContext.getCallerContext(),
            producerContext.getLowestPermittedRequestLevel(),
            isPrefetch(),
            isIntermediateResultExpected(),
            getPriority());
        mForwardingConsumer = new ForwardingConsumer();
        multiplexProducerContext = mMultiplexProducerContext;
        forwardingConsumer = mForwardingConsumer;
      }
      mNextProducer.produceResults(
          forwardingConsumer,
          multiplexProducerContext);
    }

    private synchronized boolean isPrefetch() {
      for (Pair<Consumer<CloseableReference<T>>, ProducerContext> pair : mConsumerContextPairs) {
        if (!pair.second.isPrefetch()) {
          return false;
        }
      }
      return true;
    }

    private synchronized boolean isIntermediateResultExpected() {
      for (Pair<Consumer<CloseableReference<T>>, ProducerContext> pair : mConsumerContextPairs) {
        if (pair.second.isIntermediateResultExpected()) {
          return true;
        }
      }
      return false;
    }

    private synchronized Priority getPriority() {
      Priority priority = Priority.LOW;
      for (Pair<Consumer<CloseableReference<T>>, ProducerContext> pair : mConsumerContextPairs) {
        priority = Priority.getHigherPriority(priority, pair.second.getPriority());
      }
      return priority;
    }

    public void onFailure(final ForwardingConsumer consumer, final Throwable t) {
      Iterator<Pair<Consumer<CloseableReference<T>>, ProducerContext>> iterator;
      synchronized (Multiplexer.this) {
        // check for late callbacks
        if (mForwardingConsumer != consumer) {
          return;
        }

        iterator = mConsumerContextPairs.iterator();

        mConsumerContextPairs.clear();
        removeMultiplexer(mKey, this);
        CloseableReference.closeSafely(mLastIntermediateResult);
        mLastIntermediateResult = null;
      }

      while (iterator.hasNext()) {
        Pair<Consumer<CloseableReference<T>>, ProducerContext> pair = iterator.next();
        synchronized (pair) {
          pair.first.onFailure(t);
        }
      }
    }

    public void onNextResult(
        final ForwardingConsumer consumer,
        final CloseableReference<T> closeableReference,
        final boolean isFinal) {
      Iterator<Pair<Consumer<CloseableReference<T>>, ProducerContext>> iterator;
      synchronized (Multiplexer.this) {
        // check for late callbacks
        if (mForwardingConsumer != consumer) {
          return;
        }

        CloseableReference.closeSafely(mLastIntermediateResult);
        mLastIntermediateResult = null;

        iterator = mConsumerContextPairs.iterator();
        if (!isFinal) {
          mLastIntermediateResult = closeableReference.clone();
        } else {
          mConsumerContextPairs.clear();
          removeMultiplexer(mKey, this);
        }
      }

      while (iterator.hasNext()) {
        Pair<Consumer<CloseableReference<T>>, ProducerContext> pair = iterator.next();
        synchronized (pair) {
          pair.first.onNewResult(closeableReference, isFinal);
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
        CloseableReference.closeSafely(mLastIntermediateResult);
        mLastIntermediateResult = null;
      }

      startNextProducerIfHasAttachedConsumers();
    }

    public void onProgressUpdate(ForwardingConsumer forwardingConsumer, float progress) {
      Iterator<Pair<Consumer<CloseableReference<T>>, ProducerContext>> iterator;
      synchronized (Multiplexer.this) {
        // check for late callbacks
        if (mForwardingConsumer != forwardingConsumer) {
          return;
        }

        mLastProgress = progress;
        iterator = mConsumerContextPairs.iterator();
      }

      while (iterator.hasNext()) {
        Pair<Consumer<CloseableReference<T>>, ProducerContext> pair = iterator.next();
        synchronized (pair) {
          pair.first.onProgressUpdate(progress);
        }
      }
    }

    /**
     * Forwards {@link Consumer} methods to Multiplexer.
     */
    private class ForwardingConsumer extends BaseConsumer<CloseableReference<T>> {
      @Override
      protected void onNewResultImpl(CloseableReference<T> newResult, boolean isLast) {
        Multiplexer.this.onNextResult(this, newResult, isLast);
      }

      @Override
      protected void onFailureImpl(Throwable t) {
        Multiplexer.this.onFailure(this, t);
      }

      @Override
      protected void onCancellationImpl() {
        Multiplexer.this.onCancelled(this);
      }

      @Override
      protected void onProgressUpdateImpl(float progress) {
        Multiplexer.this.onProgressUpdate(this, progress);
      }
    }
  }
}
