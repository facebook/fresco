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

import android.content.Context;
import android.graphics.Color;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.conf.Const;

/**
 * Utility class about Drawee
 */
public final class DraweeUtil {

  /**
   * Creates the Hierarchy using the information into the Config
   *
   * @param context The Context
   * @param config  The Config object
   * @return The Hierarchy to use
   */
  public static GenericDraweeHierarchy createDraweeHierarchy(
          final Context context,
          final Config config) {
    GenericDraweeHierarchyBuilder builder =
            new GenericDraweeHierarchyBuilder(context.getResources())
            .setPlaceholderImage(Const.PLACEHOLDER)
            .setFailureImage(Const.FAILURE)
            .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
    applyScaleType(builder, config);
    if (config.useRoundedCorners) {
      // Will add conf params later about this
      final RoundingParams roundingParams = new RoundingParams()
              .setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY)
              .setBorderColor(Color.RED)
              .setBorderWidth(2.0f)
              .setCornersRadius(80.0f);
      roundingParams.setRoundAsCircle(config.useRoundedAsCircle);
      builder.setRoundingParams(roundingParams);
    }
    return builder.build();
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
}
