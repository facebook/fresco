/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;

/**
 * Component that defers {@code release} until after the main Looper has completed its current
 * message. Although we would like for defer {@code release} to happen immediately after the current
 * message is done, this is not guaranteed as there might be other messages after the current one,
 * but before the deferred one, pending in the Looper's queue.
 *
 * <p>onDetach / onAttach events are used for releasing / acquiring resources. However, sometimes we
 * get an onDetach event followed by an onAttach event within the same loop. In order to avoid
 * overaggressive resource releasing / acquiring, we defer releasing. If onAttach happens within the
 * same loop, we will simply cancel corresponding deferred release, avoiding an unnecessary resource
 * release / acquire cycle. If onAttach doesn't happen before the deferred message gets executed,
 * the resources will be released.
 *
 * <p>This class could be called from BG thread, but it has one restriction: releasables scheduled
 * from BG thread can only be un-schedule from BG thread (Enough for Litho BG binding).
 */
public class DeferredReleaserConcurrentImpl extends DeferredReleaser {

  private final Object mLock = new Object();
  private final ArrayList<Releasable> mPendingReleasables;
  private final Handler mUiHandler;

  public DeferredReleaserConcurrentImpl() {
    mPendingReleasables = new ArrayList<>();
    mUiHandler = new Handler(Looper.getMainLooper());
  }

  /*
   * Walks through the set of pending releasables, and calls release on them.
   * Resets the pending list to an empty list when done.
   */
  private final Runnable releaseRunnable =
      new Runnable() {
        Releasable[] mTempReleasables; // cache to avoid re-allocation

        @Override
        public void run() {
          int pendingSize;
          synchronized (mLock) {
            pendingSize = mPendingReleasables.size();
            if (mTempReleasables == null || mTempReleasables.length < pendingSize) {
              mTempReleasables = new Releasable[Math.max(pendingSize, 8)];
            }

            int i = 0;
            for (Releasable pendingReleasable : mPendingReleasables) {
              mTempReleasables[i++] = pendingReleasable;
            }
            mPendingReleasables.clear();
          }

          for (int i = 0; i < pendingSize; i++) {
            mTempReleasables[i].release();
            mTempReleasables[i] = null;
          }
        }
      };

  /**
   * Schedules deferred release.
   *
   * <p>The object will be released after the current Looper's loop, unless {@code
   * cancelDeferredRelease} is called before then.
   *
   * @param releasable Object to release.
   */
  @Override
  public void scheduleDeferredRelease(Releasable releasable) {
    boolean shouldSchedule;
    synchronized (mLock) {
      if (mPendingReleasables.contains(releasable)) {
        return;
      }
      mPendingReleasables.add(releasable);
      shouldSchedule = mPendingReleasables.size() == 1;
    }

    // If pending is not empty, there's already something scheduled (it might even already being
    // run) but we are sure the lock protected section hasn't enter, so this schedule will be
    // executed.
    if (shouldSchedule) {
      mUiHandler.post(releaseRunnable);
    }
  }

  /**
   * Cancels a pending release for this object.
   *
   * @param releasable Object to cancel release of.
   */
  @Override
  public void cancelDeferredRelease(Releasable releasable) {
    synchronized (mLock) {
      mPendingReleasables.remove(releasable);
    }
  }
}
