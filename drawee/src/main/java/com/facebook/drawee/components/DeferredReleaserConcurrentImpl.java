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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * <p>This class is thread-safe.
 */
public class DeferredReleaserConcurrentImpl extends DeferredReleaser {

  private final Object mLock = new Object();
  private final Set<Releasable> mPendingReleasables;
  private final Handler mUiHandler;

  public DeferredReleaserConcurrentImpl() {
    mPendingReleasables = new HashSet<>();
    mUiHandler = new Handler(Looper.getMainLooper());
  }

  /*
   * Walks through the set of pending releasables, and calls release on them.
   * Resets the pending list to an empty list when done.
   */
  private final Runnable releaseRunnable =
      new Runnable() {
        @Override
        public void run() {
          List<Releasable> releasables;
          synchronized (mLock) {
            releasables = new ArrayList<>(mPendingReleasables);
            mPendingReleasables.clear();
          }

          for (Releasable releasable : releasables) {
            releasable.release();
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
    boolean wasPendingEmpty;
    synchronized (mLock) {
      wasPendingEmpty = mPendingReleasables.isEmpty();

      if (!mPendingReleasables.add(releasable)) {
        return;
      }
    }
    // If pending is not empty, there's already something scheduled (it might even already being
    // run) but we are sure the lock protected section hasn't enter, so this schedule will be
    // executed.
    if (wasPendingEmpty) {
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
    // there's no way we could un-schedule, don't access handler in locked section and still make
    // this thread-safe
  }
}
