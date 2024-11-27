/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import com.facebook.drawable.base.DrawableWithCaches
import com.facebook.drawee.drawable.DrawableProperties
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.frame.DropFramesFrameScheduler
import com.facebook.fresco.animation.frame.FrameScheduler

open class KAnimatedDrawable2(private var animationBackend: AnimationBackend) :
    Drawable(), Animatable, DrawableWithCaches {

  private val animatedFrameScheduler =
      AnimationFrameScheduler(DropFramesFrameScheduler(animationBackend))
  private var animationListener: AnimationListener = BaseAnimationListener()
  private var drawListener: DrawListener? = null
  private val drawableProperties = DrawableProperties().apply { applyTo(this@KAnimatedDrawable2) }

  @Volatile private var isRunning = false

  /**
   * Runnable that invalidates the drawable that will be scheduled according to the next target
   * frame.
   */
  private val invalidateRunnable =
      object : Runnable {
        override fun run() {
          // Remove all potential other scheduled runnables
          // (e.g. if the view has been invalidated a lot)
          unscheduleSelf(this)
          // Draw the next frame
          invalidateSelf()
        }
      }

  override fun setAlpha(alpha: Int) {
    drawableProperties.setAlpha(alpha)
    animationBackend.setAlpha(alpha)
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    drawableProperties.setColorFilter(colorFilter)
    animationBackend.setColorFilter(colorFilter)
  }

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  /** Start the animation. */
  override fun start() {
    if (animationBackend.frameCount <= 0) {
      return
    }

    animatedFrameScheduler.start()
    animationListener.onAnimationStart(this)
    invalidateSelf()
  }

  /** Stop the animation at the current frame. It can be resumed by calling [start()] again. */
  override fun stop() {
    animatedFrameScheduler.stop()
    animationListener.onAnimationStop(this)
    unscheduleSelf(invalidateRunnable)
  }

  /**
   * Check whether the animation is running.
   *
   * @return true if the animation is currently running
   */
  override fun isRunning(): Boolean = animatedFrameScheduler.running

  override fun dropCaches() {
    animationBackend.clear()
  }

  override fun onBoundsChange(bounds: Rect) {
    animationBackend.setBounds(bounds)
  }

  override fun getIntrinsicWidth(): Int = animationBackend.intrinsicWidth

  override fun getIntrinsicHeight(): Int = animationBackend.intrinsicHeight

  /**
   * Get the animation duration for 1 loop by summing all frame durations.
   *
   * @return the duration of 1 animation loop in ms
   */
  fun loopDurationMs(): Int = animationBackend.loopDurationMs

  /**
   * Get the number of frames for the animation. If no animation backend is set, 0 will be returned.
   *
   * @return the number of frames of the animation
   */
  fun getFrameCount(): Int = animationBackend.frameCount

  /**
   * Get the loop count of the animation. The returned value is either
   * [AnimationInformation#LOOP_COUNT_INFINITE] if the animation is repeated infinitely or a
   * positive integer that corresponds to the number of loops. If no animation backend is set,
   * [AnimationInformation#LOOP_COUNT_INFINITE] will be returned.
   *
   * @return the loop count of the animation or [AnimationInformation#LOOP_COUNT_INFINITE]
   */
  fun loopCount(): Int = animationBackend.loopCount

  /**
   * Frame scheduling delay to shift the target render time for a frame within the frame's visible
   * window. If the value is set to 0, the frame will be scheduled right at the beginning of the
   * frame's visible window.
   *
   * @param delayMs the delay to use in ms
   */
  fun setFrameSchedulingDelayMs(delayMs: Long) {
    animatedFrameScheduler.frameSchedulingDelayMs = delayMs
  }

  /**
   * Frame scheduling offset to shift the animation time by the given offset. This is similar to
   * [frameSchedulingDelayMs] but instead of delaying the invalidation, this offsets the animation
   * time by the given value.
   *
   * @param offsetMs the offset to use in ms
   */
  fun setFrameSchedulingOffsetMs(offsetMs: Long) {
    animatedFrameScheduler.frameSchedulingOffsetMs = offsetMs
  }

  /**
   * Set an animation listener that is notified for various animation events.
   *
   * @param listener the listener to use
   */
  fun setAnimationListener(listener: AnimationListener?) {
    animationListener = listener ?: animationListener
  }

  /**
   * Set a draw listener that is notified for each [draw(Canvas)] call.
   *
   * @param listener the listener to use
   */
  fun setDrawListener(listener: DrawListener?) {
    drawListener = listener
  }

  /**
   * Update the animation backend to be used for the animation. This will also stop the animation.
   * In order to remove the current animation backend, call this method with null.
   *
   * @param animationBackend the animation backend to be used or null
   */
  fun setAnimationBackend(animationBackend: AnimationBackend?) {
    animationBackend ?: return
    stop()
    animationBackend.setBounds(bounds)
    drawableProperties.applyTo(this)
    this.animationBackend = animationBackend
  }

  override fun draw(canvas: Canvas) {
    var frameNumber = animatedFrameScheduler.frameToDraw()
    // Check if the animation is finished and draw last frame if so
    if (frameNumber == FrameScheduler.FRAME_NUMBER_DONE) {
      frameNumber = animationBackend.frameCount - 1
      animatedFrameScheduler.running = false
      animationListener.onAnimationStop(this)
    } else if (frameNumber == 0 && animatedFrameScheduler.shouldRepeatAnimation()) {
      animationListener.onAnimationRepeat(this)
    }

    val frameDrawn = animationBackend.drawFrame(this, canvas, frameNumber)
    if (frameDrawn) {
      // Notify listeners that we draw a new frame and
      // that the animation might be repeated
      animationListener.onAnimationFrame(this, frameNumber)
      animatedFrameScheduler.lastDrawnFrameNumber = frameNumber
    } else {
      animatedFrameScheduler.onFrameDropped()
    }

    val nextFrameTime = animatedFrameScheduler.nextRenderTime()
    if (nextFrameTime != INVALID_FRAME_TIME) {
      scheduleSelf(invalidateRunnable, nextFrameTime)
    } else {
      animationListener.onAnimationStop(this)
      animatedFrameScheduler.running = false
    }
  }

  interface DrawListener {
    fun onDraw(
        animatedDrawable: KAnimatedDrawable2,
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
    )
  }
}
