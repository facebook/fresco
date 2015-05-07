/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import java.util.Arrays;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

public class RoundedColorDrawable extends Drawable implements Rounded {
  @VisibleForTesting final float[] mRadii = new float[8];
  @VisibleForTesting final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  @VisibleForTesting boolean mIsCircle = false;
  @VisibleForTesting float mBorderWidth = 0;
  @VisibleForTesting int mBorderColor = Color.TRANSPARENT;
  @VisibleForTesting final Path mPath = new Path();
  private int mColor = Color.TRANSPARENT;
  private final RectF mTempRect = new RectF();
  private int mAlpha = 255;

  /**
   * Creates a RoundedColorDrawable.
   *
   * @param color of the drawable
   */
  public RoundedColorDrawable(int color) {
    setColor(color);
  }

  /**
   * Creates a new instance of RoundedColorDrawable from the given ColorDrawable.
   * @param colorDrawable color drawable to extract the color from
   * @return a new RoundedColorDrawable
   */
  public static RoundedColorDrawable fromColorDrawable(ColorDrawable colorDrawable) {
    return new RoundedColorDrawable(colorDrawable.getColor());
  }

  /**
   * Creates a new instance of RoundedColorDrawable.
   *
   * @param radii Each corner receive two radius values [X, Y]. The corners are ordered
   *   top-left, top-right, bottom-right, bottom-left.
   * @param color of the drawable
   */
  public RoundedColorDrawable(float[] radii, int color) {
    this(color);
    setRadii(radii);
  }

  /**
   * Creates a new instance of RoundedColorDrawable.
   *
   * @param radius of the corners in pixels
   * @param color of the drawable
   */
  public RoundedColorDrawable(float radius, int color) {
    this(color);
    setRadius(radius);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    updatePath();
  }

  @Override
  public void draw(Canvas canvas) {
    mPaint.setColor(DrawableUtils.multiplyColorAlpha(mColor, mAlpha));
    mPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mPath, mPaint);
    if (mBorderWidth != 0) {
      mPaint.setColor(DrawableUtils.multiplyColorAlpha(mBorderColor, mAlpha));
      mPaint.setStyle(Paint.Style.STROKE);
      mPaint.setStrokeWidth(mBorderWidth);
      canvas.drawPath(mPath, mPaint);
    }
  }

  /**
   * Sets whether to round as circle.
   *
   * @param isCircle whether or not to round as circle
   */
  @Override
  public void setCircle(boolean isCircle) {
    mIsCircle = isCircle;
    updatePath();
    invalidateSelf();
  }

  /**
   * Sets the rounding radii.
   *
   * @param radii Each corner receive two radius values [X, Y]. The corners are ordered
   * top-left, top-right, bottom-right, bottom-left
   */
  @Override
  public void setRadii(float[] radii) {
    if (radii == null) {
      Arrays.fill(mRadii, 0);
    } else {
      Preconditions.checkArgument(radii.length == 8, "radii should have exactly 8 values");
      System.arraycopy(radii, 0, mRadii, 0, 8);
    }
    updatePath();
    invalidateSelf();
  }

  /**
   * Sets the rounding radius.
   *
   * @param radius
   */
  @Override
  public void setRadius(float radius) {
    Preconditions.checkArgument(radius >= 0, "radius should be non negative");
    Arrays.fill(mRadii, radius);
    updatePath();
    invalidateSelf();
  }

  /**
   * Sets the color.
   * @param color
   */
  public void setColor(int color) {
    if (mColor != color) {
      mColor = color;
      invalidateSelf();
    }
  }

  /**
   * Gets the color.
   * @return color
   */
  public int getColor() {
    return mColor;
  }

  /**
   * Sets the border
   * @param color of the border
   * @param width of the border
   */
  @Override
  public void setBorder(int color, float width) {
    if (mBorderColor != color) {
      mBorderColor = color;
      invalidateSelf();
    }

    if (mBorderWidth != width) {
      mBorderWidth = width;
      updatePath();
      invalidateSelf();
    }
  }

  /**
   * Sets the drawable's alpha value.
   *
   * @param alpha The alpha value to set, between 0 and 255.
   */
  @Override
  public void setAlpha(int alpha) {
    if (alpha != mAlpha) {
      mAlpha = alpha;
      invalidateSelf();
    }
  }

  /**
   * Returns the drawable's alpha value.
   *
   * @return A value between 0 and 255.
   */
  @Override
  public int getAlpha() {
    return mAlpha;
  }

  /**
   * Setting a color filter on a ColorDrawable has no effect. This has been inspired by Android
   * ColorDrawable.
   *
   * @param colorFilter Ignore.
   */
  @Override
  public void setColorFilter(ColorFilter colorFilter) {
  }

  /**
   * Returns the opacity of the final color which would be used for drawing. This has been
   * inspired by Android ColorDrawable.
   *
   * @return the opacity
   */
  @Override
  public int getOpacity() {
    return DrawableUtils.getOpacityFromColor(DrawableUtils.multiplyColorAlpha(mColor, mAlpha));
  }

  private void updatePath() {
    mPath.reset();
    mTempRect.set(getBounds());
    mTempRect.inset(mBorderWidth/2, mBorderWidth/2);
    if (mIsCircle) {
      float radius = Math.min(mTempRect.width(), mTempRect.height()) / 2;
      mPath.addCircle(mTempRect.centerX(), mTempRect.centerY(), radius, Path.Direction.CW);
    } else {
      mPath.addRoundRect(mTempRect, mRadii, Path.Direction.CW);
    }
    mTempRect.inset(-mBorderWidth/2, -mBorderWidth/2);
  }
}
