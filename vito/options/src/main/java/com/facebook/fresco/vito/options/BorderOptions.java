/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options;

import androidx.annotation.ColorInt;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Nullsafe(Nullsafe.Mode.STRICT)
@Immutable
public class BorderOptions {

  /**
   * Create border options without padding
   *
   * @param color The color of the border
   * @param width The width of the border, in pixels
   * @return BorderOptions
   */
  public static BorderOptions create(@ColorInt int color, float width) {
    return new BorderOptions(color, width);
  }

  /**
   * Create border options with padding. Note that currently padding is not supported with
   * RoundingOptions.asCircle().
   *
   * @param color The color of the border
   * @param width The width of the border, in pixels
   * @param padding The width around the edge of the image that will get chopped, in pixels
   * @return BorderOptions
   */
  public static BorderOptions create(@ColorInt int color, float width, float padding) {
    return new BorderOptions(color, width, padding);
  }

  /**
   * Create border options with padding and scaleDownInsideBorders. Note that currently padding is
   * not supported with RoundingOptions.asCircle().
   *
   * @param color The color of the border
   * @param width The width of the border, in pixels
   * @param padding The width around the edge of the image that will get chopped, in pixels
   * @param scaleDownInsideBorders true if scaled down inside border, false otherwise
   * @return BorderOptions
   */
  public static BorderOptions create(
      @ColorInt int color, float width, float padding, boolean scaleDownInsideBorders) {
    return new BorderOptions(color, width, padding, scaleDownInsideBorders);
  }

  public final @ColorInt int color;
  public final float width;
  public final float padding;
  public final boolean scaleDownInsideBorders;

  public BorderOptions(@ColorInt int color, float width) {
    this(color, width, 0, false);
  }

  public BorderOptions(@ColorInt int color, float width, float padding) {
    this(color, width, padding, false);
  }

  public BorderOptions(
      @ColorInt int color, float width, float padding, boolean scaleDownInsideBorders) {
    this.color = color;
    this.width = width;
    this.padding = padding;
    this.scaleDownInsideBorders = scaleDownInsideBorders;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    BorderOptions that = (BorderOptions) obj;
    return color == that.color
        && width == that.width
        && padding == that.padding
        && scaleDownInsideBorders == that.scaleDownInsideBorders;
  }

  @Override
  public int hashCode() {
    int result = color;
    result = 31 * result + Float.floatToIntBits(width);
    result = 31 * result + Float.floatToIntBits(padding);
    result = 31 * result + (scaleDownInsideBorders ? 0 : 1);
    return result;
  }
}
