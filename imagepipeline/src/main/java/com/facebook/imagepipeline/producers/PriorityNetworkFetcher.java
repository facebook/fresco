/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static com.facebook.imagepipeline.common.Priority.HIGH;

import androidx.annotation.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.time.MonotonicClock;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.imagepipeline.image.EncodedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * PriorityNetworkFetcher fetches images using a priority queue.
 *
 * <p>Fetches are delegated to another fetcher.
 *
 * <ul>
 *   <li>Two queues are maintained, one for each priority.
 *   <li>High-priority images (e.g, on-screen) are handled FIFO or LIFO, depending on a flag.
 *   <li>Low-priority images (e.g., prefetches) are handled FIFO.
 *   <li>Dequeuing is done thusly:
 *       <ul>
 *         <li>If there's an enqueued hi-pri requests, and there are less than 'maxOutstandingHiPri'
 *             currently active downloads, it is dequeued; then,
 *         <li>If there's an enqueued low-pri requests, and there are less than
 *             'maxOutstandingLowPri' currently active downloads, it is dequeued.
 *       </ul>
 *   <li>When a request's priority changes, it is taken out of the queue and re-enqueued according
 *       to the rules above.
 * </ul>
 */
public class PriorityNetworkFetcher<FETCH_STATE extends FetchState>
    implements NetworkFetcher<PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE>> {
  public static final String TAG = PriorityNetworkFetcher.class.getSimpleName();

  private final NetworkFetcher<FETCH_STATE> mDelegate;

  private final boolean mIsHiPriFifo;
  private final int mMaxOutstandingHiPri;
  private final int mMaxOutstandingLowPri;
  private final MonotonicClock mClock;

  private final Object mLock = new Object();
  private final LinkedList<PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE>> mHiPriQueue =
      new LinkedList<>();
  private final LinkedList<PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE>> mLowPriQueue =
      new LinkedList<>();
  private final HashSet<PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE>> mCurrentlyFetching =
      new HashSet<>();
  private final LinkedList<PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE>> mDelayedQueue =
      new LinkedList<>();

  private volatile boolean isRunning = true;

  private final boolean inflightFetchesCanBeCancelled;
  private final int maxNumberOfRequeue;
  @VisibleForTesting static final int INFINITE_REQUEUE = -1;
  private final boolean doNotCancelRequests;

  private long firstDelayedRequestEnqueuedTimeStamp;

  /** the amount of time a request should wait before it gets re-queued again */
  private final long requeueDelayTimeInMillis;
  /** the number of immediate re-queue, with no delay */
  private final int immediateRequeueCount;

  @VisibleForTesting static final int NO_DELAYED_REQUESTS = -1;

  /** if true, then dequeue requests recuresively */
  private final boolean multipleDequeue;

  /**
   * @param isHiPriFifo if true, hi-pri requests are dequeued in the order they were enqueued.
   *     Otherwise, they're dequeued in reverse order.
   * @param inflightFetchesCanBeCancelled if false, the fetcher waits for the completion of requests
   *     that have been delegated to 'delegate' even if they were cancelled by Fresco. The
   *     cancellation order is not propagated to 'delegate', and no other request is dequeued.
   * @param maxNumberOfRequeue requests that fail are re-queued up to maxNumberOfRequeue times,
   *     potentially retrying immediately. INFINITE_REQUEUE(-1) means infinite requeue.
   * @param doNotCancelRequests if true, requests will not be removed from the queue on cancellation
   * @param immediateRequeueCount number of immediate requeue attempts, when a request is re-queued
   *     more then this number, it will get re-queued again after at most requeueDelayTimeInMillis.
   *     NO_DELAYED_REQUESTS(-1) means no delay, no matter how many times the request is re-queued.
   * @param requeueDelayTimeInMillis the amount of time in ms a request needs to wait until getting
   *     re-queued again after it gets re-queued more than immediateRequeueCount times. This is an
   *     upper bound of delay time.
   */
  public PriorityNetworkFetcher(
      NetworkFetcher<FETCH_STATE> delegate,
      boolean isHiPriFifo,
      int maxOutstandingHiPri,
      int maxOutstandingLowPri,
      boolean inflightFetchesCanBeCancelled,
      int maxNumberOfRequeue,
      boolean doNotCancelRequests,
      int immediateRequeueCount,
      int requeueDelayTimeInMillis,
      boolean multipleDequeue) {
    this(
        delegate,
        isHiPriFifo,
        maxOutstandingHiPri,
        maxOutstandingLowPri,
        inflightFetchesCanBeCancelled,
        maxNumberOfRequeue,
        doNotCancelRequests,
        immediateRequeueCount,
        requeueDelayTimeInMillis,
        multipleDequeue,
        RealtimeSinceBootClock.get());
  }

  /**
   * @param isHiPriFifo if true, hi-pri requests are dequeued in the order they were enqueued.
   *     Otherwise, they're dequeued in reverse order.
   * @param inflightFetchesCanBeCancelled if false, the fetcher waits for the completion of requests
   *     that have been delegated to 'delegate' even if they were cancelled by Fresco. The
   *     cancellation order is not propagated to 'delegate', and no other request is dequeued.
   * @param infiniteRequeue if true, requests that fail are re-queued, potentially retrying
   *     immediately.
   */
  public PriorityNetworkFetcher(
      NetworkFetcher<FETCH_STATE> delegate,
      boolean isHiPriFifo,
      int maxOutstandingHiPri,
      int maxOutstandingLowPri,
      boolean inflightFetchesCanBeCancelled,
      boolean infiniteRequeue,
      boolean doNotCancelRequests) {
    this(
        delegate,
        isHiPriFifo,
        maxOutstandingHiPri,
        maxOutstandingLowPri,
        inflightFetchesCanBeCancelled,
        infiniteRequeue ? INFINITE_REQUEUE : 0,
        doNotCancelRequests,
        NO_DELAYED_REQUESTS,
        0,
        false,
        RealtimeSinceBootClock.get());
  }

  @VisibleForTesting
  public PriorityNetworkFetcher(
      NetworkFetcher<FETCH_STATE> delegate,
      boolean isHiPriFifo,
      int maxOutstandingHiPri,
      int maxOutstandingLowPri,
      boolean inflightFetchesCanBeCancelled,
      int maxNumberOfRequeue,
      boolean doNotCancelRequests,
      int immediateRequeueCount,
      int requeueDelayTimeInMillis,
      boolean multipleDequeue,
      MonotonicClock clock) {
    mDelegate = delegate;
    mIsHiPriFifo = isHiPriFifo;

    this.mMaxOutstandingHiPri = maxOutstandingHiPri;
    this.mMaxOutstandingLowPri = maxOutstandingLowPri;
    if (maxOutstandingHiPri <= maxOutstandingLowPri) {
      throw new IllegalArgumentException("maxOutstandingHiPri should be > maxOutstandingLowPri");
    }
    this.inflightFetchesCanBeCancelled = inflightFetchesCanBeCancelled;
    this.maxNumberOfRequeue = maxNumberOfRequeue;
    this.doNotCancelRequests = doNotCancelRequests;
    this.immediateRequeueCount = immediateRequeueCount;
    this.requeueDelayTimeInMillis = requeueDelayTimeInMillis;
    this.multipleDequeue = multipleDequeue;
    this.mClock = clock;
  }

  /** Stop dequeuing requests until {@link #resume()} is called. */
  public void pause() {
    isRunning = false;
  }

  /**
   * Resume dequeuing requests.
   *
   * <p>Note: a request is immediately dequeued and the delegate's fetch() method is called using
   * the current thread.
   */
  public void resume() {
    isRunning = true;
    dequeueIfAvailableSlots();
  }

  @Override
  public void fetch(
      final PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState,
      final Callback callback) {
    fetchState
        .getContext()
        .addCallbacks(
            new BaseProducerContextCallbacks() {
              @Override
              public void onCancellationRequested() {
                if (doNotCancelRequests) {
                  return;
                }
                if (!inflightFetchesCanBeCancelled && mCurrentlyFetching.contains(fetchState)) {
                  return;
                }
                removeFromQueue(fetchState, "CANCEL");
                callback.onCancellation();
              }

              @Override
              public void onPriorityChanged() {
                changePriority(fetchState, fetchState.getContext().getPriority() == HIGH);
              }
            });

    synchronized (mLock) {
      if (mCurrentlyFetching.contains(fetchState)) {
        FLog.e(TAG, "fetch state was enqueued twice: " + fetchState);
        return;
      }

      boolean isHiPri = fetchState.getContext().getPriority() == HIGH;
      FLog.v(TAG, "enqueue: %s %s", isHiPri ? "HI-PRI" : "LOW-PRI", fetchState.getUri());
      fetchState.callback = callback;
      putInQueue(fetchState, isHiPri);
    }
    dequeueIfAvailableSlots();
  }

  @Override
  public void onFetchCompletion(
      PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState, int byteSize) {
    removeFromQueue(fetchState, "SUCCESS");
    mDelegate.onFetchCompletion(fetchState.delegatedState, byteSize);
  }

  private void removeFromQueue(
      PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState, String reasonForLogging) {
    synchronized (mLock) {
      FLog.v(TAG, "remove: %s %s", reasonForLogging, fetchState.getUri());
      mCurrentlyFetching.remove(fetchState);
      if (!mHiPriQueue.remove(fetchState)) {
        mLowPriQueue.remove(fetchState);
      }
    }
    dequeueIfAvailableSlots();
  }

  private void moveDelayedRequestsToPriorityQueues() {
    if (mDelayedQueue.isEmpty()
        || mClock.now() - firstDelayedRequestEnqueuedTimeStamp <= requeueDelayTimeInMillis) {
      return;
    }
    for (PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState : mDelayedQueue) {
      putInQueue(fetchState, fetchState.getContext().getPriority() == HIGH);
    }
    mDelayedQueue.clear();
  }

  private void putInDelayedQueue(
      PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState) {
    if (mDelayedQueue.isEmpty()) {
      firstDelayedRequestEnqueuedTimeStamp = mClock.now();
    }
    fetchState.delayCount++;
    mDelayedQueue.addLast(fetchState);
  }

  private void requeue(PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState) {
    synchronized (mLock) {
      FLog.v(TAG, "requeue: %s", fetchState.getUri());

      fetchState.requeueCount++;
      fetchState.delegatedState =
          mDelegate.createFetchState(fetchState.getConsumer(), fetchState.getContext());

      mCurrentlyFetching.remove(fetchState);
      if (!mHiPriQueue.remove(fetchState)) {
        mLowPriQueue.remove(fetchState);
      }

      if (immediateRequeueCount != NO_DELAYED_REQUESTS
          && fetchState.requeueCount > immediateRequeueCount) {
        putInDelayedQueue(fetchState);
      } else {
        putInQueue(fetchState, fetchState.getContext().getPriority() == HIGH);
      }
    }
    dequeueIfAvailableSlots();
  }

  private void dequeueIfAvailableSlots() {
    if (!isRunning) {
      return;
    }

    PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> toFetch = null;
    synchronized (mLock) {
      moveDelayedRequestsToPriorityQueues();
      int outstandingRequests = mCurrentlyFetching.size();

      if (outstandingRequests < mMaxOutstandingHiPri) {
        toFetch = mHiPriQueue.pollFirst();
      }

      if (toFetch == null && outstandingRequests < mMaxOutstandingLowPri) {
        toFetch = mLowPriQueue.pollFirst();
      }
      if (toFetch == null) {
        return;
      }
      toFetch.dequeuedTimestamp = mClock.now();
      mCurrentlyFetching.add(toFetch);

      FLog.v(
          TAG,
          "fetching: %s (concurrent: %s hi-pri queue: %s low-pri queue: %s)",
          toFetch.getUri(),
          outstandingRequests,
          mHiPriQueue.size(),
          mLowPriQueue.size());
    }

    delegateFetch(toFetch);

    if (multipleDequeue) {
      dequeueIfAvailableSlots();
    }
  }

  private void delegateFetch(
      final PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState) {
    try {
      NetworkFetcher.Callback callbackWrapper =
          new NetworkFetcher.Callback() {
            @Override
            public void onResponse(InputStream response, int responseLength) throws IOException {
              fetchState.callback.onResponse(response, responseLength);
            }

            @Override
            public void onFailure(Throwable throwable) {
              final boolean shouldRequeue =
                  maxNumberOfRequeue == INFINITE_REQUEUE
                      || fetchState.requeueCount < maxNumberOfRequeue;
              if (shouldRequeue
                  && !(throwable instanceof PriorityNetworkFetcher.NonrecoverableException)) {
                requeue(fetchState);
              } else {
                removeFromQueue(fetchState, "FAIL");
                fetchState.callback.onFailure(throwable);
              }
            }

            @Override
            public void onCancellation() {
              removeFromQueue(fetchState, "CANCEL");
              fetchState.callback.onCancellation();
            }
          };
      mDelegate.fetch(fetchState.delegatedState, callbackWrapper);
    } catch (Exception e) {
      removeFromQueue(fetchState, "FAIL");
    }
  }

  private void changePriority(
      PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState, boolean isNewHiPri) {
    synchronized (mLock) {
      boolean existed =
          isNewHiPri ? mLowPriQueue.remove(fetchState) : mHiPriQueue.remove(fetchState);
      if (!existed) {
        return;
      }

      FLog.v(TAG, "change-pri: %s %s", isNewHiPri ? "HIPRI" : "LOWPRI", fetchState.getUri());

      fetchState.priorityChangedCount++;
      putInQueue(fetchState, isNewHiPri);
    }
    dequeueIfAvailableSlots();
  }

  private void putInQueue(
      PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> entry, boolean isHiPri) {
    if (isHiPri) {
      if (mIsHiPriFifo) {
        mHiPriQueue.addLast(entry);
      } else {
        mHiPriQueue.addFirst(entry);
      }
    } else {
      mLowPriQueue.addLast(entry);
    }
  }

  @VisibleForTesting
  List<PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE>> getHiPriQueue() {
    return mHiPriQueue;
  }

  @VisibleForTesting
  List<PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE>> getLowPriQueue() {
    return mLowPriQueue;
  }

  @VisibleForTesting
  List<PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE>> getDelayedQeueue() {
    return mDelayedQueue;
  }

  @VisibleForTesting
  HashSet<PriorityFetchState<FETCH_STATE>> getCurrentlyFetching() {
    return mCurrentlyFetching;
  }

  public static class PriorityFetchState<FETCH_STATE extends FetchState> extends FetchState {
    public FETCH_STATE delegatedState;
    final long enqueuedTimestamp;

    /** Size of hi-pri queue when this request was added. */
    final int hiPriCountWhenCreated;

    /** Size of low-pri queue when this request was added. */
    final int lowPriCountWhenCreated;

    /** Size of currentlyFetching queue when this request was added. */
    final int currentlyFetchingCountWhenCreated;

    @Nullable NetworkFetcher.Callback callback;
    long dequeuedTimestamp;
    int requeueCount = 0;

    /** number of times the request was delayed (inserted to the delay queue) */
    int delayCount = 0;

    /** the number of times the request's priority was changed while it was waiting the queue */
    int priorityChangedCount = 0;

    /** True if image request priority was high when it was started */
    final boolean isInitialPriorityHigh;

    private PriorityFetchState(
        Consumer<EncodedImage> consumer,
        ProducerContext producerContext,
        FETCH_STATE delegatedState,
        long enqueuedTimestamp,
        int hiPriCountWhenCreated,
        int lowPriCountWhenCreated,
        int currentlyFetchingCountWhenCreated) {
      super(consumer, producerContext);
      this.delegatedState = delegatedState;
      this.enqueuedTimestamp = enqueuedTimestamp;
      this.hiPriCountWhenCreated = hiPriCountWhenCreated;
      this.lowPriCountWhenCreated = lowPriCountWhenCreated;
      this.isInitialPriorityHigh = producerContext.getPriority() == HIGH;
      this.currentlyFetchingCountWhenCreated = currentlyFetchingCountWhenCreated;
    }
  }

  /**
   * The delegate fetcher may pass an instance of this exception to its callback's onFailure to
   * signal to a PriorityNetworkFetcher that it shouldn't retry that request.
   *
   * <p>This is useful for e.g., requests that fail due to HTTP 403: there's no point in retrying
   * them, usually.
   */
  public static class NonrecoverableException extends Throwable {
    public NonrecoverableException(@androidx.annotation.Nullable String message) {
      super(message);
    }
  }

  @Override
  public PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> createFetchState(
      Consumer<EncodedImage> consumer, ProducerContext producerContext) {
    return new PriorityFetchState<>(
        consumer,
        producerContext,
        mDelegate.createFetchState(consumer, producerContext),
        mClock.now(),
        mHiPriQueue.size(),
        mLowPriQueue.size(),
        mCurrentlyFetching.size());
  }

  @Override
  public boolean shouldPropagate(
      PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState) {
    return mDelegate.shouldPropagate(fetchState.delegatedState);
  }

  @Nullable
  @Override
  public Map<String, String> getExtraMap(
      PriorityNetworkFetcher.PriorityFetchState<FETCH_STATE> fetchState, int byteSize) {
    Map<String, String> delegateExtras = mDelegate.getExtraMap(fetchState.delegatedState, byteSize);
    HashMap<String, String> extras =
        delegateExtras != null ? new HashMap<>(delegateExtras) : new HashMap<String, String>();
    extras.put(
        "pri_queue_time", "" + (fetchState.dequeuedTimestamp - fetchState.enqueuedTimestamp));
    extras.put("hipri_queue_size", "" + fetchState.hiPriCountWhenCreated);
    extras.put("lowpri_queue_size", "" + fetchState.lowPriCountWhenCreated);
    extras.put("requeueCount", "" + fetchState.requeueCount);
    extras.put("priority_changed_count", "" + fetchState.priorityChangedCount);
    extras.put("request_initial_priority_is_high", "" + fetchState.isInitialPriorityHigh);
    extras.put("currently_fetching_size", "" + fetchState.currentlyFetchingCountWhenCreated);
    extras.put("delay_count", "" + fetchState.delayCount);

    return extras;
  }
}
