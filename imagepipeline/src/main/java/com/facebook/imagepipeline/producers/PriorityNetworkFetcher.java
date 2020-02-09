/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static com.facebook.imagepipeline.common.Priority.HIGH;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.image.EncodedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
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
    implements NetworkFetcher<FETCH_STATE> {
  public static final String TAG = PriorityNetworkFetcher.class.getSimpleName();

  private final NetworkFetcher<FETCH_STATE> mDelegate;

  private final boolean mIsHiPriFifo;
  private final int mMaxOutstandingHiPri;
  private final int mMaxOutstandingLowPri;

  private final Object mLock = new Object();
  private final LinkedList<Entry<FETCH_STATE>> mHiPriQueue = new LinkedList<>();
  private final LinkedList<Entry<FETCH_STATE>> mLowPriQueue = new LinkedList<>();
  private final HashSet<FetchState> mCurrentlyFetching = new HashSet<>();

  /**
   * @param isHiPriFifo if true, hi-pri requests are dequeued in the order they were enqueued.
   *     Otherwise, they're dequeued in reverse order.
   */
  public PriorityNetworkFetcher(
      NetworkFetcher<FETCH_STATE> delegate,
      boolean isHiPriFifo,
      int maxOutstandingHiPri,
      int maxOutstandingLowPri) {
    mDelegate = delegate;
    mIsHiPriFifo = isHiPriFifo;

    this.mMaxOutstandingHiPri = maxOutstandingHiPri;
    this.mMaxOutstandingLowPri = maxOutstandingLowPri;
    if (maxOutstandingHiPri <= maxOutstandingLowPri) {
      throw new IllegalArgumentException("maxOutstandingHiPri should be > maxOutstandingLowPri");
    }
  }

  @Override
  public void fetch(final FETCH_STATE fetchState, final Callback callback) {
    fetchState
        .getContext()
        .addCallbacks(
            new BaseProducerContextCallbacks() {
              @Override
              public void onCancellationRequested() {
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
      putInQueue(new Entry<>(fetchState, callback), isHiPri);
    }
    dequeueIfAvailableSlots();
  }

  @Override
  public void onFetchCompletion(FETCH_STATE fetchState, int byteSize) {
    removeFromQueue(fetchState, "SUCCESS");
    mDelegate.onFetchCompletion(fetchState, byteSize);
  }

  private void removeFromQueue(FETCH_STATE fetchState, String reasonForLogging) {
    synchronized (mLock) {
      FLog.v(TAG, "remove: %s %s", reasonForLogging, fetchState.getUri());
      mCurrentlyFetching.remove(fetchState);
      if (findAndRemoveFetchState(mHiPriQueue, fetchState) == null) {
        findAndRemoveFetchState(mLowPriQueue, fetchState);
      }
    }
    dequeueIfAvailableSlots();
  }

  @Nullable
  private Entry<FETCH_STATE> findAndRemoveFetchState(
      List<Entry<FETCH_STATE>> queue, FETCH_STATE fetchState) {
    Iterator<Entry<FETCH_STATE>> i = queue.iterator();
    while (i.hasNext()) {
      Entry<FETCH_STATE> e = i.next();
      if (e.fetchState.equals(fetchState)) {
        i.remove();
        return e;
      }
    }
    return null;
  }

  private void dequeueIfAvailableSlots() {
    Entry<FETCH_STATE> toFetch = null;
    synchronized (mLock) {
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
      mCurrentlyFetching.add(toFetch.fetchState);

      FLog.v(
          TAG,
          "fetching: %s (concurrent: %s hi-pri queue: %s low-pri queue: %s)",
          toFetch.fetchState.getUri(),
          outstandingRequests,
          mHiPriQueue.size(),
          mLowPriQueue.size());
    }

    delegateFetch(toFetch.fetchState, toFetch.callback);
  }

  private void delegateFetch(final FETCH_STATE fetchState, final NetworkFetcher.Callback callback) {
    try {
      NetworkFetcher.Callback callbackWrapper =
          new NetworkFetcher.Callback() {
            @Override
            public void onResponse(InputStream response, int responseLength) throws IOException {
              callback.onResponse(response, responseLength);
            }

            @Override
            public void onFailure(Throwable throwable) {
              removeFromQueue(fetchState, "FAIL");
              callback.onFailure(throwable);
            }

            @Override
            public void onCancellation() {
              removeFromQueue(fetchState, "CANCEL");
              callback.onCancellation();
            }
          };
      mDelegate.fetch(fetchState, callbackWrapper);
    } catch (Exception e) {
      removeFromQueue(fetchState, "FAIL");
    }
  }

  private void changePriority(FETCH_STATE fetchState, boolean isNewHiPri) {
    synchronized (mLock) {
      Entry<FETCH_STATE> entry =
          isNewHiPri
              ? findAndRemoveFetchState(mLowPriQueue, fetchState)
              : findAndRemoveFetchState(mHiPriQueue, fetchState);
      if (entry == null) {
        return;
      }

      FLog.v(TAG, "change-pri: %s %s", isNewHiPri ? "HIPRI" : "LOWPRI", fetchState.getUri());

      putInQueue(entry, isNewHiPri);
    }
    dequeueIfAvailableSlots();
  }

  private void putInQueue(Entry<FETCH_STATE> entry, boolean isHiPri) {
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
  List<Entry<FETCH_STATE>> getHiPriQueue() {
    return mHiPriQueue;
  }

  @VisibleForTesting
  List<Entry<FETCH_STATE>> getLowPriQueue() {
    return mLowPriQueue;
  }

  @VisibleForTesting
  HashSet<FetchState> getCurrentlyFetching() {
    return mCurrentlyFetching;
  }

  @VisibleForTesting
  static class Entry<FETCH_STATE extends FetchState> {
    final FETCH_STATE fetchState;
    final NetworkFetcher.Callback callback;

    @VisibleForTesting
    Entry(FETCH_STATE fetchState, NetworkFetcher.Callback callback) {
      this.fetchState = fetchState;
      this.callback = callback;
    }
  }

  @Override
  public FETCH_STATE createFetchState(
      Consumer<EncodedImage> consumer, ProducerContext producerContext) {
    return mDelegate.createFetchState(consumer, producerContext);
  }

  @Override
  public boolean shouldPropagate(FETCH_STATE fetchState) {
    return mDelegate.shouldPropagate(fetchState);
  }

  @Nullable
  @Override
  public Map<String, String> getExtraMap(FETCH_STATE fetchState, int byteSize) {
    return mDelegate.getExtraMap(fetchState, byteSize);
  }
}
