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

import javax.annotation.Nullable;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import com.facebook.common.internal.Preconditions;
import com.facebook.samples.gestures.TransformGestureDetector;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;

/**
 * Zoomable controller that calculates transformation based on touch events.
 */
public class DefaultZoomableController
    implements ZoomableController, TransformGestureDetector.Listener {

  private static final RectF IDENTITY_RECT = new RectF(0, 0, 1, 1);

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
  private final RectF mTempRect = new RectF();

  private final ValueAnimator mValueAnimator;
  private final float[] mAnimationStartValues = new float[9];
  private final float[] mAnimationDestValues = new float[9];
  private final float[] mAnimationCurrValues = new float[9];
  private final Matrix mNewTransform = new Matrix();

  public DefaultZoomableController(TransformGestureDetector gestureDetector) {
    mGestureDetector = gestureDetector;
    mGestureDetector.setListener(this);
    mValueAnimator = ValueAnimator.ofFloat(0, 1);
    mValueAnimator.setInterpolator(new DecelerateInterpolator());
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
    onTransformChanged();
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

  protected RectF getTransformedImageBounds() {
    return mTransformedImageBounds;
  }

  /** Sets the image bounds before zoomable transformation is applied. */
  @Override
  public void setImageBounds(RectF imageBounds) {
    if (!imageBounds.equals(mImageBounds)) {
      mImageBounds.set(imageBounds);
      onTransformChanged();
    }
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
      destPoints[i * 2 + 1] = (srcPoints[i * 2 + 1] - mImageBounds.top)  / mImageBounds.height();
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
   * Returns the matrix that fully transforms the image from image-relative coordinates
   * to scaled view-absolute coordinates.
   */
  public void getImageRelativeToViewAbsoluteTransform(Matrix outMatrix) {
    mActiveTransform.mapRect(mTempRect, mImageBounds);
    outMatrix.setRectToRect(IDENTITY_RECT, mTempRect, Matrix.ScaleToFit.FILL);
  }

  // TODO(balazsbalazs) resolve issues with interrupting an existing animation/gesture with
  // a new animation or transform

  /**
   * Sets a new zoom transformation.
   *
   * <p>If this method is called while an animation or gesture is already in progress,
   * this will currently result in undefined behavior.
   */
  public void setTransform(Matrix newTransform) {
    setTransform(newTransform, 0, null);
  }

  /**
   * Sets a new zoomable transformation and animates to it if desired.
   *
   * <p>If this method is called while an animation or gesture is already in progress,
   * this will currently result in undefined behavior.
   *
   * @param newTransform new transform to make active
   * @param durationMs duration of the animation, or 0 to not animate
   * @param onAnimationComplete code to run when the animation completes. Ignored if durationMs=0
   */
  public void setTransform(
      Matrix newTransform,
      long durationMs,
      @Nullable Runnable onAnimationComplete) {
    if (mGestureDetector.isGestureInProgress()) {
      mGestureDetector.reset();
    }
    cancelAnimation();
    if (durationMs <= 0) {
      mActiveTransform.set(newTransform);
      onTransformChanged();
    } else {
      setTransformAnimated(newTransform, durationMs, onAnimationComplete);
    }
  }

  /** Do not call this method directly; call it only from setTransform. */
  private void setTransformAnimated(
      final Matrix newTransform,
      long durationMs,
      @Nullable final Runnable onAnimationComplete) {
    Preconditions.checkArgument(durationMs > 0);
    Preconditions.checkState(!mValueAnimator.isRunning());
    mValueAnimator.setDuration(durationMs);
    mActiveTransform.getValues(mAnimationStartValues);
    newTransform.getValues(mAnimationDestValues);
    mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator valueAnimator) {
        float fraction = (float) valueAnimator.getAnimatedValue();
        for (int i = 0; i < mAnimationCurrValues.length; i++) {
          mAnimationCurrValues[i] = (1 - fraction) * mAnimationStartValues[i] +
              fraction * mAnimationDestValues[i];
        }
        mActiveTransform.setValues(mAnimationCurrValues);
        onTransformChanged();
      }
    });
    if (onAnimationComplete != null) {
      mValueAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          onAnimationComplete.run();
        }
      });
    }
    mValueAnimator.start();
  }

  private void cancelAnimation() {
    mValueAnimator.removeAllUpdateListeners();
    mValueAnimator.removeAllListeners();
    if (mValueAnimator.isRunning()) {
      mValueAnimator.cancel();
    }
  }

  /** Notifies controller of the received touch event.  */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mIsEnabled) {
      return mGestureDetector.onTouchEvent(event);
    }
    return false;
  }

  protected void onTransformChanged() {
    mActiveTransform.mapRect(mTransformedImageBounds, mImageBounds);
    if (mListener != null && isEnabled()) {
      mListener.onTransformChanged(mActiveTransform);
    }
  }

  /**
   * Zooms to the desired scale and positions the view so that imagePoint is in the center.
   *
   * <p>If this method is called while an animation or gesture is already in progress,
   * this will currently result in undefined behavior.
   *
   * @param scale desired scale, will be limited to {min, max} scale factor
   * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
   * @param viewPoint 2D point in view's absolute coordinate system
   * @param limitTransX  Whether to adjust the transform to prevent black bars from appearing on
   *                     the left or right.
   * @param limitTransY Whether to adjust the transform to prevent black bars from appearing on
   *                    the top or bottom.
   * @param durationMs length of animation of the zoom, or 0 if no animation desired
   * @param onAnimationComplete code to execute when the animation is complete.
   *                            Ignored if durationMs=0
   */
  public void zoomToImagePoint(
      float scale,
      PointF imagePoint,
      PointF viewPoint,
      boolean limitTransX,
      boolean limitTransY,
      long durationMs,
      @Nullable Runnable onAnimationComplete) {
    scale = limit(scale, mMinScaleFactor, mMaxScaleFactor);
    float[] viewAbsolute = mTempValues;
    viewAbsolute[0] = imagePoint.x;
    viewAbsolute[1] = imagePoint.y;
    mapRelativeToAbsolute(viewAbsolute, viewAbsolute, 1);
    float distanceX = viewPoint.x - viewAbsolute[0];
    float distanceY = viewPoint.y - viewAbsolute[1];
    mNewTransform.setScale(scale, scale, viewAbsolute[0], viewAbsolute[1]);
    mNewTransform.postTranslate(distanceX, distanceY);
    limitTranslation(mNewTransform, limitTransX, limitTransY);

    setTransform(mNewTransform, durationMs, onAnimationComplete);
  }

  /* TransformGestureDetector.Listener methods  */

  @Override
  public void onGestureBegin(TransformGestureDetector detector) {
    mPreviousTransform.set(mActiveTransform);
    // TODO(balazsbalazs): animation should be cancelled here
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
    if (limitTranslation(mActiveTransform, true, true)) {
      mGestureDetector.restartGesture();
    }
    onTransformChanged();
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
   * @param limitX whether to apply the limit on the x-axis
   * @param limitY whether to apply the limit on the y-axis
   * @return whether adjustments were needed or not
   */
  private boolean limitTranslation(Matrix newTransform, boolean limitX, boolean limitY) {
    RectF bounds = mTransformedImageBounds;
    bounds.set(mImageBounds);
    newTransform.mapRect(bounds);
    float offsetLeft = limitX ?
        getOffset(bounds.left, bounds.width(), mViewBounds.width()) : bounds.left;
    float offsetTop = limitY ?
        getOffset(bounds.top, bounds.height(), mViewBounds.height()) : bounds.top;
    if (offsetLeft != bounds.left || offsetTop != bounds.top) {
      newTransform.postTranslate(offsetLeft - bounds.left, offsetTop - bounds.top);
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
