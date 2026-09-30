/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components

import android.os.Handler
import android.os.Looper
import androidx.annotation.AnyThread

internal class DeferredReleaserConcurrentImpl : DeferredReleaser() {

  private val lock = Any()
  private val uiHandler: Handler = Handler(Looper.getMainLooper())

  private var pendingReleasables: ArrayList<Releasable> = ArrayList()
  private var tempList: ArrayList<Releasable> = ArrayList()

  /*
   * Walks through the set of pending releasables, and calls release on them.
   * Resets the pending list to an empty list when done.
   */
  private val releaseRunnable = Runnable {
    synchronized(lock) {
      val tmp = tempList
      tempList = pendingReleasables
      pendingReleasables = tmp
    }
    var i = 0
    val size = tempList.size
    while (i < size) {
      tempList[i].release()
      i++
    }
    tempList.clear()
  }

  @AnyThread
  override fun scheduleDeferredRelease(releasable: Releasable) {
    if (!DeferredReleaser.isOnUiThread) {
      releasable.release()
      return
    }

    val shouldSchedule: Boolean
    synchronized(lock) {
      if (pendingReleasables.contains(releasable)) {
        return
      }
      pendingReleasables.add(releasable)
      shouldSchedule = pendingReleasables.size == 1
    }

    // Posting to the UI queue is an O(n) operation, so we only do it once.
    // The one runnable does all the releases.
    if (shouldSchedule) {
      uiHandler.post(releaseRunnable)
    }
  }

  @AnyThread
  override fun cancelDeferredRelease(releasable: Releasable) {
    // it's possible an releasable is scheduled from FG thread and then reused in BG thread (common
    // in Litho lifecycle)
    synchronized(lock) {
      pendingReleasables.remove(releasable)
    }
  }
}
