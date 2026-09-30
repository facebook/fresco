/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components

import android.os.Looper

/**
 * Component that defers `release` until after the main Looper has completed its current message.
 * Although we would like for defer `release` to happen immediately after the current message is
 * done, this is not guaranteed as there might be other messages after the current one, but before
 * the deferred one, pending in the Looper's queue.
 *
 * onDetach / onAttach events are used for releasing / acquiring resources. However, sometimes we
 * get an onDetach event followed by an onAttach event within the same loop. In order to avoid
 * overaggressive resource releasing / acquiring, we defer releasing. If onAttach happens within the
 * same loop, we will simply cancel corresponding deferred release, avoiding an unnecessary resource
 * release / acquire cycle. If onAttach doesn't happen before the deferred message gets executed,
 * the resources will be released.
 */
abstract class DeferredReleaser {

  fun interface Releasable {
    fun release()
  }

  /**
   * Schedules deferred release.
   *
   * The object will be released after the current Looper's loop, unless `cancelDeferredRelease` is
   * called before then.
   *
   * @param releasable Object to release.
   */
  abstract fun scheduleDeferredRelease(releasable: Releasable)

  /**
   * Cancels a pending release for this object.
   *
   * @param releasable Object to cancel release of.
   */
  abstract fun cancelDeferredRelease(releasable: Releasable)

  companion object {
    private var _instance: DeferredReleaser? = null

    @JvmStatic
    @Synchronized
    fun getInstance(): DeferredReleaser {
      if (_instance == null) {
        _instance = DeferredReleaserConcurrentImpl()
      }
      return _instance!!
    }

    @get:JvmStatic
    val isOnUiThread: Boolean
      get() = Looper.getMainLooper().thread === Thread.currentThread()
  }
}
