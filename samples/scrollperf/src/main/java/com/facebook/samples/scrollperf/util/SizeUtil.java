/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.conf.Const;

/** Utility class for resizing */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class SizeUtil {

  public static int DISPLAY_WIDTH;
  public static int DISPLAY_HEIGHT;

  /**
   * Update the LayoutParams of the given View
   *
   * @param view The View to layout
   * @param width The wanted width
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
   * @param context The Context
   * @param parentWidth The width of the Parent View
   * @param parentHeight The height of the Parent View
   * @return The desired size for the View
   */
  public static int calcDesiredSize(Context context, int parentWidth, int parentHeight) {
    int orientation = context.getResources().getConfiguration().orientation;
    int desiredSize =
        (orientation == Configuration.ORIENTATION_LANDSCAPE) ? parentWidth : parentHeight;
    return Math.min(desiredSize, parentWidth);
  }

  /**
   * Utility method which set the size based on the parent and configurations
   *
   * @param parentView The parent View
   * @param draweeView The View to resize
   * @param config The Config object
   */
  public static void setConfiguredSize(
      final View parentView, final View draweeView, final Config config) {
    if (parentView != null) {
      if (config.overrideSize) {
        SizeUtil.updateViewLayoutParams(draweeView, config.overridenWidth, config.overridenHeight);
      } else {
        int size =
            SizeUtil.calcDesiredSize(
                parentView.getContext(), parentView.getWidth(), parentView.getHeight());
        SizeUtil.updateViewLayoutParams(draweeView, size, (int) (size / Const.RATIO));
      }
    }
  }

  /**
   * Invoke one into the Activity to get info about the Display size
   *
   * @param activity The Activity
   */
  public static void initSizeData(Activity activity) {
    DisplayMetrics metrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
    DISPLAY_WIDTH = metrics.widthPixels;
    DISPLAY_HEIGHT = metrics.heightPixels;
  }

  public static int dpToPx(Context context, int dp) {
    return (int)
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
  }
}
