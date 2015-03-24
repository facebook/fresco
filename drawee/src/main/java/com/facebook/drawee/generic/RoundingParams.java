/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.generic;

import java.util.Arrays;

import android.graphics.Color;

import com.facebook.common.internal.Preconditions;

/**
 * Class that encapsulates rounding parameters.
 */
public class RoundingParams {

  public enum RoundingMethod {
    /**
     * Draws rounded corners on top of the underlying drawable by overlaying a solid color which
     * is specified by {@code setOverlayColor}. This option should only be used when the
     * background beneath the underlying drawable is static and of the same solid color.
     */
    OVERLAY_COLOR,

    /**
     * Uses BitmapShader to draw bitmap with rounded corners. Works only with BitmapDrawables and
     * ColorDrawables.
     * IMPORTANT: Only the actual image and the placeholder image will get rounded. Other images
     * (such as retry, failure, progress bar, backgrounds, overlays, etc.) won't get rounded.
     */
    BITMAP_ONLY
  }

  private RoundingMethod mRoundingMethod = RoundingMethod.BITMAP_ONLY;
  private boolean mRoundAsCircle = false;
  private float[] mCornersRadii = null;
  private int mOverlayColor = 0;
  private float mBorderWidth = 0;
  private int mBorderColor = Color.TRANSPARENT;

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
  public RoundingParams setOverlayColor(int overlayColor) {
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
   * Sets the border around the rounded drawable
   * @param color of the border
   * @param width of the width
   */
  public RoundingParams setBorder(int color, float width) {
    Preconditions.checkArgument(width >= 0, "the border width cannot be < 0");
    mBorderWidth = width;
    mBorderColor = color;
    return this;
  }

  /** Gets the border width */
  public float getBorderWidth() {
    return mBorderWidth;
  }

  /** Gets the border color */
  public int getBorderColor() {
    return mBorderColor;
  }
}
