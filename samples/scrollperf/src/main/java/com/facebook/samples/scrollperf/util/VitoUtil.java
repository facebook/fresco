/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.util;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.conf.Const;

/** Utility class about Vito */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class VitoUtil {

  /**
   * Creates the Hierarchy using the information into the Config
   *
   * @param context The Context
   * @param config The Config object
   * @return The Hierarchy to use
   */
  public static ImageOptions.Builder createImageOptions(
      final Context context, final Config config) {
    FrescoSystrace.beginSection("VitoUtil#createVitoHierarchy");
    ImageOptions.Builder builder =
        ImageOptions.create()
            .fadeDurationMs(config.fadeDurationMs)
            .placeholderColor(Const.PLACEHOLDER_COLOR)
            .errorColor(Const.FAILURE_COLOR)
            .scale(ScalingUtils.ScaleType.FIT_CENTER);
    applyScaleType(builder, config);

    if (config.useRoundedCorners || config.drawBorder) {
      final Resources res = context.getResources();

      if (config.useRoundedCorners) {
        if (config.useRoundedAsCircle) {
          builder.round(RoundingOptions.asCircle());
        } else {
          builder.round(
              RoundingOptions.forCornerRadiusPx(
                  res.getDimensionPixelSize(R.dimen.vito_corner_radius)));
        }
      }

      if (config.drawBorder) {
        builder.borders(
            BorderOptions.create(
                res.getColor(R.color.colorPrimary),
                res.getDimensionPixelSize(R.dimen.vito_border_width)));
      }
    }
    FrescoSystrace.endSection();
    return builder;
  }

  public static void applyScaleType(ImageOptions.Builder builder, final Config config) {
    switch (config.scaleType) {
      case "scale_type_none":
        builder.scale(null);
        break;
      case "scale_type_center":
        builder.scale(ScalingUtils.ScaleType.CENTER);
        break;
      case "scale_type_center_crop":
        builder.scale(ScalingUtils.ScaleType.CENTER_CROP);
        break;
      case "scale_type_center_inside":
        builder.scale(ScalingUtils.ScaleType.CENTER_INSIDE);
        break;
      case "scale_type_fit_center":
        builder.scale(ScalingUtils.ScaleType.FIT_CENTER);
        break;
      case "scale_type_fit_start":
        builder.scale(ScalingUtils.ScaleType.FIT_START);
        break;
      case "scale_type_fit_end":
        builder.scale(ScalingUtils.ScaleType.FIT_END);
        break;
      case "scale_type_fit_xy":
        builder.scale(ScalingUtils.ScaleType.FIT_XY);
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
