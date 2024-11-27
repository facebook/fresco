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
import android.os.SystemClock
import com.facebook.common.logging.FLog
import com.facebook.drawable.base.DrawableWithCaches
import com.facebook.drawee.drawable.DrawableProperties
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.frame.DropFramesFrameScheduler
import com.facebook.fresco.animation.frame.FrameScheduler
import kotlin.concurrent.Volatile
import kotlin.math.max

/**
 * Experimental new animated drawable that uses a supplied [AnimationBackend] for drawing frames.
 */
open class AnimatedDrawable2
@JvmOverloads
constructor(private var _animationBackend: AnimationBackend? = null) :
    Drawable(), Animatable, DrawableWithCaches {

  /** [draw(Canvas)] listener that is notified for each draw call. Can be used for debugging. */
  fun interface DrawListener {
    fun onDraw(
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
    )
  }

  private var frameScheduler: FrameScheduler?

  // Animation parameters
  @Volatile private var _isRunning = false
  var startTimeMs: Long = 0
    private set

  private var lastFrameAnimationTimeMs: Long = 0
  private var expectedRenderTimeMs: Long = 0
  private var lastDrawnFrameNumber = 0

  private var pausedStartTimeMsDifference: Long = 0
  private var pausedLastFrameAnimationTimeMsDifference: Long = 0
  private var pausedLastDrawnFrameNumber = 0

  private var frameSchedulingDelayMs = DEFAULT_FRAME_SCHEDULING_DELAY_MS.toLong()
  private var frameSchedulingOffsetMs = DEFAULT_FRAME_SCHEDULING_OFFSET_MS.toLong()

  // Animation statistics
  private var _droppedFrames = 0

  // Listeners
  @Volatile private var animationListener = NO_OP_LISTENER

  @Volatile private var drawListener: DrawListener? = null
  private val animationBackendListener =
      AnimationBackend.Listener { animationListener.onAnimationLoaded() }

  // Holder for drawable properties like alpha to be able to re-apply if the backend changes.
  // The instance is created lazily only if needed.
  private var drawableProperties: DrawableProperties? = null

  /**
   * Runnable that invalidates the drawable that will be scheduled according to the next target
   * frame.
   */
  private val invalidateRunnable: Runnable =
      object : Runnable {
        override fun run() {
          // Remove all potential other scheduled runnables
          // (e.g. if the view has been invalidated a lot)
          unscheduleSelf(this)
          // Draw the next frame
          invalidateSelf()
        }
      }

  override fun getIntrinsicWidth(): Int {
    return _animationBackend?.intrinsicWidth ?: super.getIntrinsicWidth()
  }

  override fun getIntrinsicHeight(): Int {
    return _animationBackend?.intrinsicHeight ?: super.getIntrinsicHeight()
  }

  /** Start the animation. */
  override fun start() {
    if (_isRunning || _animationBackend == null || _animationBackend!!.frameCount <= 1) {
      return
    }
    _isRunning = true

    val now = now()
    startTimeMs = now - pausedStartTimeMsDifference
    expectedRenderTimeMs = startTimeMs
    lastFrameAnimationTimeMs = now - pausedLastFrameAnimationTimeMsDifference
    lastDrawnFrameNumber = pausedLastDrawnFrameNumber
    invalidateSelf()
    animationListener.onAnimationStart(this)
  }

  /** Stop the animation at the current frame. It can be resumed by calling [start()] again. */
  override fun stop() {
    if (!_isRunning) {
      return
    }
    val now = now()
    pausedStartTimeMsDifference = now - startTimeMs
    pausedLastFrameAnimationTimeMsDifference = now - lastFrameAnimationTimeMs
    pausedLastDrawnFrameNumber = lastDrawnFrameNumber

    _isRunning = false
    startTimeMs = 0
    expectedRenderTimeMs = startTimeMs
    lastFrameAnimationTimeMs = -1
    lastDrawnFrameNumber = -1
    unscheduleSelf(invalidateRunnable)
    animationListener.onAnimationStop(this)
  }

  /**
   * Check whether the animation is running.
   *
   * @return true if the animation is currently running
   */
  override fun isRunning(): Boolean = _isRunning

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    _animationBackend?.setBounds(bounds)
  }

  override fun draw(canvas: Canvas) {
    if (_animationBackend == null || frameScheduler == null) {
      return
    }

    val actualRenderTimeStartMs = now()
    val animationTimeMs =
        if (_isRunning) (actualRenderTimeStartMs - startTimeMs + frameSchedulingOffsetMs)
        else max(lastFrameAnimationTimeMs.toDouble(), 0.0).toLong()

    // What frame should be drawn?
    var frameNumberToDraw =
        frameScheduler!!.getFrameNumberToRender(animationTimeMs, lastFrameAnimationTimeMs)

    // Check if the animation is finished and draw last frame if so
    if (frameNumberToDraw == FrameScheduler.FRAME_NUMBER_DONE) {
      frameNumberToDraw = _animationBackend!!.frameCount - 1
      animationListener.onAnimationStop(this)
      _isRunning = false
    } else if (frameNumberToDraw == 0) {
      if (lastDrawnFrameNumber != -1 && actualRenderTimeStartMs >= expectedRenderTimeMs) {
        animationListener.onAnimationRepeat(this)
      }
    }

    // Draw the frame
    val frameDrawn = _animationBackend!!.drawFrame(this, canvas, frameNumberToDraw)
    if (frameDrawn) {
      // Notify listeners that we draw a new frame and
      // that the animation might be repeated
      animationListener.onAnimationFrame(this, frameNumberToDraw)
      lastDrawnFrameNumber = frameNumberToDraw
    }

    // Log potential dropped frames
    if (!frameDrawn) {
      onFrameDropped()
    }

    var targetRenderTimeForNextFrameMs = FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong()
    var scheduledRenderTimeForNextFrameMs: Long = -1
    val actualRenderTimeEnd = now()
    if (_isRunning) {
      // Schedule the next frame if needed.
      targetRenderTimeForNextFrameMs =
          frameScheduler!!.getTargetRenderTimeForNextFrameMs(actualRenderTimeEnd - startTimeMs)
      if (targetRenderTimeForNextFrameMs != FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong()) {
        scheduledRenderTimeForNextFrameMs = targetRenderTimeForNextFrameMs + frameSchedulingDelayMs
        scheduleNextFrame(scheduledRenderTimeForNextFrameMs)
      } else {
        animationListener.onAnimationStop(this)
        _isRunning = false
      }
    }

    val listener = drawListener
    listener?.onDraw(
        this,
        checkNotNull(frameScheduler),
        frameNumberToDraw,
        frameDrawn,
        _isRunning,
        startTimeMs,
        animationTimeMs,
        lastFrameAnimationTimeMs,
        actualRenderTimeStartMs,
        actualRenderTimeEnd,
        targetRenderTimeForNextFrameMs,
        scheduledRenderTimeForNextFrameMs)
    lastFrameAnimationTimeMs = animationTimeMs
  }

  override fun setAlpha(alpha: Int) {
    if (drawableProperties == null) {
      drawableProperties = DrawableProperties()
    }
    drawableProperties!!.setAlpha(alpha)
    _animationBackend?.setAlpha(alpha)
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    if (drawableProperties == null) {
      drawableProperties = DrawableProperties()
    }
    drawableProperties!!.setColorFilter(colorFilter)
    _animationBackend?.setColorFilter(colorFilter)
  }

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  fun preloadAnimation() {
    _animationBackend?.preloadAnimation()
  }

  var animationBackend: AnimationBackend?
    get() = _animationBackend
    /**
     * Update the animation backend to be used for the animation. This will also stop the animation.
     * In order to remove the current animation backend, call this method with null.
     *
     * @param animationBackend the animation backend to be used or null
     */
    set(animationBackend) {
      if (this._animationBackend != null) {
        _animationBackend!!.setAnimationListener(null)
      }

      this._animationBackend = animationBackend
      if (this._animationBackend != null) {
        frameScheduler = DropFramesFrameScheduler(_animationBackend!!)
        _animationBackend!!.setAnimationListener(animationBackendListener)
        _animationBackend!!.setBounds(bounds)
        drawableProperties?.applyTo(this)
      }
      frameScheduler = createSchedulerForBackendAndDelayMethod(this._animationBackend)
      stop()
    }

  val droppedFrames: Long
    get() = _droppedFrames.toLong()

  val isInfiniteAnimation: Boolean
    get() = frameScheduler?.isInfiniteAnimation == true

  /**
   * Jump immediately to the given frame number. The animation will not be paused if it is running.
   * If the animation is not running, the animation will not be started.
   *
   * @param targetFrameNumber the frame number to jump to
   */
  fun jumpToFrame(targetFrameNumber: Int) {
    if (_animationBackend == null || frameScheduler == null) {
      return
    }
    // In order to jump to a given frame, we have to compute the correct start time
    lastFrameAnimationTimeMs = frameScheduler!!.getTargetRenderTimeMs(targetFrameNumber)

    // Reset the paused timing as we broke the animation frame flow
    pausedLastDrawnFrameNumber = targetFrameNumber
    pausedStartTimeMsDifference = 0
    pausedLastFrameAnimationTimeMsDifference = 0

    startTimeMs = now() - lastFrameAnimationTimeMs
    expectedRenderTimeMs = startTimeMs
    invalidateSelf()
  }

  val loopDurationMs: Long
    /**
     * Get the animation duration for 1 loop by summing all frame durations.
     *
     * @return the duration of 1 animation loop in ms
     */
    get() {
      if (_animationBackend == null) {
        return 0L
      }
      if (frameScheduler != null) {
        return frameScheduler!!.loopDurationMs
      }
      var loopDurationMs = 0
      for (i in 0..<_animationBackend!!.frameCount) {
        loopDurationMs += _animationBackend!!.getFrameDurationMs(i)
      }
      return loopDurationMs.toLong()
    }

  val frameCount: Int
    /**
     * Get the number of frames for the animation. If no animation backend is set, 0 will be
     * returned.
     *
     * @return the number of frames of the animation
     */
    get() = if (_animationBackend == null) 0 else _animationBackend!!.frameCount

  /**
   * Get the duration of a specific frame. If not animation backend is set, 0 will be returned.
   *
   * @param frameNumber the requested frame
   * @return the duration of the frame
   */
  fun getFrameDurationMs(frameNumber: Int): Int =
      if (_animationBackend == null) 0 else _animationBackend!!.getFrameDurationMs(frameNumber)

  val loopCount: Int
    /**
     * Get the loop count of the animation. The returned value is either
     * [AnimationInformation#LOOP_COUNT_INFINITE] if the animation is repeated infinitely or a
     * positive integer that corresponds to the number of loops. If no animation backend is set,
     * [AnimationInformation#LOOP_COUNT_INFINITE] will be returned.
     *
     * @return the loop count of the animation or [AnimationInformation#LOOP_COUNT_INFINITE]
     */
    get() = if (_animationBackend == null) 0 else _animationBackend!!.loopCount

  init {
    frameScheduler = createSchedulerForBackendAndDelayMethod(this._animationBackend)

    _animationBackend?.setAnimationListener(animationBackendListener)
  }

  /**
   * Frame scheduling delay to shift the target render time for a frame within the frame's visible
   * window. If the value is set to 0, the frame will be scheduled right at the beginning of the
   * frame's visible window.
   *
   * @param frameSchedulingDelayMs the delay to use in ms
   */
  fun setFrameSchedulingDelayMs(frameSchedulingDelayMs: Long) {
    this.frameSchedulingDelayMs = frameSchedulingDelayMs
  }

  /**
   * Frame scheduling offset to shift the animation time by the given offset. This is similar to
   * [mFrameSchedulingDelayMs] but instead of delaying the invalidation, this offsets the animation
   * time by the given value.
   *
   * @param frameSchedulingOffsetMs the offset to use in ms
   */
  fun setFrameSchedulingOffsetMs(frameSchedulingOffsetMs: Long) {
    this.frameSchedulingOffsetMs = frameSchedulingOffsetMs
  }

  /**
   * Set an animation listener that is notified for various animation events.
   *
   * @param animationListener the listener to use
   */
  fun setAnimationListener(animationListener: AnimationListener?) {
    this.animationListener = animationListener ?: NO_OP_LISTENER
  }

  /**
   * Set a draw listener that is notified for each [draw(Canvas)] call.
   *
   * @param drawListener the listener to use
   */
  fun setDrawListener(drawListener: DrawListener?) {
    this.drawListener = drawListener
  }

  /**
   * Schedule the next frame to be rendered after the given delay.
   *
   * @param targetAnimationTimeMs the time in ms to update the frame
   */
  private fun scheduleNextFrame(targetAnimationTimeMs: Long) {
    expectedRenderTimeMs = startTimeMs + targetAnimationTimeMs
    scheduleSelf(invalidateRunnable, expectedRenderTimeMs)
  }

  private fun onFrameDropped() {
    _droppedFrames++
    // we need to drop frames
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "Dropped a frame. Count: %s", _droppedFrames)
    }
  }

  /** @return the current uptime in ms */
  private fun now(): Long =
      // This call has to return [SystemClock#uptimeMillis()] in order to preserve correct
      // frame scheduling.
      SystemClock.uptimeMillis()

  /**
   * Set the animation to the given level. The level represents the animation time in ms. If the
   * animation time is greater than the last frame time for the last loop, the last frame will be
   * displayed.
   *
   * If the animation is running (e.g. if [start()] has been called, the level change will be
   * ignored. In this case, [stop()] the animation first.
   *
   * @param level the animation time in ms
   * @return true if the level change could be performed
   */
  override fun onLevelChange(level: Int): Boolean {
    if (_isRunning) {
      // If the client called start on us, they expect us to run the animation. In that case,
      // we ignore level changes.
      return false
    }
    if (lastFrameAnimationTimeMs != level.toLong()) {
      lastFrameAnimationTimeMs = level.toLong()
      invalidateSelf()
      return true
    }
    return false
  }

  override fun dropCaches() {
    _animationBackend?.clear()
  }

  companion object {
    private val TAG: Class<*> = AnimatedDrawable2::class.java

    private val NO_OP_LISTENER: AnimationListener = BaseAnimationListener()

    private const val DEFAULT_FRAME_SCHEDULING_DELAY_MS = 8
    private const val DEFAULT_FRAME_SCHEDULING_OFFSET_MS = 0

    private fun createSchedulerForBackendAndDelayMethod(
        animationBackend: AnimationBackend?
    ): FrameScheduler? {
      if (animationBackend == null) {
        return null
      }
      return DropFramesFrameScheduler(animationBackend)
    }
  }
}
