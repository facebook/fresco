/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.gestures;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.facebook.common.internal.VisibleForTesting;

/**
 * Gesture detector based on touch events.
 * <p>
 * This class allows us to get click events when we need them, but not to consume them when we are
 * temporarily not interested in them. Doing {@code View.setClickable(true)} will cause for the
 * view always to consume click event, even if {@code View.performClick} is overridden to return
 * false. That means even though our view didn't handle the click event, the event will not get
 * propagated upwards. Result of {@code View.onTouchEvent} is handled correctly though so we use
 * that instead.
 * <p> This class currently only detects clicks.
 */
public class GestureDetector {

  /** Interface for the click listener. */
  public interface ClickListener {
    public boolean onClick();
  }

  @VisibleForTesting ClickListener mClickListener;

  @VisibleForTesting final float mSingleTapSlopPx;
  @VisibleForTesting boolean mIsCapturingGesture;
  @VisibleForTesting boolean mIsClickCandidate;
  @VisibleForTesting long mActionDownTime;
  @VisibleForTesting float mActionDownX;
  @VisibleForTesting float mActionDownY;

  public GestureDetector(Context context) {
    final ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
    mSingleTapSlopPx = viewConfiguration.getScaledTouchSlop();
    init();
  }

  /** Creates a new instance of this gesture detector. */
  public static GestureDetector newInstance(Context context) {
    return new GestureDetector(context);
  }

  /** Initializes this component to its initial state. */
  public void init() {
    mClickListener = null;
    reset();
  }

  /**
   * Resets component.
   * <p> This will drop any gesture recognition that might currently be in progress.
   */
  public void reset() {
    mIsCapturingGesture = false;
    mIsClickCandidate = false;
  }

  /** Sets the click listener. */
  public void setClickListener(ClickListener clickListener) {
    mClickListener = clickListener;
  }

  /** Returns whether the gesture capturing is in progress. */
  public boolean isCapturingGesture() {
    return mIsCapturingGesture;
  }

  /** Handles the touch event */
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        mIsCapturingGesture = true;
        mIsClickCandidate = true;
        mActionDownTime = event.getEventTime();
        mActionDownX = event.getX();
        mActionDownY = event.getY();
        break;
      case MotionEvent.ACTION_MOVE:
        if (Math.abs(event.getX() - mActionDownX) > mSingleTapSlopPx ||
            Math.abs(event.getY() - mActionDownY) > mSingleTapSlopPx) {
          mIsClickCandidate = false;
        }
        break;
      case MotionEvent.ACTION_CANCEL:
        mIsCapturingGesture = false;
        mIsClickCandidate = false;
        break;
      case MotionEvent.ACTION_UP:
        mIsCapturingGesture = false;
        if (Math.abs(event.getX() - mActionDownX) > mSingleTapSlopPx ||
            Math.abs(event.getY() - mActionDownY) > mSingleTapSlopPx) {
          mIsClickCandidate = false;
        }
        if (mIsClickCandidate) {
          if (event.getEventTime() - mActionDownTime <= ViewConfiguration.getLongPressTimeout()) {
            if (mClickListener != null) {
              mClickListener.onClick();
            }
          } else {
            // long click, not handled
          }
        }
        mIsClickCandidate = false;
        break;
    }
    return true;
  }

}
