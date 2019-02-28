/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.generic;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.drawable.ScalingUtils;
import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * Class that encapsulates rounding parameters.
 */
public class RoundingParams {

  public enum RoundingMethod {
    /**
     * Draws rounded corners on top of the underlying drawable by overlaying a solid color which is
     * specified by {@code setOverlayColor}. This option should only be used when the background
     * beneath the underlying drawable is static and of the same solid color.
     *
     * <p>Adding borders with this method will cause image edges to be trimmed off. Not noticeable
     * if the color is opaque, but very noticeable with low opacity.
     */
    OVERLAY_COLOR,

    /**
     * Uses BitmapShader to draw the bitmap with rounded corners. This is the default rounding
     * method. It doesn't support animations, and it does not support any scale types other than
     * {@link ScalingUtils.ScaleType#CENTER_CROP}, {@link ScalingUtils.ScaleType#FOCUS_CROP} and
     * {@link ScalingUtils.ScaleType#FIT_XY}.
     *
     * If you use this rounding method with other scale types, such as
     * {@link ScalingUtils.ScaleType#CENTER}, you won't get an Exception but the image might look
     * wrong (e.g. repeated edges), especially in cases the source image is smaller than the view.
     */
    BITMAP_ONLY
  }

  private RoundingMethod mRoundingMethod = RoundingMethod.BITMAP_ONLY;
  private boolean mRoundAsCircle = false;
  private float[] mCornersRadii = null;
  private int mOverlayColor = 0;
  private float mBorderWidth = 0;
  private int mBorderColor = Color.TRANSPARENT;
  private float mPadding = 0;
  private boolean mScaleDownInsideBorders = false;
  private boolean mPaintFilterBitmap = false;

  /**
   *  Sets whether to round as circle.
   *
   * @param roundAsCircle whether or not to round as circle
   * @return modified instance
   */
  public RoundingParams setRoundAsCircle(boolean roundAsCircle) {
    mRoundAsCircle = roundAsCircle;
    return this;
  }

  /** Gets whether to round as circle */
  public boolean getRoundAsCircle() {
    return mRoundAsCircle;
  }

  /**
   * Sets the rounded corners radius.
   *
   * @param radius corner radius in pixels
   * @return  modified instance
   */
  public RoundingParams setCornersRadius(float radius) {
    Arrays.fill(getOrCreateRoundedCornersRadii(), radius);
    return this;
  }

  /**
   * Sets the rounded corners radii.
   *
   * @param topLeft top-left corner radius in pixels
   * @param topRight top-right corner radius in pixels
   * @param bottomRight bottom-right corner radius in pixels
   * @param bottomLeft bottom-left corner radius in pixels
   * @return modified instance
   */
  public RoundingParams setCornersRadii(
      float topLeft,
      float topRight,
      float bottomRight,
      float bottomLeft) {
    float[] radii = getOrCreateRoundedCornersRadii();
    radii[0] = radii[1] = topLeft;
    radii[2] = radii[3] = topRight;
    radii[4] = radii[5] = bottomRight;
    radii[6] = radii[7] = bottomLeft;
    return this;
  }

  /**
   * Sets the rounded corners radii.
   *
   * @param radii float array of 8 radii in pixels. Each corner receives two radius values [X, Y].
   *     The corners are ordered top-left, top-right, bottom-right, bottom-left.
   * @return modified instance
   */
  public RoundingParams setCornersRadii(float[] radii) {
    Preconditions.checkNotNull(radii);
    Preconditions.checkArgument(radii.length == 8, "radii should have exactly 8 values");
    System.arraycopy(radii, 0, getOrCreateRoundedCornersRadii(), 0, 8);
    return this;
  }

  /**
   * Gets the rounded corners radii.
   *
   * <p> For performance reasons the internal array is returned directly. Do not modify it directly,
   * but use one of the exposed corner radii setters instead.
   */
  public float[] getCornersRadii() {
    return mCornersRadii;
  }

  /**
   * Sets the rounding method.
   *
   * @param roundingMethod method of rounding
   * @return modified instance
   */
  public RoundingParams setRoundingMethod(RoundingMethod roundingMethod) {
    mRoundingMethod = roundingMethod;
    return this;
  }

  /** Gets the rounding method. */
  public RoundingMethod getRoundingMethod() {
    return mRoundingMethod;
  }

  /**
   * Sets the overlay color and changes the method to {@code RoundingMethod.COLOR_OVERLAY}.
   *
   * @param overlayColor overlay color
   */
  public RoundingParams setOverlayColor(@ColorInt int overlayColor) {
    mOverlayColor = overlayColor;
    mRoundingMethod = RoundingMethod.OVERLAY_COLOR;
    return this;
  }

  /** Gets the overlay color. */
  public int getOverlayColor() {
    return mOverlayColor;
  }

  private float[] getOrCreateRoundedCornersRadii() {
    if (mCornersRadii == null) {
      mCornersRadii = new float[8];
    }
    return mCornersRadii;
  }

  /** Factory method that creates new RoundingParams with RoundAsCircle specified. */
  public static RoundingParams asCircle() {
    return (new RoundingParams()).setRoundAsCircle(true);
  }

