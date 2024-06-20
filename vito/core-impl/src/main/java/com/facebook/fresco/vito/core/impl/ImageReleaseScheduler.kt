/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.os.Handler
import android.os.Looper
import com.facebook.drawee.components.DeferredReleaser

object ImageReleaseScheduler {
  var releaseDelayMs: Long = 16 * 5 // Roughly 5 frames.

  var improveDelayedReleasing = false
  var enableReleaseDelayed = true
  var enableReleaseNextFrame = true
  var enableReleaseImmediately = true

  class ImageReleaseState(val drawable: KFrescoVitoDrawable) :
      Runnable, DeferredReleaser.Releasable {

    var delayedReleasePending = false

    override fun run() {
      // the Runnable interface is used to release next frame
      releaseNextFrame(drawable)
    }

    override fun release() {
      // The releasable is used to clean up immediately
      drawable.reset()
    }
  }

  private val handler = Handler(Looper.getMainLooper())
  private val deferredReleaser = DeferredReleaser.getInstance()

  fun releaseImmediately(drawable: KFrescoVitoDrawable) {
    if (!enableReleaseImmediately) {
      return
    }
    drawable.imagePerfListener.onReleaseImmediately(drawable)
    drawable.reset()
  }

  fun releaseDelayed(drawable: KFrescoVitoDrawable) {
    if (!enableReleaseDelayed || drawable.releaseState.delayedReleasePending) {
      return
    }
    drawable.imagePerfListener.onScheduleReleaseDelayed(drawable)
    handler.postDelayed(drawable.releaseState, releaseDelayMs)
    if (improveDelayedReleasing) {
      drawable.releaseState.delayedReleasePending = true
    }
  }

  fun releaseNextFrame(drawable: KFrescoVitoDrawable) {
    if (!enableReleaseNextFrame) {
      return
    }
    cancelReleaseDelayed(drawable)
    drawable.imagePerfListener.onScheduleReleaseNextFrame(drawable)
    deferredReleaser.scheduleDeferredRelease(drawable.releaseState)
  }

  fun cancelAllReleasing(drawable: KFrescoVitoDrawable) {
    cancelReleaseDelayed(drawable)
    cancelReleaseNextFrame(drawable)
  }

  fun cancelReleaseDelayed(drawable: KFrescoVitoDrawable) {
    if (!improveDelayedReleasing || drawable.releaseState.delayedReleasePending) {
      handler.removeCallbacks(drawable.releaseState)
    }
    drawable.releaseState.delayedReleasePending = false
  }

  fun cancelReleaseNextFrame(drawable: KFrescoVitoDrawable) {
    deferredReleaser.cancelDeferredRelease(drawable.releaseState)
  }

  fun createReleaseState(drawable: KFrescoVitoDrawable): ImageReleaseState =
      ImageReleaseState(drawable)
}
