/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options;

import com.facebook.common.internal.Objects;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Arrays;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class RoundingOptions {

  public static final float CORNER_RADIUS_UNSET = 0f;

  private static final RoundingOptions AS_CIRCLE =
      new RoundingOptions(true, CORNER_RADIUS_UNSET, null, false);

  private static final RoundingOptions AS_CIRCLE_ANTI_ALIASING =
      new RoundingOptions(true, CORNER_RADIUS_UNSET, null, true);

  public static RoundingOptions asCircle() {
    return AS_CIRCLE;
  }

  public static RoundingOptions asCircle(boolean antiAliasing) {
    return antiAliasing ? AS_CIRCLE_ANTI_ALIASING : AS_CIRCLE;
  }

  public static RoundingOptions forCornerRadiusPx(float cornerRadiusPx) {
    return new RoundingOptions(false, cornerRadiusPx, null, false);
  }

  public static RoundingOptions forCornerRadii(
      float topLeft, float topRight, float bottomRight, float bottomLeft) {
    float[] radii = new float[8];
    radii[0] = radii[1] = topLeft;
    radii[2] = radii[3] = topRight;
    radii[4] = radii[5] = bottomRight;
    radii[6] = radii[7] = bottomLeft;
    return new RoundingOptions(false, CORNER_RADIUS_UNSET, radii, false);
  }

  private final boolean mIsCircular;
  private final float mCornerRadius;
  private final @Nullable float[] mCornerRadii;
  private final boolean mAntiAliasing;

  private RoundingOptions(
      boolean isCircular,
      float cornerRadiusPx,
      @Nullable float[] cornerRadii,
      boolean antiAliasing) {
    mIsCircular = isCircular;
    mCornerRadius = cornerRadiusPx;
    mCornerRadii = cornerRadii;
    mAntiAliasing = antiAliasing;
  }

  public boolean isCircular() {
    return mIsCircular;
  }

  public boolean hasRoundedCorners() {
    return mCornerRadius != CORNER_RADIUS_UNSET || mCornerRadii != null;
  }

  public float getCornerRadius() {
    return mCornerRadius;
  }

  @Nullable
  public float[] getCornerRadii() {
    return mCornerRadii;
  }

  public boolean isAntiAliased() {
    return mAntiAliasing;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    RoundingOptions that = (RoundingOptions) obj;
    return mIsCircular == that.mIsCircular
        && mCornerRadius == that.mCornerRadius
        && Objects.equal(mCornerRadii, that.mCornerRadii)
        && mAntiAliasing == that.mAntiAliasing;
  }

  @Override
  public int hashCode() {
    int result = mIsCircular ? 1 : 0;
    result =
        31 * result
            + (mCornerRadius == CORNER_RADIUS_UNSET ? 0 : Float.floatToIntBits(mCornerRadius));
    result = 31 * result + (mCornerRadii != null ? Arrays.hashCode(mCornerRadii) : 0);
    result = 31 * result + (mAntiAliasing ? 1 : 0);
    return result;
  }
}
