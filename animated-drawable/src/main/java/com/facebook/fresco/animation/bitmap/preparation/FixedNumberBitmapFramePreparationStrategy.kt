/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import com.facebook.common.logging.FLog
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapFrameCache

/** Frame preparation strategy to prepare the next n frames */
class FixedNumberBitmapFramePreparationStrategy
@JvmOverloads
constructor(private val framesToPrepare: Int = 3) : BitmapFramePreparationStrategy {

  private val TAG = FixedNumberBitmapFramePreparationStrategy::class.java

  override fun prepareFrames(
      bitmapFramePreparer: BitmapFramePreparer,
      bitmapFrameCache: BitmapFrameCache,
      animationBackend: AnimationBackend,
      lastDrawnFrameNumber: Int,
      onAnimationLoaded: (() -> Unit)?
  ) {
    for (i in 1..framesToPrepare) {
      val nextFrameNumber = (lastDrawnFrameNumber + i) % animationBackend.frameCount
      if (FLog.isLoggable(FLog.VERBOSE)) {
        FLog.v(TAG, "Preparing frame %d, last drawn: %d", nextFrameNumber, lastDrawnFrameNumber)
      }
      if (!bitmapFramePreparer.prepareFrame(bitmapFrameCache, animationBackend, nextFrameNumber)) {
        // We cannot prepare more frames, so we return early
        return
      }
    }

    onAnimationLoaded?.invoke()
  }
}
