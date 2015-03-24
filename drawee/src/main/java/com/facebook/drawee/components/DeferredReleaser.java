/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.components;

import java.util.HashSet;
import java.util.Set;

import android.os.Handler;
import android.os.Looper;

/**
 * Component that defers {@code release} until after the main Looper has completed its current
 * message. Although we would like for defer {@code release} to happen immediately after the current
 * message is done, this is not guaranteed as there might be other messages after the current one,
 * but before the deferred one, pending in the Looper's queue.
 * <p>
 * onDetach / onAttach events are used for releasing / acquiring resources. However, sometimes we
 * get an onDetach event followed by an onAttach event within the same loop. In order to avoid
 * overaggressive resource releasing / acquiring, we defer releasing. If onAttach happens within
 * the same loop, we will simply cancel corresponding deferred release, avoiding an unnecessary
 * resource release / acquire cycle. If onAttach doesn't happen before the deferred message gets
 * executed, the resources will be released.
 * <p>
 * This class is not thread-safe and should only be used from the main thread (UI thread).
 */
public class DeferredReleaser {

  private static DeferredReleaser sInstance = null;

  public static synchronized DeferredReleaser getInstance() {
    if (sInstance == null) {
      sInstance = new DeferredReleaser();
    }
    return sInstance;
  }

  public interface Releasable {
    public void release();
  }

  private final Set<Releasable> mPendingReleasables;
  private final Handler mUiHandler;

  public DeferredReleaser() {
    mPendingReleasables =  new HashSet<Releasable>();
    mUiHandler = new Handler(Looper.getMainLooper());
  }

  /*
   * Walks through the set of pending releasables, and calls release on them.
   * Resets the pending list to an empty list when done.
   */
  private final Runnable releaseRunnable = new Runnable() {
    @Override
    public void run() {
      for (Releasable releasable : mPendingReleasables) {
        releasable.release();
      }
      mPendingReleasables.clear();
    }
  };

  /**
   * Schedules deferred release.
   * <p>
   * The object will be released after the current Looper's loop,
   * unless {@code cancelDeferredRelease} is called before then.
   * @param releasable Object to release.
   */
  public void scheduleDeferredRelease(Releasable releasable) {
    if (!mPendingReleasables.add(releasable)) {
      return;
    }
    // Posting to the UI queue is an O(n) operation, so we only do it once.
    // The one runnable does all the releases.
    if (mPendingReleasables.size() == 1) {
      mUiHandler.post(releaseRunnable);
    }
  }

  /**
   * Cancels a pending release for this object.
   * @param releasable Object to cancel release of.
   */
  public void cancelDeferredRelease(Releasable releasable) {
    mPendingReleasables.remove(releasable);
  }

}
