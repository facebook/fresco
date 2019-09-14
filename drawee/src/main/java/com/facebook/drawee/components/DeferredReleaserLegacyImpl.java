/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components;

import android.os.Handler;
import android.os.Looper;
import com.facebook.common.internal.Preconditions;
import java.util.HashSet;
import java.util.Set;

class DeferredReleaserLegacyImpl extends DeferredReleaser {

  protected final Set<Releasable> mPendingReleasables;
  protected final Handler mUiHandler;
  /*
   * Walks through the set of pending releasables, and calls release on them.
   * Resets the pending list to an empty list when done.
   */
  private final Runnable releaseRunnable =
      new Runnable() {
        @Override
        public void run() {
          ensureOnUiThread();
          for (Releasable releasable : mPendingReleasables) {
            releasable.release();
          }
          mPendingReleasables.clear();
        }
      };

  public DeferredReleaserLegacyImpl() {
    mPendingReleasables = new HashSet<>();
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
  public void scheduleDeferredRelease(Releasable releasable) {
    if (!isOnUiThread()) {
      releasable.release();
      return;
    }

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
   *
   * @param releasable Object to cancel release of.
   */
  public void cancelDeferredRelease(Releasable releasable) {
    // releasable from BG threads are released immediately
    if (!isOnUiThread()) return;

    mPendingReleasables.remove(releasable);
  }

  private static void ensureOnUiThread() {
    Preconditions.checkState(isOnUiThread());
  }
}
