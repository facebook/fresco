/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.gestures;

import android.view.MotionEvent;

/**
 * Component that detects and tracks multiple pointers based on touch events.
 * <p>
 * Each time a pointer gets pressed or released, the current gesture (if any) will end, and a new
 * one will be started (if there are still pressed pointers left). It is guaranteed that the number
 * of pointers within the single gesture will remain the same during the whole gesture.
 */
public class MultiPointerGestureDetector {

  /** The listener for receiving notifications when gestures occur. */
  public interface Listener {
    /** Responds to the beginning of a gesture. */
    public void onGestureBegin(MultiPointerGestureDetector detector);

    /** Responds to the update of a gesture in progress. */
    public void onGestureUpdate(MultiPointerGestureDetector detector);

    /** Responds to the end of a gesture. */
    public void onGestureEnd(MultiPointerGestureDetector detector);
  }

  private static final int MAX_POINTERS = 2;

  private boolean mGestureInProgress;
  private int mCount;
  private final int mId[] = new int[MAX_POINTERS];
  private final float mStartX[] = new float[MAX_POINTERS];
  private final float mStartY[] = new float[MAX_POINTERS];
  private final float mCurrentX[] = new float[MAX_POINTERS];
  private final float mCurrentY[] = new float[MAX_POINTERS];

  private Listener mListener = null;

  public MultiPointerGestureDetector() {
    reset();
  }

  /** Factory method that creates a new instance of MultiPointerGestureDetector */
  public static MultiPointerGestureDetector newInstance() {
    return new MultiPointerGestureDetector();
  }

  /**
   * Sets the listener.
   * @param listener listener to set
   */
  public void setListener(Listener listener) {
    mListener = listener;
  }

  /**
   * Resets the component to the initial state.
   */
  public void reset() {
    mGestureInProgress = false;
    mCount = 0;
    for (int i = 0; i < MAX_POINTERS; i++) {
      mId[i] = MotionEvent.INVALID_POINTER_ID;
    }
  }

  /**
   * This method can be overridden in order to perform threshold check or something similar.
   * @return whether or not to start a new gesture
   */
  protected boolean shouldStartGesture() {
    return true;
  }

  private void startGesture() {
    if (!mGestureInProgress) {
      mGestureInProgress = true;
      if (mListener != null) {
        mListener.onGestureBegin(this);
      }
    }
  }

  private void stopGesture() {
    if (mGestureInProgress) {
      mGestureInProgress = false;
      if (mListener != null) {
        mListener.onGestureEnd(this);
      }
    }
  }

  /**
   * Gets the index of the i-th pressed pointer.
   * Normally, the index will be equal to i, except in the case when the pointer is released.
   * @return index of the specified pointer or -1 if not found (i.e. not enough pointers are down)
   */
  private int getPressedPointerIndex(MotionEvent event, int i) {
    final int count = event.getPointerCount();
    final int action = event.getActionMasked();
    final int index = event.getActionIndex();
    if (action == MotionEvent.ACTION_UP ||
        action == MotionEvent.ACTION_POINTER_UP) {
      if (i >= index) {
        i++;
      }
    }
    return (i < count) ? i : -1;
  }

  /**
   * Handles the given motion event.
   * @param event event to handle
   * @return whether or not the event was handled
   */
  public boolean onTouchEvent(final MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_MOVE: {
        // update pointers
        for (int i = 0; i < MAX_POINTERS; i++) {
          int index = event.findPointerIndex(mId[i]);
          if (index != -1) {
            mCurrentX[i] = event.getX(index);
            mCurrentY[i] = event.getY(index);
          }
        }
        // start a new gesture if not already started
        if (!mGestureInProgress && shouldStartGesture()) {
          startGesture();
        }
        // notify listener
        if (mGestureInProgress && mListener != null) {
          mListener.onGestureUpdate(this);
        }
        break;
      }

      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
      case MotionEvent.ACTION_POINTER_UP:
      case MotionEvent.ACTION_UP: {
        // we'll restart the current gesture (if any) whenever the number of pointers changes
        // NOTE: we only restart existing gestures here, new gestures are started in ACTION_MOVE
        boolean wasGestureInProgress = mGestureInProgress;
        stopGesture();
        reset();
        // update pointers
        for (int i = 0; i < MAX_POINTERS; i++) {
          int index = getPressedPointerIndex(event, i);
          if (index == -1) {
            break;
          }
          mId[i] = event.getPointerId(index);
          mCurrentX[i] = mStartX[i] = event.getX(index);
          mCurrentY[i] = mStartY[i] = event.getY(index);
          mCount++;
        }
        // restart the gesture (if any) if there are still pointers left
        if (wasGestureInProgress && mCount > 0) {
          startGesture();
        }
        break;
      }

      case MotionEvent.ACTION_CANCEL: {
        stopGesture();
        reset();
        break;
      }
    }
    return true;
  }

  /** Restarts the current gesture */
  public void restartGesture() {
    if (!mGestureInProgress) {
      return;
    }
    stopGesture();
    for (int i = 0; i < MAX_POINTERS; i++) {
      mStartX[i] = mCurrentX[i];
      mStartY[i] = mCurrentY[i];
    }
    startGesture();
  }

  /** Gets whether gesture is in progress or not */
  public boolean isGestureInProgress() {
    return mGestureInProgress;
  }

  /** Gets the number of pointers in the current gesture */
  public int getCount() {
    return mCount;
  }

  /**
   * Gets the start X coordinates for the all pointers
   * Mutable array is exposed for performance reasons and is not to be modified by the callers.
   */
  public float[] getStartX() {
    return mStartX;
  }

  /**
   * Gets the start Y coordinates for the all pointers
   * Mutable array is exposed for performance reasons and is not to be modified by the callers.
   */
  public float[] getStartY() {
    return mStartY;
  }

  /**
   * Gets the current X coordinates for the all pointers
   * Mutable array is exposed for performance reasons and is not to be modified by the callers.
   */
  public float[] getCurrentX() {
    return mCurrentX;
  }

  /**
   * Gets the current Y coordinates for the all pointers
   * Mutable array is exposed for performance reasons and is not to be modified by the callers.
   */
  public float[] getCurrentY() {
    return mCurrentY;
  }
}
