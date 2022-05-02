/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport;

import androidx.annotation.Nullable;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class RoundingParamsWrapper {

  @Nullable
  public static RoundingOptions roundingOptionsFromRoundingParams(
      @Nullable RoundingParams roundingParams) {
    if (roundingParams == null) {
      return null;
    }
    if (roundingParams.getRoundAsCircle()) {
      return RoundingOptions.asCircle(true);
    }
    if (roundingParams.getCornersRadii() != null) {
      return RoundingOptions.forCornerRadii(roundingParams.getCornersRadii(), true);
    }
    // TODO: we also have an overlay color mode where you specify a color to be drawn on top. Vito
    // does not support this.
    return null; // Probably only used for borders
  }

  @Nullable
  public static BorderOptions borderOptionsFromRoundingParams(
      @Nullable RoundingParams roundingParams) {
    if (roundingParams == null || roundingParams.getBorderWidth() <= 0f) {
      return null;
    }
    return BorderOptions.create(
        roundingParams.getBorderColor(),
        roundingParams.getBorderWidth(),
        roundingParams.getPadding(),
        roundingParams.getScaleDownInsideBorders());
  }
}
