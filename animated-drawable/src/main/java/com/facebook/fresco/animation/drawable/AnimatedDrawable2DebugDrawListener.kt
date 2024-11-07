/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

import com.facebook.common.logging.FLog
import com.facebook.fresco.animation.frame.FrameScheduler

/**
 * [com.facebook.fresco.animation.drawable.AnimatedDrawable2.DrawListener] for debugging
 * [AnimatedDrawable2].
 */
class AnimatedDrawable2DebugDrawListener : AnimatedDrawable2.DrawListener {

  private var lastFrameNumber = -1
  private var skippedFrames = 0
  private var duplicateFrames = 0
  private var drawCalls = 0

  override fun onDraw(
      animatedDrawable: AnimatedDrawable2,
      frameScheduler: FrameScheduler,
      frameNumberToDraw: Int,
      frameDrawn: Boolean,
      isAnimationRunning: Boolean,
      animationStartTimeMs: Long,
      animationTimeMs: Long,
      lastFrameAnimationTimeMs: Long,
      actualRenderTimeStartMs: Long,
      actualRenderTimeEndMs: Long,
      startRenderTimeForNextFrameMs: Long,
      scheduledRenderTimeForNextFrameMs: Long
  ) {
    val frameCount = animatedDrawable.animationBackend?.getFrameCount() ?: return

    val animationTimeDifference = animationTimeMs - lastFrameAnimationTimeMs
    drawCalls++
    val expectedNextFrameNumber = (lastFrameNumber + 1) % frameCount
    if (expectedNextFrameNumber != frameNumberToDraw) {
      // something went wrong...
      if (lastFrameNumber == frameNumberToDraw) {
        duplicateFrames++
      } else {
        var skippedFrameCount = (frameNumberToDraw - expectedNextFrameNumber) % frameCount
        if (skippedFrameCount < 0) {
          skippedFrameCount += frameCount
        }
        skippedFrames += skippedFrameCount
      }
    }
    lastFrameNumber = frameNumberToDraw
    FLog.d(
        TAG,
        ("draw: frame: %2d, drawn: %b, delay: %3d ms, rendering: %3d ms, prev: %3d ms ago, duplicates: %3d, skipped: %3d, draw calls: %4d, anim time: %6d ms, next start: %6d ms, next scheduled: %6d ms"),
        frameNumberToDraw,
        frameDrawn,
        animationTimeMs % frameScheduler.loopDurationMs -
            frameScheduler.getTargetRenderTimeMs(frameNumberToDraw),
        actualRenderTimeEndMs - actualRenderTimeStartMs,
        animationTimeDifference,
        duplicateFrames,
        skippedFrames,
        drawCalls,
        animationTimeMs,
        startRenderTimeForNextFrameMs,
        scheduledRenderTimeForNextFrameMs)
  }

  companion object {
    private val TAG: Class<*> = AnimatedDrawable2DebugDrawListener::class.java
  }
}
