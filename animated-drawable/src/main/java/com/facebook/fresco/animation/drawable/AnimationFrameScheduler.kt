/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

import android.os.SystemClock
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.frame.DropFramesFrameScheduler
import com.facebook.fresco.animation.frame.FrameScheduler
import kotlin.math.max

const val INVALID_FRAME_TIME = -1L
private const val DEFAULT_FRAME_SCHEDULING_DELAY_MS = 8L
private const val DEFAULT_FRAME_SCHEDULING_OFFSET_MS = 0L

class AnimationFrameScheduler(
    animationBackend: AnimationBackend,
    private val frameSchedulingDelayMillis: Long = DEFAULT_FRAME_SCHEDULING_DELAY_MS,
    private val frameSchedulingOffsetMillis: Long = DEFAULT_FRAME_SCHEDULING_OFFSET_MS,
) {

  var running: Boolean = false
  private val frameScheduler = DropFramesFrameScheduler(animationBackend)

  // Frame scheduling timer in milliseconds
  private var pauseTimeMillis = 0L
  private var startInMillis = 0L
  private var expectedRenderTimeMillis = 0L
  private var lastFrameAnimationTimeMillis = 0L
  private var lastFrameAnimationTimeDifferenceMillis = 0L

  // Frame management
  var lastDrawnFrameNumber = -1
  private var pausedLastDrawnFrameNumber = -1

  // Stats
  private var framesDropped = 0

  private fun now() = SystemClock.uptimeMillis()

  fun start() {
    if (!running) {
      val now = now()
      startInMillis = now - pauseTimeMillis
      expectedRenderTimeMillis = startInMillis
      lastFrameAnimationTimeMillis = now - lastFrameAnimationTimeDifferenceMillis
      lastDrawnFrameNumber = pausedLastDrawnFrameNumber
      running = true
    }
  }

  fun stop() {
    if (running) {
      val now = now()
      pauseTimeMillis = now - startInMillis
      lastFrameAnimationTimeDifferenceMillis = now - lastFrameAnimationTimeMillis
      startInMillis = 0L
      expectedRenderTimeMillis = 0L
      lastFrameAnimationTimeMillis = -1L
      lastDrawnFrameNumber = -1
      running = false
    }
  }

  fun frameToDraw(): Int {
    val renderTimeMillis = now()

    val animationTimeMillis =
        if (running) {
          renderTimeMillis - startInMillis + frameSchedulingOffsetMillis
        } else {
          max(lastFrameAnimationTimeMillis, 0)
        }

    // What frame should be drawn?
    val frameNumberToDraw =
        frameScheduler.getFrameNumberToRender(animationTimeMillis, lastFrameAnimationTimeMillis)

    lastFrameAnimationTimeMillis = animationTimeMillis

    return frameNumberToDraw
  }

  // Returns -1 if it's an valid next render time
  fun nextRenderTime(): Long {
    if (running) {
      val actualRenderTimeEnd = now()
      val targetRenderTimeForNextFrameMillis =
          frameScheduler.getTargetRenderTimeForNextFrameMs(actualRenderTimeEnd - startInMillis)
      if (targetRenderTimeForNextFrameMillis !=
          FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong()) {
        val nextFrameTime = targetRenderTimeForNextFrameMillis + frameSchedulingDelayMillis
        expectedRenderTimeMillis = startInMillis + nextFrameTime
        return nextFrameTime
      }
      running = false
    }
    return INVALID_FRAME_TIME
  }

  fun shouldRepeatAnimation(): Boolean {
    return lastDrawnFrameNumber != -1 && now() >= expectedRenderTimeMillis
  }

  fun onFrameDropped() {
    framesDropped++
    // TODO Add log info here
  }

  fun infinite(): Boolean = frameScheduler.isInfiniteAnimation

  fun loopDuration(): Long = frameScheduler.loopDurationMs
}
