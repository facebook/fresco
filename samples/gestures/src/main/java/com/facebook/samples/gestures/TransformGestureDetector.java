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
 * Component that detects translation, scale and rotation based on touch events.
 * <p>
 * This class notifies its listeners whenever a gesture begins, updates or ends.
 * The instance of this detector is passed to the listeners, so it can be queried
 * for pivot, translation, scale or rotation.
 */
public class TransformGestureDetector implements MultiPointerGestureDetector.Listener {

  /** The listener for receiving notifications when gestures occur. */
  public interface Listener {
    /** Responds to the beginning of a gesture. */
    public void onGestureBegin(TransformGestureDetector detector);

    /** Responds to the update of a gesture in progress. */
    public void onGestureUpdate(TransformGestureDetector detector);

    /** Responds to the end of a gesture. */
    public void onGestureEnd(TransformGestureDetector detector);
  }

  private final MultiPointerGestureDetector mDetector;

  private Listener mListener = null;

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
   * @param listener listener to set
   */
  public void setListener(Listener listener) {
    mListener = listener;
  }

  /**
   * Resets the component to the initial state.
   */
  public void reset() {
    mDetector.reset();
  }

  /**
   * Handles the given motion event.
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

  /** Restarts the current gesture */
  public void restartGesture() {
    mDetector.restartGesture();
  }

  /** Gets whether gesture is in progress or not */
  public boolean isGestureInProgress() {
    return mDetector.isGestureInProgress();
  }

  /** Gets the X coordinate of the pivot point */
  public float getPivotX() {
    return calcAverage(mDetector.getStartX(), mDetector.getCount());
  }

  /** Gets the Y coordinate of the pivot point */
  public float getPivotY() {
    return calcAverage(mDetector.getStartY(), mDetector.getCount());
  }

  /** Gets the X component of the translation */
  public float getTranslationX() {
    return calcAverage(mDetector.getCurrentX(), mDetector.getCount()) -
        calcAverage(mDetector.getStartX(), mDetector.getCount());
  }

  /** Gets the Y component of the translation */
  public float getTranslationY() {
    return calcAverage(mDetector.getCurrentY(), mDetector.getCount()) -
        calcAverage(mDetector.getStartY(), mDetector.getCount());
  }

  /** Gets the scale */
  public float getScale() {
    if (mDetector.getCount() < 2) {
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
    if (mDetector.getCount() < 2) {
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
