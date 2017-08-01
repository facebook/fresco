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

package com.facebook.samples.scrollperf.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.conf.Const;

/**
 * Utility class for resizing
 */
public final class SizeUtil {

  public static int DISPLAY_WIDTH;
  public static int DISPLAY_HEIGHT;

  /**
   * Update the LayoutParams of the given View
   *
   * @param view   The View to layout
   * @param width  The wanted width
   * @param height The wanted height
   */
  public static void updateViewLayoutParams(View view, int width, int height) {
    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
    if (layoutParams == null || layoutParams.height != width || layoutParams.width != height) {
      layoutParams = new AbsListView.LayoutParams(width, height);
      view.setLayoutParams(layoutParams);
    }
  }

  /**
   * Calculate desired size for the given View based on device orientation
   *
   * @param context      The Context
   * @param parentWidth  The width of the Parent View
   * @param parentHeight The height of the Parent View
   * @return The desired size for the View
   */
  public static int calcDesiredSize(Context context, int parentWidth, int parentHeight) {
    int orientation = context.getResources().getConfiguration().orientation;
    int desiredSize = (orientation == Configuration.ORIENTATION_LANDSCAPE) ?
            parentWidth : parentHeight;
    return Math.min(desiredSize, parentWidth);
  }

  /**
   * Utility method which set the size based on the parent and configurations
   * @param parentView The parent View
   * @param draweeView The View to resize
   * @param config The Config object
   */
  public static void setConfiguredSize(
      final View parentView,
      final View draweeView,
      final Config config) {
    if (parentView != null) {
      if (config.overrideSize) {
        SizeUtil.updateViewLayoutParams(
            draweeView,
            config.overridenWidth,
            config.overridenHeight);
      } else {
        int size = SizeUtil.calcDesiredSize(
            parentView.getContext(),
            parentView.getWidth(),
            parentView.getHeight());
        SizeUtil.updateViewLayoutParams(draweeView, size, (int) (size / Const.RATIO));
      }
    }
  }

  /**
   * Invoke one into the Activity to get info about the Display size
   * @param activity The Activity
   */
  public static void initSizeData(Activity activity) {
    DisplayMetrics metrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
    DISPLAY_WIDTH = metrics.widthPixels;
    DISPLAY_HEIGHT = metrics.heightPixels;
  }

  public static int dpToPx(Context context, int dp) {
    return (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.getResources().getDisplayMetrics());
  }
}
