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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.IntDef;
import android.view.MotionEvent;

import com.facebook.common.logging.FLog;
import com.facebook.samples.gestures.TransformGestureDetector;

/**
 * Zoomable controller that calculates transformation based on touch events.
 */
public class DefaultZoomableController
    implements ZoomableController, TransformGestureDetector.Listener {

  @IntDef(flag=true, value={
      LIMIT_NONE,
      LIMIT_TRANSLATION_X,
      LIMIT_TRANSLATION_Y,
      LIMIT_SCALE,
      LIMIT_ALL
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface LimitFlag {}

  public static final int LIMIT_NONE = 0;
  public static final int LIMIT_TRANSLATION_X = 1;
  public static final int LIMIT_TRANSLATION_Y = 2;
  public static final int LIMIT_SCALE = 4;
  public static final int LIMIT_ALL = LIMIT_TRANSLATION_X | LIMIT_TRANSLATION_Y | LIMIT_SCALE;

  private static final Class<?> TAG = DefaultZoomableController.class;

  private static final RectF IDENTITY_RECT = new RectF(0, 0, 1, 1);

  private TransformGestureDetector mGestureDetector;

  private Listener mListener = null;

  private boolean mIsEnabled = false;
  private boolean mIsRotationEnabled = false;
  private boolean mIsScaleEnabled = true;
  private boolean mIsTranslationEnabled = true;

  private float mMinScaleFactor = 1.0f;
  private float mMaxScaleFactor = 2.0f;

  // View bounds, in view-absolute coordinates
  private final RectF mViewBounds = new RectF();
  // Non-transformed image bounds, in view-absolute coordinates
  private final RectF mImageBounds = new RectF();
  // Transformed image bounds, in view-absolute coordinates
  private final RectF mTransformedImageBounds = new RectF();

  private final Matrix mPreviousTransform = new Matrix();
  private final Matrix mActiveTransform = new Matrix();
  private final Matrix mActiveTransformInverse = new Matrix();
  private final float[] mTempValues = new float[9];
  private final RectF mTempRect = new RectF();

  public static DefaultZoomableController newInstance() {
    return new DefaultZoomableController(TransformGestureDetector.newInstance());
  }

  public DefaultZoomableController(TransformGestureDetector gestureDetector) {
    mGestureDetector = gestureDetector;
    mGestureDetector.setListener(this);
  }

  /** Rests the controller. */
  public void reset() {
    FLog.v(TAG, "reset");
    mGestureDetector.reset();
    mPreviousTransform.reset();
    mActiveTransform.reset();
    onTransformChanged();
  }

  /** Sets the zoomable listener. */
  @Override
  public void setListener(Listener listener) {
    mListener = listener;
  }

  /** Sets whether the controller is enabled or not. */
  @Override
  public void setEnabled(boolean enabled) {
    mIsEnabled = enabled;
    if (!enabled) {
      reset();
    }
  }

  /** Gets whether the controller is enabled or not. */
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

  /**
   * Sets the minimum scale factor allowed.
   * <p> Hierarchy's scaling (if any) is not taken into account.
   */
  public void setMinScaleFactor(float minScaleFactor) {
    mMinScaleFactor = minScaleFactor;
  }

  /** Gets the minimum scale factor allowed. */
  public float getMinScaleFactor() {
    return mMinScaleFactor;
  }

  /**
   * Sets the maximum scale factor allowed.
   * <p> Hierarchy's scaling (if any) is not taken into account.
   */
  public void setMaxScaleFactor(float maxScaleFactor) {
    mMaxScaleFactor = maxScaleFactor;
  }

  /** Gets the maximum scale factor allowed. */
  public float getMaxScaleFactor() {
    return mMaxScaleFactor;
  }

  /** Gets the current scale factor. */
  @Override
  public float getScaleFactor() {
    return getMatrixScaleFactor(mActiveTransform);
  }

  /** Sets the image bounds, in view-absolute coordinates. */
  @Override
  public void setImageBounds(RectF imageBounds) {
    if (!imageBounds.equals(mImageBounds)) {
      mImageBounds.set(imageBounds);
      onTransformChanged();
    }
  }

  /** Gets the non-transformed image bounds, in view-absolute coordinates. */
  public RectF getImageBounds() {
    return mImageBounds;
  }

  /** Gets the transformed image bounds, in view-absolute coordinates */
  private RectF getTransformedImageBounds() {
    return mTransformedImageBounds;
  }

  /** Sets the view bounds. */
  @Override
  public void setViewBounds(RectF viewBounds) {
    mViewBounds.set(viewBounds);
  }

  /** Gets the view bounds. */
  public RectF getViewBounds() {
    return mViewBounds;
  }

  /**
   * Returns true if the zoomable transform is identity matrix.
   */
  @Override
  public boolean isIdentity() {
    return isMatrixIdentity(mActiveTransform, 1e-3f);
  }

  /**
   * Gets the matrix that transforms image-absolute coordinates to view-absolute coordinates.
   * The zoomable transformation is taken into account.
   *
   * Internal matrix is exposed for performance reasons and is not to be modified by the callers.
   */
  @Override
  public Matrix getTransform() {
    return mActiveTransform;
  }

  /**
   * Gets the matrix that transforms image-relative coordinates to view-absolute coordinates.
   * The zoomable transformation is taken into account.
   */
  public void getImageRelativeToViewAbsoluteTransform(Matrix outMatrix) {
    outMatrix.setRectToRect(IDENTITY_RECT, mTransformedImageBounds, Matrix.ScaleToFit.FILL);
  }

  /**
   * Maps point from view-absolute to image-relative coordinates.
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
   * Maps point from image-relative to view-absolute coordinates.
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
   * Maps array of 2D points from view-absolute to image-relative coordinates.
   * This does NOT take into account the zoomable transformation.
   * Points are represented by a float array of [x0, y0, x1, y1, ...].
   *
   * @param destPoints destination array (may be the same as source array)
   * @param srcPoints source array
   * @param numPoints number of points to map
   */
  private void mapAbsoluteToRelative(float[] destPoints, float[] srcPoints, int numPoints) {
    for (int i = 0; i < numPoints; i++) {
      destPoints[i * 2 + 0] = (srcPoints[i * 2 + 0] - mImageBounds.left) / mImageBounds.width();
      destPoints[i * 2 + 1] = (srcPoints[i * 2 + 1] - mImageBounds.top)  / mImageBounds.height();
    }
  }

  /**
   * Maps array of 2D points from image-relative to view-absolute coordinates.
   * This does NOT take into account the zoomable transformation.
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
   * Zooms to the desired scale and positions the image so that the given image point corresponds
   * to the given view point.
   *
   * @param scale desired scale, will be limited to {min, max} scale factor
   * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
   * @param viewPoint 2D point in view's absolute coordinate system
   */
  public void zoomToPoint(float scale, PointF imagePoint, PointF viewPoint) {
    FLog.v(TAG, "zoomToPoint");
    calculateZoomToPointTransform(mActiveTransform, scale, imagePoint, viewPoint, LIMIT_ALL);
    onTransformChanged();
  }

  /**
   * Calculates the zoom transformation that would zoom to the desired scale and position the image
   * so that the given image point corresponds to the given view point.
   *
   * @param outTransform the matrix to store the result to
   * @param scale desired scale, will be limited to {min, max} scale factor
   * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
   * @param viewPoint 2D point in view's absolute coordinate system
   * @param limitFlags whether to limit translation and/or scale.
   * @return whether or not the transform has been corrected due to limitation
   */
  protected boolean calculateZoomToPointTransform(
      Matrix outTransform,
      float scale,
      PointF imagePoint,
      PointF viewPoint,
      @LimitFlag int limitFlags) {
    float[] viewAbsolute = mTempValues;
    viewAbsolute[0] = imagePoint.x;
    viewAbsolute[1] = imagePoint.y;
    mapRelativeToAbsolute(viewAbsolute, viewAbsolute, 1);
    float distanceX = viewPoint.x - viewAbsolute[0];
    float distanceY = viewPoint.y - viewAbsolute[1];
    boolean transformCorrected = false;
    outTransform.setScale(scale, scale, viewAbsolute[0], viewAbsolute[1]);
    transformCorrected |= limitScale(outTransform, viewAbsolute[0], viewAbsolute[1], limitFlags);
    outTransform.postTranslate(distanceX, distanceY);
    transformCorrected |= limitTranslation(outTransform, limitFlags);
    return transformCorrected;
  }

  /** Sets a new zoom transformation. */
  public void setTransform(Matrix newTransform) {
    FLog.v(TAG, "setTransform");
    mActiveTransform.set(newTransform);
    onTransformChanged();
  }

  /** Gets the gesture detector. */
  protected TransformGestureDetector getDetector() {
    return mGestureDetector;
  }

  /** Notifies controller of the received touch event.  */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    FLog.v(TAG, "onTouchEvent: action: ", event.getAction());
    if (mIsEnabled) {
      return mGestureDetector.onTouchEvent(event);
    }
    return false;
  }

  /* TransformGestureDetector.Listener methods  */

  @Override
  public void onGestureBegin(TransformGestureDetector detector) {
    FLog.v(TAG, "onGestureBegin");
    mPreviousTransform.set(mActiveTransform);
  }

  @Override
  public void onGestureUpdate(TransformGestureDetector detector) {
    FLog.v(TAG, "onGestureUpdate");
    boolean transformCorrected = calculateGestureTransform(mActiveTransform, LIMIT_ALL);
    onTransformChanged();
    if (transformCorrected) {
      mGestureDetector.restartGesture();
    }
  }

  @Override
  public void onGestureEnd(TransformGestureDetector detector) {
    FLog.v(TAG, "onGestureEnd");
  }

  /**
   * Calculates the zoom transformation based on the current gesture.
   *
   * @param outTransform the matrix to store the result to
   * @param limitTypes whether to limit translation and/or scale.
   * @return whether or not the transform has been corrected due to limitation
   */
  protected boolean calculateGestureTransform(
      Matrix outTransform,
      @LimitFlag int limitTypes) {
    TransformGestureDetector detector = mGestureDetector;
    boolean transformCorrected = false;
    outTransform.set(mPreviousTransform);
    if (mIsRotationEnabled) {
      float angle = detector.getRotation() * (float) (180 / Math.PI);
      outTransform.postRotate(angle, detector.getPivotX(), detector.getPivotY());
    }
    if (mIsScaleEnabled) {
      float scale = detector.getScale();
      outTransform.postScale(scale, scale, detector.getPivotX(), detector.getPivotY());
    }
    transformCorrected |=
        limitScale(outTransform, detector.getPivotX(), detector.getPivotY(), limitTypes);
    if (mIsTranslationEnabled) {
      outTransform.postTranslate(detector.getTranslationX(), detector.getTranslationY());
    }
    transformCorrected |= limitTranslation(outTransform, limitTypes);
    return transformCorrected;
  }

  private void onTransformChanged() {
    mActiveTransform.mapRect(mTransformedImageBounds, mImageBounds);
    if (mListener != null && isEnabled()) {
      mListener.onTransformChanged(mActiveTransform);
    }
  }

  /**
   * Keeps the scaling factor within the specified limits.
   *
   * @param pivotX x coordinate of the pivot point
   * @param pivotY y coordinate of the pivot point
   * @param limitTypes whether to limit scale.
   * @return whether limiting has been applied or not
   */
  private boolean limitScale(
      Matrix transform,
      float pivotX,
      float pivotY,
      @LimitFlag int limitTypes) {
    if (!shouldLimit(limitTypes, LIMIT_SCALE)) {
      return false;
    }
    float currentScale = getMatrixScaleFactor(transform);
    float targetScale = limit(currentScale, mMinScaleFactor, mMaxScaleFactor);
    if (targetScale != currentScale) {
      float scale = targetScale / currentScale;
      transform.postScale(scale, scale, pivotX, pivotY);
      return true;
    }
    return false;
  }

  /**
   * Limits the translation so that there are no empty spaces on the sides if possible.
   *
   * <p> The image is attempted to be centered within the view bounds if the transformed image is
   * smaller. There will be no empty spaces within the view bounds if the transformed image is
   * bigger. This applies to each dimension (horizontal and vertical) independently.
   *
   * @param limitTypes whether to limit translation along the specific axis.
   * @return whether limiting has been applied or not
   */
  private boolean limitTranslation(Matrix transform, @LimitFlag int limitTypes) {
    if (!shouldLimit(limitTypes, LIMIT_TRANSLATION_X | LIMIT_TRANSLATION_Y)) {
      return false;
    }
    RectF b = mTempRect;
    b.set(mImageBounds);
    transform.mapRect(b);
    float offsetLeft = shouldLimit(limitTypes, LIMIT_TRANSLATION_X) ?
        getOffset(b.left, b.right, mViewBounds.left, mViewBounds.right, mImageBounds.centerX()) : 0;
    float offsetTop = shouldLimit(limitTypes, LIMIT_TRANSLATION_Y) ?
        getOffset(b.top, b.bottom, mViewBounds.top, mViewBounds.bottom, mImageBounds.centerY()) : 0;
    if (offsetLeft != 0 || offsetTop != 0) {
      transform.postTranslate(offsetLeft, offsetTop);
      return true;
    }
    return false;
  }

  /**
   * Checks whether the specified limit flag is present in the limits provided.
   *
   * <p> If the flag contains multiple flags together using a bitwise OR, this only checks that at
   * least one of the flags is included.
   *
   * @param limits the limits to apply
   * @param flag the limit flag(s) to check for
   * @return true if the flag (or one of the flags) is included in the limits
   */
  private static boolean shouldLimit(@LimitFlag int limits, @LimitFlag int flag) {
    return (limits & flag) != LIMIT_NONE;
  }

  /**
   * Returns the offset necessary to make sure that:
   * - the image is centered within the limit if the image is smaller than the limit
   * - there is no empty space on left/right if the image is bigger than the limit
   */
  private float getOffset(
      float imageStart,
      float imageEnd,
      float limitStart,
      float limitEnd,
      float limitCenter) {
    float imageWidth = imageEnd - imageStart, limitWidth = limitEnd - limitStart;
    float limitInnerWidth = Math.min(limitCenter - limitStart, limitEnd - limitCenter) * 2;
    // center if smaller than limitInnerWidth
    if (imageWidth < limitInnerWidth) {
      return limitCenter - (imageEnd + imageStart) / 2;
    }
    // to the edge if in between and limitCenter is not (limitLeft + limitRight) / 2
    if (imageWidth < limitWidth) {
      if (limitCenter < (limitStart + limitEnd) / 2) {
        return limitStart - imageStart;
      } else {
        return limitEnd - imageEnd;
      }
    }
    // to the edge if larger than limitWidth and empty space visible
    if (imageStart > limitStart) {
      return limitStart - imageStart;
    }
    if (imageEnd < limitEnd) {
      return limitEnd - imageEnd;
    }
    return 0;
  }

  /**
   * Limits the value to the given min and max range.
   */
  private float limit(float value, float min, float max) {
    return Math.min(Math.max(min, value), max);
  }

  /**
   * Gets the scale factor for the given matrix.
   * This method assumes the equal scaling factor for X and Y axis.
   */
  private float getMatrixScaleFactor(Matrix transform) {
    transform.getValues(mTempValues);
    return mTempValues[Matrix.MSCALE_X];
  }

  /**
   * Same as {@code Matrix.isIdentity()}, but with tolerance {@code eps}.
   */
  private boolean isMatrixIdentity(Matrix transform, float eps) {
    // Checks whether the given matrix is close enough to the identity matrix:
    //   1 0 0
    //   0 1 0
    //   0 0 1
    // Or equivalently to the zero matrix, after subtracting 1.0f from the diagonal elements:
    //   0 0 0
    //   0 0 0
    //   0 0 0
    transform.getValues(mTempValues);
    mTempValues[0] -= 1.0f; // m00
    mTempValues[4] -= 1.0f; // m11
    mTempValues[8] -= 1.0f; // m22
    for (int i = 0; i < 9; i++) {
      if (Math.abs(mTempValues[i]) > eps) {
        return false;
      }
    }
    return true;
  }
}
