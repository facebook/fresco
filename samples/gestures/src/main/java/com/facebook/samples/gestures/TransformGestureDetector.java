/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.gestures;

import android.view.MotionEvent;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Component that detects translation, scale and rotation based on touch events.
 *
 * <p>This class notifies its listeners whenever a gesture begins, updates or ends. The instance of
 * this detector is passed to the listeners, so it can be queried for pivot, translation, scale or
 * rotation.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class TransformGestureDetector implements MultiPointerGestureDetector.Listener {

  /** The listener for receiving notifications when gestures occur. */
  public interface Listener {
    /** A callback called right before the gesture is about to start. */
    public void onGestureBegin(TransformGestureDetector detector);

    /** A callback called each time the gesture gets updated. */
    public void onGestureUpdate(TransformGestureDetector detector);

    /** A callback called right after the gesture has finished. */
    public void onGestureEnd(TransformGestureDetector detector);
  }

  private final MultiPointerGestureDetector mDetector;

  @Nullable private Listener mListener = null;

  public TransformGestureDetector(MultiPointerGestureDetector multiPointerGestureDetector) {
    mDetector = multiPointerGestureDetector;
    mDetector.setListener(this);
  }

  /** Factory method that creates a new instance of TransformGestureDetector */
  public static TransformGestureDetector newInstance() {
    return new TransformGestureDetector(MultiPointerGestureDetector.newInstance());
  }

  /**
   * Sets the listener.
   *
   * @param listener listener to set
   */
  public void setListener(Listener listener) {
    mListener = listener;
  }

  /** Resets the component to the initial state. */
  public void reset() {
    mDetector.reset();
  }

  /**
   * Handles the given motion event.
   *
   * @param event event to handle
   * @return whether or not the event was handled
   */
  public boolean onTouchEvent(final MotionEvent event) {
    return mDetector.onTouchEvent(event);
  }

  @Override
  public void onGestureBegin(MultiPointerGestureDetector detector) {
    if (mListener != null) {
      mListener.onGestureBegin(this);
    }
  }

  @Override
  public void onGestureUpdate(MultiPointerGestureDetector detector) {
    if (mListener != null) {
      mListener.onGestureUpdate(this);
    }
  }

  @Override
  public void onGestureEnd(MultiPointerGestureDetector detector) {
    if (mListener != null) {
      mListener.onGestureEnd(this);
    }
  }

  private float calcAverage(float[] arr, int len) {
    float sum = 0;
    for (int i = 0; i < len; i++) {
      sum += arr[i];
    }
    return (len > 0) ? sum / len : 0;
  }

  /** Restarts the current gesture (if any). */
  public void restartGesture() {
    mDetector.restartGesture();
  }

  /** Gets whether there is a gesture in progress */
  public boolean isGestureInProgress() {
    return mDetector.isGestureInProgress();
  }

  /** Gets the number of pointers after the current gesture */
  public int getNewPointerCount() {
    return mDetector.getNewPointerCount();
  }

  /** Gets the number of pointers in the current gesture */
  public int getPointerCount() {
    return mDetector.getPointerCount();
  }

  /** Gets the X coordinate of the pivot point */
  public float getPivotX() {
    return calcAverage(mDetector.getStartX(), mDetector.getPointerCount());
  }

  /** Gets the Y coordinate of the pivot point */
  public float getPivotY() {
    return calcAverage(mDetector.getStartY(), mDetector.getPointerCount());
  }

  /** Gets the X component of the translation */
  public float getTranslationX() {
    return calcAverage(mDetector.getCurrentX(), mDetector.getPointerCount())
        - calcAverage(mDetector.getStartX(), mDetector.getPointerCount());
  }

  /** Gets the Y component of the translation */
  public float getTranslationY() {
    return calcAverage(mDetector.getCurrentY(), mDetector.getPointerCount())
        - calcAverage(mDetector.getStartY(), mDetector.getPointerCount());
  }

  /** Gets the scale */
  public float getScale() {
    if (mDetector.getPointerCount() < 2) {
      return 1;
    } else {
      float startDeltaX = mDetector.getStartX()[1] - mDetector.getStartX()[0];
      float startDeltaY = mDetector.getStartY()[1] - mDetector.getStartY()[0];
      float currentDeltaX = mDetector.getCurrentX()[1] - mDetector.getCurrentX()[0];
      float currentDeltaY = mDetector.getCurrentY()[1] - mDetector.getCurrentY()[0];
      float startDist = (float) Math.hypot(startDeltaX, startDeltaY);
      float currentDist = (float) Math.hypot(currentDeltaX, currentDeltaY);
      return currentDist / startDist;
    }
  }

  /** Gets the rotation in radians */
  public float getRotation() {
    if (mDetector.getPointerCount() < 2) {
      return 0;
    } else {
      float startDeltaX = mDetector.getStartX()[1] - mDetector.getStartX()[0];
      float startDeltaY = mDetector.getStartY()[1] - mDetector.getStartY()[0];
      float currentDeltaX = mDetector.getCurrentX()[1] - mDetector.getCurrentX()[0];
      float currentDeltaY = mDetector.getCurrentY()[1] - mDetector.getCurrentY()[0];
      float startAngle = (float) Math.atan2(startDeltaY, startDeltaX);
      float currentAngle = (float) Math.atan2(currentDeltaY, currentDeltaX);
      return currentAngle - startAngle;
    }
  }
}
