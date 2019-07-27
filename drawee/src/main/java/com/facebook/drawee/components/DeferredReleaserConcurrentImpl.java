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
import java.util.Collections;
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
 * <p>This class could be called from BG thread, but it has one restriction: releasables scheduled
 * from BG thread can only be un-schedule from BG thread (Enough for Litho BG binding).
 */
public class DeferredReleaserConcurrentImpl extends DeferredReleaser {

  private final PendingSet mPendingSet = new PendingSet();
  private final PendingSetSync mPendingSetSync = new PendingSetSync();

  private final Runnable releaseRunnable = new ReleaseRunnable(mPendingSet);
  private final Runnable releaseRunnableSync = new ReleaseRunnable(mPendingSetSync);

  private final Handler mUiHandler;

  public DeferredReleaserConcurrentImpl() {
    mUiHandler = new Handler(Looper.getMainLooper());
  }

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
    PendingSet pendingSet;
    Runnable runnable;
    if (isOnUiThread()) {
      pendingSet = mPendingSet;
      runnable = releaseRunnable;
    } else {
      pendingSet = mPendingSetSync;
      runnable = releaseRunnableSync;
    }

    if (pendingSet.schedule(releasable)) mUiHandler.post(runnable);
  }

  /**
   * Cancels a pending release for this object.
   *
   * @param releasable Object to cancel release of.
   */
  @Override
  public void cancelDeferredRelease(Releasable releasable) {
    if (isOnUiThread()) {
      mPendingSet.cancel(releasable);
    } else {
      mPendingSetSync.cancel(releasable);
    }
  }

  static class PendingSet {
    final Set<Releasable> mPendingReleasables = new HashSet<>();

    /** returns if we need to schedule a runnable * */
    boolean schedule(Releasable releasable) {
      boolean wasPendingEmpty = mPendingReleasables.isEmpty();
      return mPendingReleasables.add(releasable) && wasPendingEmpty;
    }

    void cancel(Releasable releasable) {
      mPendingReleasables.remove(releasable);
    }

    List<Releasable> takeAll() {
      List<Releasable> result;
      if (mPendingReleasables.isEmpty()) {
        result = Collections.emptyList();
      } else {
        result = new ArrayList<>(mPendingReleasables);
      }
      mPendingReleasables.clear();
      return result;
    }
  }

  static class PendingSetSync extends PendingSet {

    @Override
    synchronized boolean schedule(Releasable releasable) {
      return super.schedule(releasable);
    }

    @Override
    synchronized void cancel(Releasable releasable) {
      super.cancel(releasable);
    }

    @Override
    synchronized List<Releasable> takeAll() {
      return super.takeAll();
    }
  }

  private static class ReleaseRunnable implements Runnable {
    final PendingSet mPendingSet;

    private ReleaseRunnable(PendingSet pendingSet) {
      mPendingSet = pendingSet;
    }

    @Override
    public void run() {
      for (Releasable releasable : mPendingSet.takeAll()) {
        releasable.release();
      }
    }
  }
}
