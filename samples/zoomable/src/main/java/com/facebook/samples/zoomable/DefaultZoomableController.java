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

package com.facebook.samples.zoomable;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.facebook.samples.gestures.TransformGestureDetector;

/**
 * Zoomable controller that calculates transformation based on touch events.
 */
public class DefaultZoomableController
    implements ZoomableController, TransformGestureDetector.Listener {

  private TransformGestureDetector mGestureDetector;

  private Listener mListener = null;

  private boolean mIsEnabled = false;
  private boolean mIsRotationEnabled = false;
  private boolean mIsScaleEnabled = true;
  private boolean mIsTranslationEnabled = true;

  private float mMinScaleFactor = 1.0f;
  private float mMaxScaleFactor = Float.POSITIVE_INFINITY;

  private final RectF mViewBounds = new RectF();
  private final RectF mImageBounds = new RectF();
  private final RectF mTransformedImageBounds = new RectF();
  private final Matrix mPreviousTransform = new Matrix();
  private final Matrix mActiveTransform = new Matrix();
  private final Matrix mActiveTransformInverse = new Matrix();
  private final float[] mTempValues = new float[9];

  public DefaultZoomableController(TransformGestureDetector gestureDetector) {
    mGestureDetector = gestureDetector;
    mGestureDetector.setListener(this);
  }

  public static DefaultZoomableController newInstance() {
    return new DefaultZoomableController(TransformGestureDetector.newInstance());
  }

  @Override
  public void setListener(Listener listener) {
    mListener = listener;
  }

  /** Rests the controller. */
  public void reset() {
    mGestureDetector.reset();
    mPreviousTransform.reset();
    mActiveTransform.reset();
  }

  /** Sets whether the controller is enabled or not. */
  @Override
  public void setEnabled(boolean enabled) {
    mIsEnabled = enabled;
    if (!enabled) {
      reset();
    }
  }

  /** Returns whether the controller is enabled or not. */
  @Override
  public boolean isEnabled() {
    return mIsEnabled;
  }

  /** Sets whether the rotation gesture is enabled or not. */
  public void setRotationEnabled(boolean enabled) {
    mIsRotationEnabled = enabled;
  }

  /** Gets whether the rotation gesture is enabled or not. */
  public boolean isRotationEnabled() {
    return  mIsRotationEnabled;
  }

  /** Sets whether the scale gesture is enabled or not. */
  public void setScaleEnabled(boolean enabled) {
    mIsScaleEnabled = enabled;
  }

  /** Gets whether the scale gesture is enabled or not. */
  public boolean isScaleEnabled() {
    return  mIsScaleEnabled;
  }

  /** Sets whether the translation gesture is enabled or not. */
  public void setTranslationEnabled(boolean enabled) {
    mIsTranslationEnabled = enabled;
  }

  /** Gets whether the translations gesture is enabled or not. */
  public boolean isTranslationEnabled() {
    return  mIsTranslationEnabled;
  }

  /** Gets the image bounds before zoomable transformation is applied. */
  public RectF getImageBounds() {
    return mImageBounds;
  }

  /** Sets the image bounds before zoomable transformation is applied. */
  @Override
  public void setImageBounds(RectF imageBounds) {
    mImageBounds.set(imageBounds);
  }

  /** Gets the view bounds. */
  public RectF getViewBounds() {
    return mViewBounds;
  }

  /** Sets the view bounds. */
  @Override
  public void setViewBounds(RectF viewBounds) {
    mViewBounds.set(viewBounds);
  }

  /** Gets the minimum scale factor allowed. */
  public float getMinScaleFactor() {
    return mMinScaleFactor;
  }

  /**
   * Sets the minimum scale factor allowed.
   * <p>
   * Note that the hierarchy performs scaling as well, which
   * is not accounted here, so the actual scale factor may differ.
   */
  public void setMinScaleFactor(float minScaleFactor) {
    mMinScaleFactor = minScaleFactor;
  }

  /** Gets the maximum scale factor allowed. */
  public float getMaxScaleFactor() {
    return mMaxScaleFactor;
  }

  /**
   * Sets the maximum scale factor allowed.
   * <p>
   * Note that the hierarchy performs scaling as well, which
   * is not accounted here, so the actual scale factor may differ.
   */
  public void setMaxScaleFactor(float maxScaleFactor) {
    mMaxScaleFactor = maxScaleFactor;
  }

  /**
   * Maps point from the view's to the image's relative coordinate system.
   * This takes into account the zoomable transformation.
   */
  public PointF mapViewToImage(PointF viewPoint) {
    float[] points = mTempValues;
    points[0] = viewPoint.x;
    points[1] = viewPoint.y;
    mActiveTransform.invert(mActiveTransformInverse);
    mActiveTransformInverse.mapPoints(points, 0, points, 0, 1);
    mapAbsoluteToRelative(points, points, 1);
    return new PointF(points[0], points[1]);
  }

  /**
   * Maps point from the image's relative to the view's coordinate system.
   * This takes into account the zoomable transformation.
   */
  public PointF mapImageToView(PointF imagePoint) {
    float[] points = mTempValues;
    points[0] = imagePoint.x;
    points[1] = imagePoint.y;
    mapRelativeToAbsolute(points, points, 1);
    mActiveTransform.mapPoints(points, 0, points, 0, 1);
    return new PointF(points[0], points[1]);
  }

  /**
   * Maps array of 2D points from absolute to the image's relative coordinate system,
   * and writes the transformed points back into the array.
   * Points are represented by float array of [x0, y0, x1, y1, ...].
   *
   * @param destPoints destination array (may be the same as source array)
   * @param srcPoints source array
   * @param numPoints number of points to map
   */
  private void mapAbsoluteToRelative(float[] destPoints, float[] srcPoints, int numPoints) {
    for (int i = 0; i < numPoints; i++) {
      destPoints[i * 2 + 0] = (srcPoints[i * 2 + 0] - mImageBounds.left) / mImageBounds.width();
      destPoints[i * 2 + 1] = (srcPoints[i * 2 + 1] - mImageBounds.top) / mImageBounds.height();
    }
  }

  /**
   * Maps array of 2D points from relative to the image's absolute coordinate system,
   * and writes the transformed points back into the array
   * Points are represented by float array of [x0, y0, x1, y1, ...].
   *
   * @param destPoints destination array (may be the same as source array)
   * @param srcPoints source array
   * @param numPoints number of points to map
   */
  private void mapRelativeToAbsolute(float[] destPoints, float[] srcPoints, int numPoints) {
    for (int i = 0; i < numPoints; i++) {
      destPoints[i * 2 + 0] = srcPoints[i * 2 + 0] * mImageBounds.width() + mImageBounds.left;
      destPoints[i * 2 + 1] = srcPoints[i * 2 + 1] * mImageBounds.height() + mImageBounds.top;
    }
  }

  /**
   * Gets the zoomable transformation
   * Internal matrix is exposed for performance reasons and is not to be modified by the callers.
   */
  @Override
  public Matrix getTransform() {
    return mActiveTransform;
  }

  /**
   * Sets the zoomable transformation. Cancels the current gesture if one is happening.
   */
  public void setTransform(Matrix activeTransform) {
    if (mGestureDetector.isGestureInProgress()) {
      mGestureDetector.reset();
    }
    mActiveTransform.set(activeTransform);
  }

  /** Notifies controller of the received touch event.  */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mIsEnabled) {
      return mGestureDetector.onTouchEvent(event);
    }
    return false;
  }

  /**
   * Zooms to the desired scale and positions the view so that imagePoint is in the center.
   * <p>
   * It might not be possible to center imagePoint (= a corner for e.g.), in those cases the view
   * will be adjusted so that there are no black bars in it.
   * Resets any previous transform and cancels the current gesture if one is happening.
   *
   * @param scale desired scale, will be limited to {min, max} scale factor
   * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
   */
  public void zoomToImagePoint(float scale, PointF imagePoint) {
    if (mGestureDetector.isGestureInProgress()) {
      mGestureDetector.reset();
    }
    scale = limit(scale, mMinScaleFactor, mMaxScaleFactor);
    float[] points = mTempValues;
    points[0] = imagePoint.x;
    points[1] = imagePoint.y;
    mapRelativeToAbsolute(points, points, 1);
    mActiveTransform.setScale(scale, scale, points[0], points[1]);
    mActiveTransform.postTranslate(
        mViewBounds.centerX() - points[0],
        mViewBounds.centerY() - points[1]);
    limitTranslation();
  }

  /* TransformGestureDetector.Listener methods  */

  @Override
  public void onGestureBegin(TransformGestureDetector detector) {
    mPreviousTransform.set(mActiveTransform);
  }

  @Override
  public void onGestureUpdate(TransformGestureDetector detector) {
    mActiveTransform.set(mPreviousTransform);
    if (mIsRotationEnabled) {
      float angle = detector.getRotation() * (float) (180 / Math.PI);
      mActiveTransform.postRotate(angle, detector.getPivotX(), detector.getPivotY());
    }
    if (mIsScaleEnabled) {
      float scale = detector.getScale();
      mActiveTransform.postScale(scale, scale, detector.getPivotX(), detector.getPivotY());
    }
    limitScale(detector.getPivotX(), detector.getPivotY());
    if (mIsTranslationEnabled) {
      mActiveTransform.postTranslate(detector.getTranslationX(), detector.getTranslationY());
    }
    if (limitTranslation()) {
      mGestureDetector.restartGesture();
    }
    if (mListener != null) {
      mListener.onTransformChanged(mActiveTransform);
    }
  }

  @Override
  public void onGestureEnd(TransformGestureDetector detector) {
    mPreviousTransform.set(mActiveTransform);
  }

  /** Gets the current scale factor. */
  @Override
  public float getScaleFactor() {
    mActiveTransform.getValues(mTempValues);
    return mTempValues[Matrix.MSCALE_X];
  }

  private void limitScale(float pivotX, float pivotY) {
    float currentScale = getScaleFactor();
    float targetScale = limit(currentScale, mMinScaleFactor, mMaxScaleFactor);
    if (targetScale != currentScale) {
      float scale = targetScale / currentScale;
      mActiveTransform.postScale(scale, scale, pivotX, pivotY);
    }
  }

  /**
   * Keeps the view inside the image if possible, if not (i.e. image is smaller than view)
   * centers the image.
   * @return whether adjustments were needed or not
   */
  private boolean limitTranslation() {
    RectF bounds = mTransformedImageBounds;
    bounds.set(mImageBounds);
    mActiveTransform.mapRect(bounds);

    float offsetLeft = getOffset(bounds.left, bounds.width(), mViewBounds.width());
    float offsetTop = getOffset(bounds.top, bounds.height(), mViewBounds.height());
    if (offsetLeft != bounds.left || offsetTop != bounds.top) {
      mActiveTransform.postTranslate(offsetLeft - bounds.left, offsetTop - bounds.top);
      return true;
    }
    return false;
  }

  private float getOffset(float offset, float imageDimension, float viewDimension) {
    float diff = viewDimension - imageDimension;
    return (diff > 0) ? diff / 2 : limit(offset, diff, 0);
  }

  private float limit(float value, float min, float max) {
    return Math.min(Math.max(min, value), max);
  }

}