  /** Factory method that creates new RoundingParams with the specified corners radius. */
  public static RoundingParams fromCornersRadius(float radius) {
    return (new RoundingParams()).setCornersRadius(radius);
  }

  /** Factory method that creates new RoundingParams with the specified corners radii. */
  public static RoundingParams fromCornersRadii(
      float topLeft,
      float topRight,
      float bottomRight,
      float bottomLeft) {
    return (new RoundingParams())
        .setCornersRadii(topLeft, topRight, bottomRight, bottomLeft);
  }

  /** Factory method that creates new RoundingParams with the specified corners radii. */
  public static RoundingParams fromCornersRadii(float[] radii) {
    return (new RoundingParams()).setCornersRadii(radii);
  }

  /**
   * Sets the border width
   * @param width of the width
   */
  public RoundingParams setBorderWidth(float width) {
    Preconditions.checkArgument(width >= 0, "the border width cannot be < 0");
    mBorderWidth = width;
    return this;
  }

  /** Gets the border width */
  public float getBorderWidth() {
    return mBorderWidth;
  }

  /**
   * Sets the border color
   * @param color of the border
   */
  public RoundingParams setBorderColor(@ColorInt int color) {
    mBorderColor = color;
    return this;
  }

  /** Gets the border color */
  public int getBorderColor() {
    return mBorderColor;
  }

  /**
   * Sets the border around the rounded drawable
   * @param color of the border
   * @param width of the width
   */
  public RoundingParams setBorder(@ColorInt int color, float width) {
    Preconditions.checkArgument(width >= 0, "the border width cannot be < 0");
    mBorderWidth = width;
    mBorderColor = color;
    return this;
  }

  /**
   * Sets the padding on rounded drawable. Works only with {@code RoundingMethod.BITMAP_ONLY}
   * @param padding the padding in pixels
   */
  public RoundingParams setPadding(float padding){
    Preconditions.checkArgument(padding >= 0, "the padding cannot be < 0");
    mPadding = padding;
    return this;
  }

  /** Gets the padding size */
  public float getPadding() {
    return mPadding;
  }

  /**
   * Sets whether image should be scaled down inside borders.
   *
   * @param scaleDownInsideBorders whether image should be scaled down inside borders or borders
   *     will be drawn over image
   * @return modified instance
   */
  public RoundingParams setScaleDownInsideBorders(boolean scaleDownInsideBorders) {
    mScaleDownInsideBorders = scaleDownInsideBorders;
    return this;
  }

  /** Gets whether image should be scaled down inside borders. */
  public boolean getScaleDownInsideBorders() {
    return mScaleDownInsideBorders;
  }

  /**
   * Sets FILTER_BITMAP_FLAG flag to Paint. {@link android.graphics.Paint#FILTER_BITMAP_FLAG}
   *
   * <p>This should generally be on when drawing bitmaps, unless performance-bound (rendering to software
   * canvas) or preferring pixelation artifacts to blurriness when scaling
   * significantly.
   *
   * @param paintFilterBitmap whether to set FILTER_BITMAP_FLAG flag to Paint.
   * @return modified instance
   */
  public RoundingParams setPaintFilterBitmap(boolean paintFilterBitmap) {
    mPaintFilterBitmap = paintFilterBitmap;
    return this;
  }

  /** Gets whether to set FILTER_BITMAP_FLAG flag to Paint. */
  public boolean getPaintFilterBitmap() {
    return mPaintFilterBitmap;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RoundingParams that = (RoundingParams) o;

    if (mRoundAsCircle != that.mRoundAsCircle) {
      return false;
    }

    if (mOverlayColor != that.mOverlayColor) {
      return false;
    }

    if (Float.compare(that.mBorderWidth, mBorderWidth) != 0) {
      return false;
    }

    if (mBorderColor != that.mBorderColor) {
      return false;
    }

    if (Float.compare(that.mPadding, mPadding) != 0) {
      return false;
    }

    if (mRoundingMethod != that.mRoundingMethod) {
      return false;
    }

    if (mScaleDownInsideBorders != that.mScaleDownInsideBorders) {
      return false;
    }

    if (mPaintFilterBitmap != that.mPaintFilterBitmap) {
      return false;
    }

    return Arrays.equals(mCornersRadii, that.mCornersRadii);
  }

  @Override
  public int hashCode() {
    int result = mRoundingMethod != null ? mRoundingMethod.hashCode() : 0;
    result = 31 * result + (mRoundAsCircle ? 1 : 0);
    result = 31 * result + (mCornersRadii != null ? Arrays.hashCode(mCornersRadii) : 0);
    result = 31 * result + mOverlayColor;
    result = 31 * result + (mBorderWidth != +0.0f ? Float.floatToIntBits(mBorderWidth) : 0);
    result = 31 * result + mBorderColor;
    result = 31 * result + (mPadding != +0.0f ? Float.floatToIntBits(mPadding) : 0);
    result = 31 * result + (mScaleDownInsideBorders ? 1 : 0);
    result = 31 * result + (mPaintFilterBitmap ? 1 : 0);

    return result;
  }
}
