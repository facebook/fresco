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
    /** A callback called right before the gesture is about to start. */
    public void onGestureBegin(MultiPointerGestureDetector detector);

    /** A callback called each time the gesture gets updated. */
    public void onGestureUpdate(MultiPointerGestureDetector detector);

    /** A callback called right after the gesture has finished. */
    public void onGestureEnd(MultiPointerGestureDetector detector);
  }

  private static final int MAX_POINTERS = 2;

  private boolean mGestureInProgress;
  private int mPointerCount;
  private int mNewPointerCount;
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
    mPointerCount = 0;
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

  /**
   * Starts a new gesture and calls the listener just before starting it.
   */
  private void startGesture() {
    if (!mGestureInProgress) {
      if (mListener != null) {
        mListener.onGestureBegin(this);
      }
      mGestureInProgress = true;
    }
  }

  /**
   * Stops the current gesture and calls the listener right after stopping it.
   */
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
   * Gets the number of pressed pointers (fingers down).
   */
  private static int getPressedPointerCount(MotionEvent event) {
    int count = event.getPointerCount();
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_UP ||
        action == MotionEvent.ACTION_POINTER_UP) {
      count--;
    }
    return count;
  }

  private void updatePointersOnTap(MotionEvent event) {
    mPointerCount = 0;
    for (int i = 0; i < MAX_POINTERS; i++) {
      int index = getPressedPointerIndex(event, i);
      if (index == -1) {
        mId[i] = MotionEvent.INVALID_POINTER_ID;
      } else {
        mId[i] = event.getPointerId(index);
        mCurrentX[i] = mStartX[i] = event.getX(index);
        mCurrentY[i] = mStartY[i] = event.getY(index);
        mPointerCount++;
      }
    }
  }

  private void updatePointersOnMove(MotionEvent event) {
    for (int i = 0; i < MAX_POINTERS; i++) {
      int index = event.findPointerIndex(mId[i]);
      if (index != -1) {
        mCurrentX[i] = event.getX(index);
        mCurrentY[i] = event.getY(index);
      }
    }
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
        updatePointersOnMove(event);
        // start a new gesture if not already started
        if (!mGestureInProgress && mPointerCount > 0 && shouldStartGesture()) {
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
        // restart gesture whenever the number of pointers changes
        mNewPointerCount = getPressedPointerCount(event);
        stopGesture();
        updatePointersOnTap(event);
        if (mPointerCount > 0 && shouldStartGesture()) {
          startGesture();
        }
        break;
      }

      case MotionEvent.ACTION_CANCEL: {
        mNewPointerCount = 0;
        stopGesture();
        reset();
        break;
      }
    }
    return true;
  }

  /** Restarts the current gesture (if any).  */
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

  /** Gets whether there is a gesture in progress */
  public boolean isGestureInProgress() {
    return mGestureInProgress;
  }

  /** Gets the number of pointers after the current gesture */
  public int getNewPointerCount() {
    return mNewPointerCount;
  }

  /** Gets the number of pointers in the current gesture */
  public int getPointerCount() {
    return mPointerCount;
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
