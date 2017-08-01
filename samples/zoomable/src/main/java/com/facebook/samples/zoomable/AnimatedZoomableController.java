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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.view.animation.DecelerateInterpolator;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.samples.gestures.TransformGestureDetector;
import javax.annotation.Nullable;

/**
 * ZoomableController that adds animation capabilities to DefaultZoomableController using standard
 * Android animation classes
 */
public class AnimatedZoomableController extends AbstractAnimatedZoomableController {

  private static final Class<?> TAG = AnimatedZoomableController.class;

  private final ValueAnimator mValueAnimator;

  public static AnimatedZoomableController newInstance() {
    return new AnimatedZoomableController(TransformGestureDetector.newInstance());
  }

  @SuppressLint("NewApi")
  public AnimatedZoomableController(TransformGestureDetector transformGestureDetector) {
    super(transformGestureDetector);
    mValueAnimator = ValueAnimator.ofFloat(0, 1);
    mValueAnimator.setInterpolator(new DecelerateInterpolator());
  }

  @SuppressLint("NewApi")
  @Override
  public void setTransformAnimated(
      final Matrix newTransform,
      long durationMs,
      @Nullable final Runnable onAnimationComplete) {
    FLog.v(getLogTag(), "setTransformAnimated: duration %d ms", durationMs);
    stopAnimation();
    Preconditions.checkArgument(durationMs > 0);
    Preconditions.checkState(!isAnimating());
    setAnimating(true);
    mValueAnimator.setDuration(durationMs);
    getTransform().getValues(getStartValues());
    newTransform.getValues(getStopValues());
    mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator valueAnimator) {
        calculateInterpolation(getWorkingTransform(), (float) valueAnimator.getAnimatedValue());
        AnimatedZoomableController.super.setTransform(getWorkingTransform());
      }
    });
    mValueAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationCancel(Animator animation) {
        FLog.v(getLogTag(), "setTransformAnimated: animation cancelled");
        onAnimationStopped();
      }
      @Override
      public void onAnimationEnd(Animator animation) {
        FLog.v(getLogTag(), "setTransformAnimated: animation finished");
        onAnimationStopped();
      }
      private void onAnimationStopped() {
        if (onAnimationComplete != null) {
          onAnimationComplete.run();
        }
        setAnimating(false);
        getDetector().restartGesture();
      }
    });
    mValueAnimator.start();
  }

  @SuppressLint("NewApi")
  @Override
  public void stopAnimation() {
    if (!isAnimating()) {
      return;
    }
    FLog.v(getLogTag(), "stopAnimation");
    mValueAnimator.cancel();
    mValueAnimator.removeAllUpdateListeners();
    mValueAnimator.removeAllListeners();
  }

  @Override
  protected Class<?> getLogTag() {
    return TAG;
  }

}
