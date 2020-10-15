/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;

@Nullsafe(Nullsafe.Mode.STRICT)
class DeferredReleaserConcurrentImpl extends DeferredReleaser {

  private final Object mLock = new Object();
  private final Handler mUiHandler;

  private ArrayList<Releasable> mPendingReleasables;
  private ArrayList<Releasable> mTempList;

  /*
   * Walks through the set of pending releasables, and calls release on them.
   * Resets the pending list to an empty list when done.
   */
  private final Runnable releaseRunnable =
      new Runnable() {

        @MainThread
        @Override
        public void run() {
          synchronized (mLock) {
            ArrayList<Releasable> tmp = mTempList;
            mTempList = mPendingReleasables;
            mPendingReleasables = tmp;
          }

          //noinspection ForLoopReplaceableByForEach
          for (int i = 0, size = mTempList.size(); i < size; i++) {
            mTempList.get(i).release();
          }
          mTempList.clear();
        }
      };

  public DeferredReleaserConcurrentImpl() {
    mPendingReleasables = new ArrayList<>();
    mTempList = new ArrayList<>();
    mUiHandler = new Handler(Looper.getMainLooper());
  }

  @AnyThread
  @Override
  public void scheduleDeferredRelease(Releasable releasable) {
    if (!isOnUiThread()) {
      releasable.release();
      return;
    }

    boolean shouldSchedule;
    synchronized (mLock) {
      if (mPendingReleasables.contains(releasable)) {
        return;
      }
      mPendingReleasables.add(releasable);
      shouldSchedule = mPendingReleasables.size() == 1;
    }

    // Posting to the UI queue is an O(n) operation, so we only do it once.
    // The one runnable does all the releases.
    if (shouldSchedule) {
      mUiHandler.post(releaseRunnable);
    }
  }

  @AnyThread
  @Override
  public void cancelDeferredRelease(Releasable releasable) {
    // it's possible an releasable is scheduled from FG thread and then reused in BG thread (common
    // in Litho lifecycle)
    synchronized (mLock) {
      mPendingReleasables.remove(releasable);
    }
  }
}
