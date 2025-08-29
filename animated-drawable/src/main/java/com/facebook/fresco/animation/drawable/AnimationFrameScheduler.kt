/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

import android.os.SystemClock
import com.facebook.fresco.animation.frame.FrameScheduler
import com.facebook.fresco.animation.frame.FrameScheduler.NO_NEXT_TARGET_RENDER_TIME
import kotlin.math.max

const val INVALID_FRAME_TIME = -1L
private const val DEFAULT_FRAME_SCHEDULING_DELAY_MS = 8L
private const val DEFAULT_FRAME_SCHEDULING_OFFSET_MS = 0L

class AnimationFrameScheduler(private val frameScheduler: FrameScheduler) {

  var running: Boolean = false

  // Frame scheduling timer in milliseconds
  var frameSchedulingDelayMs: Long = DEFAULT_FRAME_SCHEDULING_DELAY_MS
  var frameSchedulingOffsetMs: Long = DEFAULT_FRAME_SCHEDULING_OFFSET_MS
  private var pauseTimeMs = 0L
  private var startMs = 0L
  private var expectedRenderTimeMs = 0L
  private var lastFrameAnimationTimeMs = 0L
  private var lastFrameAnimationTimeDifferenceMs = 0L

  // Frame management
  var lastDrawnFrameNumber = -1
  private val pausedLastDrawnFrameNumber = -1

  // Stats
  private var framesDropped = 0

  private fun now() = SystemClock.uptimeMillis()

  fun start() {
    if (!running) {
      val now = now()
      startMs = now - pauseTimeMs
      expectedRenderTimeMs = startMs
      lastFrameAnimationTimeMs = now - lastFrameAnimationTimeDifferenceMs
      lastDrawnFrameNumber = pausedLastDrawnFrameNumber
      running = true
    }
  }

  fun stop() {
    if (running) {
      val now = now()
      pauseTimeMs = now - startMs
      lastFrameAnimationTimeDifferenceMs = now - lastFrameAnimationTimeMs
      startMs = 0L
      expectedRenderTimeMs = 0L
      lastFrameAnimationTimeMs = -1L
      lastDrawnFrameNumber = -1
      running = false
    }
  }

  fun frameToDraw(): Int {
    val renderTimeMillis = now()

    val animationTimeMillis =
        if (running) {
          renderTimeMillis - startMs + frameSchedulingOffsetMs
        } else {
          max(lastFrameAnimationTimeMs, 0)
        }

    // What frame should be drawn?
    val frameNumberToDraw =
        frameScheduler.getFrameNumberToRender(animationTimeMillis, lastFrameAnimationTimeMs)

    lastFrameAnimationTimeMs = animationTimeMillis

    return frameNumberToDraw
  }

  // Returns -1 if it's an valid next render time
  fun nextRenderTime(): Long {
    if (!running) {
      return INVALID_FRAME_TIME
    }

    val actualRenderTimeEnd = now()
    val targetRenderTimeForNextFrameMs =
        frameScheduler.getTargetRenderTimeForNextFrameMs(actualRenderTimeEnd - startMs)
    if (targetRenderTimeForNextFrameMs != NO_NEXT_TARGET_RENDER_TIME.toLong()) {
      val nextFrameTime = targetRenderTimeForNextFrameMs + frameSchedulingDelayMs
      expectedRenderTimeMs = startMs + nextFrameTime
      return nextFrameTime
    }
    running = false

    return INVALID_FRAME_TIME
  }

  fun shouldRepeatAnimation(): Boolean {
    return lastDrawnFrameNumber != -1 && now() >= expectedRenderTimeMs
  }

  fun onFrameDropped() {
    framesDropped++
    // TODO Add log info here
  }

  fun infinite(): Boolean = frameScheduler.isInfiniteAnimation

  fun loopDuration(): Long = frameScheduler.loopDurationMs
}
