/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.gestures

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.VisibleForTesting
import kotlin.math.abs

/**
 * Gesture detector based on touch events.
 *
 * This class allows us to get click events when we need them, but not to consume them when we are
 * temporarily not interested in them. Doing `View.setClickable(true)` will cause for the view
 * always to consume click event, even if `View.performClick` is overridden to return false. That
 * means even though our view didn't handle the click event, the event will not get propagated
 * upwards. Result of `View.onTouchEvent` is handled correctly though so we use that instead.
 *
 * This class currently only detects clicks.
 */
class GestureDetector(context: Context) {

  /** Interface for the click listener. */
  fun interface ClickListener {
    fun onClick(): Boolean
  }

  @VisibleForTesting var mClickListener: ClickListener? = null

  @JvmField @VisibleForTesting val mSingleTapSlopPx: Float

  @JvmField @VisibleForTesting var isCapturingGesture: Boolean = false

  /** Returns whether the gesture capturing is in progress. */
  fun isCapturingGesture(): Boolean = isCapturingGesture

  // automatically generated for Java compatibility, please inline it as soon as possible
  fun setIsCapturingGesture(value: Boolean) {
    this.isCapturingGesture = value
  }

  @JvmField @VisibleForTesting var mIsClickCandidate: Boolean = false

  @JvmField @VisibleForTesting var mActionDownTime: Long = 0

  @JvmField @VisibleForTesting var mActionDownX: Float = 0f

  @JvmField @VisibleForTesting var mActionDownY: Float = 0f

  init {
    val viewConfiguration = ViewConfiguration.get(context)
    mSingleTapSlopPx = viewConfiguration.scaledTouchSlop.toFloat()
    init()
  }

  /** Initializes this component to its initial state. */
  fun init() {
    mClickListener = null
    reset()
  }

  /**
   * Resets component.
   *
   * This will drop any gesture recognition that might currently be in progress.
   */
  fun reset() {
    isCapturingGesture = false
    mIsClickCandidate = false
  }

  /** Sets the click listener. */
  fun setClickListener(clickListener: ClickListener) {
    this.mClickListener = clickListener
  }

  /** Handles the touch event */
  fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        isCapturingGesture = true
        mIsClickCandidate = true
        mActionDownTime = event.eventTime
        mActionDownX = event.x
        mActionDownY = event.y
      }

      MotionEvent.ACTION_MOVE ->
          if (
              abs(event.x - mActionDownX) > mSingleTapSlopPx ||
                  abs(event.y - mActionDownY) > mSingleTapSlopPx
          ) {
            mIsClickCandidate = false
          }

      MotionEvent.ACTION_CANCEL -> {
        isCapturingGesture = false
        mIsClickCandidate = false
      }

      MotionEvent.ACTION_UP -> {
        isCapturingGesture = false
        if (
            abs(event.x - mActionDownX) > mSingleTapSlopPx ||
                abs(event.y - mActionDownY) > mSingleTapSlopPx
        ) {
          mIsClickCandidate = false
        }
        if (mIsClickCandidate) {
          if (event.eventTime - mActionDownTime <= ViewConfiguration.getLongPressTimeout()) {
            mClickListener?.onClick()
          } else {
            // long click, not handled
          }
        }
        mIsClickCandidate = false
      }
      else -> {}
    }
    return true
  }

  companion object {
    /** Creates a new instance of this gesture detector. */
    @JvmStatic fun newInstance(context: Context): GestureDetector = GestureDetector(context)
  }
}
