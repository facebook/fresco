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
import com.facebook.common.logging.FLog;
import com.facebook.samples.gestures.TransformGestureDetector;
import javax.annotation.Nullable;

/**
 * Abstract class for ZoomableController that adds animation capabilities to
 * DefaultZoomableController.
 */
public abstract class AbstractAnimatedZoomableController extends DefaultZoomableController {

  private boolean mIsAnimating;
  private final float[] mStartValues = new float[9];
  private final float[] mStopValues = new float[9];
  private final float[] mCurrentValues = new float[9];
  private final Matrix mNewTransform = new Matrix();
  private final Matrix mWorkingTransform = new Matrix();


  public AbstractAnimatedZoomableController(TransformGestureDetector transformGestureDetector) {
    super(transformGestureDetector);
  }

  @Override
  public void reset() {
    FLog.v(getLogTag(), "reset");
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
    FLog.v(getLogTag(), "zoomToPoint: duration %d ms", durationMs);
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
    FLog.v(getLogTag(), "setTransform: duration %d ms", durationMs);
    if (durationMs <= 0) {
      setTransformImmediate(newTransform);
    } else {
      setTransformAnimated(newTransform, durationMs, onAnimationComplete);
    }
  }

  private void setTransformImmediate(final Matrix newTransform) {
    FLog.v(getLogTag(), "setTransformImmediate");
    stopAnimation();
    mWorkingTransform.set(newTransform);
    super.setTransform(newTransform);
    getDetector().restartGesture();
  }

  protected boolean isAnimating() {
    return mIsAnimating;
  }

  protected void setAnimating(boolean isAnimating) {
    mIsAnimating = isAnimating;
  }

  protected float[] getStartValues() {
    return mStartValues;
  }

  protected float[] getStopValues() {
    return mStopValues;
  }

  protected Matrix getWorkingTransform() {
    return mWorkingTransform;
  }

  @Override
  public void onGestureBegin(TransformGestureDetector detector) {
    FLog.v(getLogTag(), "onGestureBegin");
    stopAnimation();
    super.onGestureBegin(detector);
  }

  @Override
  public void onGestureUpdate(TransformGestureDetector detector) {
    FLog.v(getLogTag(), "onGestureUpdate %s", isAnimating() ? "(ignored)" : "");
    if (isAnimating()) {
      return;
    }
    super.onGestureUpdate(detector);
  }

  protected void calculateInterpolation(Matrix outMatrix, float fraction) {
    for (int i = 0; i < 9; i++) {
      mCurrentValues[i] = (1 - fraction) * mStartValues[i] + fraction * mStopValues[i];
    }
    outMatrix.setValues(mCurrentValues);
  }

  public abstract void setTransformAnimated(
      final Matrix newTransform,
      long durationMs,
      @Nullable final Runnable onAnimationComplete);

  protected abstract void stopAnimation();

  protected abstract Class<?> getLogTag();
}
