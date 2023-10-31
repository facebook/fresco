/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.frame

import androidx.annotation.VisibleForTesting
import com.facebook.fresco.animation.backend.AnimationInformation

/** Frame scheduler that maps time values to frames. */
class DropFramesFrameScheduler(private val animationInformation: AnimationInformation) :
    FrameScheduler {

  private var _loopDurationMs = UNSET.toLong()

  override fun getFrameNumberToRender(animationTimeMs: Long, lastFrameTimeMs: Long): Int {
    val loopDurationMs = this.loopDurationMs
    if (loopDurationMs == 0L) {
      return getFrameNumberWithinLoop(0)
    }
    if (!isInfiniteAnimation) {
      val loopCount = animationTimeMs / loopDurationMs
      if (loopCount >= animationInformation.loopCount) {
        return FrameScheduler.FRAME_NUMBER_DONE
      }
    }
    val timeInCurrentLoopMs = animationTimeMs % loopDurationMs
    return getFrameNumberWithinLoop(timeInCurrentLoopMs)
  }

  override fun getLoopDurationMs(): Long {
    if (_loopDurationMs != UNSET.toLong()) {
      return _loopDurationMs
    }
    _loopDurationMs = 0
    val frameCount = animationInformation.frameCount
    for (i in 0 until frameCount) {
      _loopDurationMs += animationInformation.getFrameDurationMs(i).toLong()
    }
    return _loopDurationMs
  }

  override fun getTargetRenderTimeMs(frameNumber: Int): Long {
    var targetRenderTimeMs = 0L
    for (i in 0 until frameNumber) {
      targetRenderTimeMs += animationInformation.getFrameDurationMs(frameNumber).toLong()
    }
    return targetRenderTimeMs
  }

  override fun getTargetRenderTimeForNextFrameMs(animationTimeMs: Long): Long {
    val loopDurationMs = this.loopDurationMs
    // Sanity check.
    if (loopDurationMs == 0L) {
      return FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong()
    }
    if (!isInfiniteAnimation) {
      val loopCount = animationTimeMs / loopDurationMs
      if (loopCount >= animationInformation.loopCount) {
        return FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong()
      }
    }
    // The animation time in the current loop
    val timePassedInCurrentLoopMs = animationTimeMs % loopDurationMs
    // The animation time in the current loop for the next frame
    var timeOfNextFrameInLoopMs = 0L
    val frameCount = animationInformation.frameCount
    var i = 0
    while (i < frameCount && timeOfNextFrameInLoopMs <= timePassedInCurrentLoopMs) {
      timeOfNextFrameInLoopMs += animationInformation.getFrameDurationMs(i).toLong()
      i++
    }

    // Difference between current time in loop and next frame in loop
    val timeUntilNextFrameInLoopMs = timeOfNextFrameInLoopMs - timePassedInCurrentLoopMs
    // Add the difference to the current animation time
    return animationTimeMs + timeUntilNextFrameInLoopMs
  }

  override fun isInfiniteAnimation(): Boolean =
      animationInformation.loopCount == AnimationInformation.LOOP_COUNT_INFINITE

  @VisibleForTesting
  fun getFrameNumberWithinLoop(timeInCurrentLoopMs: Long): Int {
    var frame = 0
    var currentDuration = 0L
    do {
      currentDuration += animationInformation.getFrameDurationMs(frame).toLong()
      frame++
    } while (timeInCurrentLoopMs >= currentDuration)
    return frame - 1
  }

  companion object {
    private const val UNSET = -1
  }
}
