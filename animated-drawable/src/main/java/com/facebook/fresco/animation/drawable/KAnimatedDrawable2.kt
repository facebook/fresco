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
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import com.facebook.drawable.base.DrawableWithCaches
import com.facebook.drawee.drawable.DrawableProperties
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.frame.FrameScheduler

class KAnimatedDrawable2(private val animationBackend: AnimationBackend) :
    Drawable(), Animatable, DrawableWithCaches {

  private val animatedFrameScheduler = AnimationFrameScheduler(animationBackend)
  private val animationListener: AnimationListener = BaseAnimationListener()
  private val drawableProperties = DrawableProperties().apply { applyTo(this@KAnimatedDrawable2) }

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

  override fun draw(canvas: Canvas) {
    // Render command must be in charge of working this
    render(canvas)
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

  override fun start() {
    if (animationBackend.frameCount > 1) {
      animatedFrameScheduler.start()
      animationListener.onAnimationStart(this)
      invalidateSelf()
    }
  }

  override fun stop() {
    animatedFrameScheduler.stop()
    animationListener.onAnimationStop(this)
    unscheduleSelf(invalidateRunnable)
  }

  override fun isRunning(): Boolean = animatedFrameScheduler.running

  override fun dropCaches() {
    animationBackend.clear()
  }

  override fun getIntrinsicWidth(): Int = animationBackend.intrinsicWidth

  override fun getIntrinsicHeight(): Int = animationBackend.intrinsicHeight

  // Render command should be placed here.
  fun render(canvas: Canvas) {
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
}
