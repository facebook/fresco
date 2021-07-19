/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.util;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.conf.Const;

/** Utility class about Drawee */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class DraweeUtil {

  /**
   * Creates the Hierarchy using the information into the Config
   *
   * @param context The Context
   * @param config The Config object
   * @return The Hierarchy to use
   */
  public static GenericDraweeHierarchy createDraweeHierarchy(
      final Context context, final Config config) {
    FrescoSystrace.beginSection("DraweeUtil#createDraweeHierarchy");
    GenericDraweeHierarchyBuilder builder =
        new GenericDraweeHierarchyBuilder(context.getResources())
            .setFadeDuration(config.fadeDurationMs)
            .setPlaceholderImage(Const.PLACEHOLDER)
            .setFailureImage(Const.FAILURE)
            .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
    applyScaleType(builder, config);

    if (config.useRoundedCorners || config.drawBorder) {
      final Resources res = context.getResources();
      final RoundingParams roundingParams = new RoundingParams();

      if (config.useRoundedCorners) {
        roundingParams.setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
        roundingParams.setCornersRadius(res.getDimensionPixelSize(R.dimen.drawee_corner_radius));
        roundingParams.setRoundAsCircle(config.useRoundedAsCircle);
      }

      if (config.drawBorder) {
        //noinspection deprecation
        roundingParams.setBorderColor(res.getColor(R.color.colorPrimary));
        roundingParams.setBorderWidth(res.getDimensionPixelSize(R.dimen.drawee_border_width));
      }

      builder.setRoundingParams(roundingParams);
    }
    GenericDraweeHierarchy result = builder.build();
    FrescoSystrace.endSection();
    return result;
  }

  public static void applyScaleType(GenericDraweeHierarchyBuilder builder, final Config config) {
    switch (config.scaleType) {
      case "scale_type_none":
        builder.setActualImageScaleType(null);
        break;
      case "scale_type_center":
        builder.setActualImageScaleType(ScalingUtils.ScaleType.CENTER);
        break;
      case "scale_type_center_crop":
        builder.setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
        break;
      case "scale_type_center_inside":
        builder.setActualImageScaleType(ScalingUtils.ScaleType.CENTER_INSIDE);
        break;
      case "scale_type_fit_center":
        builder.setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
        break;
      case "scale_type_fit_start":
        builder.setActualImageScaleType(ScalingUtils.ScaleType.FIT_START);
        break;
      case "scale_type_fit_end":
        builder.setActualImageScaleType(ScalingUtils.ScaleType.FIT_END);
        break;
      case "scale_type_fit_xy":
        builder.setActualImageScaleType(ScalingUtils.ScaleType.FIT_XY);
        break;
    }
  }

  /**
   * Utility method which set the bgColor based on configuration values
   *
   * @param view The View to change the bgColor to
   * @param config The Config object
   */
  public static void setBgColor(View view, final Config config) {
    int[] colors = view.getContext().getResources().getIntArray(R.array.bg_colors);
    final int bgColor = colors[config.bgColor];
    view.setBackgroundColor(bgColor);
  }
}
