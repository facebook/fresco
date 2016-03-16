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
import android.view.animation.DecelerateInterpolator;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.samples.gestures.TransformGestureDetector;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;

/**
 * ZoomableController that adds animation capabilities to DefaultZoomableController.
 */
public class AnimatedZoomableController extends DefaultZoomableController {

  private static final Class<?> TAG = AnimatedZoomableController.class;

  private boolean mIsAnimating;
  private final ValueAnimator mValueAnimator;
  private final float[] mStartValues = new float[9];
  private final float[] mStopValues = new float[9];
  private final float[] mCurrentValues = new float[9];
  private final Matrix mNewTransform = new Matrix();
  private final Matrix mWorkingTransform = new Matrix();

  public static AnimatedZoomableController newInstance() {
    return new AnimatedZoomableController(TransformGestureDetector.newInstance());
  }

  public AnimatedZoomableController(TransformGestureDetector transformGestureDetector) {
    super(transformGestureDetector);
    mValueAnimator = ValueAnimator.ofFloat(0, 1);
    mValueAnimator.setInterpolator(new DecelerateInterpolator());
  }

  @Override
  public void reset() {
    FLog.v(TAG, "reset");
    stopAnimation();
    mWorkingTransform.reset();
    mNewTransform.reset();
    super.reset();
  }

  /**
   * Returns true if the zoomable transform is identity matrix, and the controller is idle.
   */
  @Override
  public boolean isIdentity() {
    return !isAnimating() && super.isIdentity();
  }

  /**
   * Zooms to the desired scale and positions the image so that the given image point corresponds
   * to the given view point.
   *
   * <p>If this method is called while an animation or gesture is already in progress,
   * the current animation or gesture will be stopped first.
   *
   * @param scale desired scale, will be limited to {min, max} scale factor
   * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
   * @param viewPoint 2D point in view's absolute coordinate system
   */
  @Override
  public void zoomToPoint(
      float scale,
      PointF imagePoint,
      PointF viewPoint) {
    zoomToPoint(scale, imagePoint, viewPoint, LIMIT_ALL, 0, null);
  }

  /**
   * Zooms to the desired scale and positions the image so that the given image point corresponds
   * to the given view point.
   *
   * <p>If this method is called while an animation or gesture is already in progress,
   * the current animation or gesture will be stopped first.
   *
   * @param scale desired scale, will be limited to {min, max} scale factor
   * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
   * @param viewPoint 2D point in view's absolute coordinate system
   * @param limitFlags whether to limit translation and/or scale.
   * @param durationMs length of animation of the zoom, or 0 if no animation desired
   * @param onAnimationComplete code to run when the animation completes. Ignored if durationMs=0
   */
  public void zoomToPoint(
      float scale,
      PointF imagePoint,
      PointF viewPoint,
      @LimitFlag int limitFlags,
      long durationMs,
      @Nullable Runnable onAnimationComplete) {
    FLog.v(TAG, "zoomToPoint: duration %d ms", durationMs);
    calculateZoomToPointTransform(
        mNewTransform,
        scale,
        imagePoint,
        viewPoint,
        limitFlags);
    setTransform(mNewTransform, durationMs, onAnimationComplete);
  }

  /**
   * Sets a new zoomable transformation and animates to it if desired.
   *
   * <p>If this method is called while an animation or gesture is already in progress,
   * the current animation or gesture will be stopped first.
   *
   * @param newTransform new transform to make active
   * @param durationMs duration of the animation, or 0 to not animate
   * @param onAnimationComplete code to run when the animation completes. Ignored if durationMs=0
   */
  public void setTransform(
      Matrix newTransform,
      long durationMs,
      @Nullable Runnable onAnimationComplete) {
    FLog.v(TAG, "setTransform: duration %d ms", durationMs);
    if (durationMs <= 0) {
      setTransformImmediate(newTransform);
    } else {
      setTransformAnimated(newTransform, durationMs, onAnimationComplete);
    }
  }

  private void setTransformImmediate(final Matrix newTransform) {
    FLog.v(TAG, "setTransformImmediate");
    stopAnimation();
    mWorkingTransform.set(newTransform);
    super.setTransform(newTransform);
    getDetector().restartGesture();
  }

  private void setTransformAnimated(
      final Matrix newTransform,
      long durationMs,
      @Nullable final Runnable onAnimationComplete) {
    FLog.v(TAG, "setTransformAnimated: duration %d ms", durationMs);
    stopAnimation();
    Preconditions.checkArgument(durationMs > 0);
    Preconditions.checkState(!isAnimating());
    mIsAnimating = true;
    mValueAnimator.setDuration(durationMs);
    getTransform().getValues(mStartValues);
    newTransform.getValues(mStopValues);
    mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator valueAnimator) {
        calculateInterpolation(mWorkingTransform, (float) valueAnimator.getAnimatedValue());
        AnimatedZoomableController.super.setTransform(mWorkingTransform);
      }
    });
    mValueAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationCancel(Animator animation) {
        FLog.v(TAG, "setTransformAnimated: animation cancelled");
        onAnimationStopped();
      }
      @Override
      public void onAnimationEnd(Animator animation) {
        FLog.v(TAG, "setTransformAnimated: animation finished");
        onAnimationStopped();
      }
      private void onAnimationStopped() {
        if (onAnimationComplete != null) {
          onAnimationComplete.run();
        }
        mIsAnimating = false;
        getDetector().restartGesture();
      }
    });
    mValueAnimator.start();
  }

  private void stopAnimation() {
    if (!mIsAnimating) {
      return;
    }
    FLog.v(TAG, "stopAnimation");
    mValueAnimator.cancel();
    mValueAnimator.removeAllUpdateListeners();
    mValueAnimator.removeAllListeners();
  }

  private boolean isAnimating() {
    return mIsAnimating;
  }

  @Override
  public void onGestureBegin(TransformGestureDetector detector) {
    FLog.v(TAG, "onGestureBegin");
    stopAnimation();
    super.onGestureBegin(detector);
  }

  @Override
  public void onGestureUpdate(TransformGestureDetector detector) {
    FLog.v(TAG, "onGestureUpdate %s", isAnimating() ? "(ignored)" : "");
    if (isAnimating()) {
      return;
    }
    super.onGestureUpdate(detector);
  }

  private void calculateInterpolation(Matrix outMatrix, float fraction) {
    for (int i = 0; i < 9; i++) {
      mCurrentValues[i] = (1 - fraction) * mStartValues[i] + fraction * mStopValues[i];
    }
    outMatrix.setValues(mCurrentValues);
  }
}
