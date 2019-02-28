/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import java.util.Arrays;
import javax.annotation.Nullable;

public class RoundedColorDrawable extends Drawable implements Rounded {
  private final float[] mRadii = new float[8];
  @VisibleForTesting final float[] mBorderRadii = new float[8];
  @VisibleForTesting @Nullable float[] mInsideBorderRadii;
  @VisibleForTesting final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private boolean mIsCircle = false;
  private float mBorderWidth = 0;
  private float mPadding = 0;
  private int mBorderColor = Color.TRANSPARENT;
  private boolean mScaleDownInsideBorders = false;
  private boolean mPaintFilterBitmap = false;
  @VisibleForTesting final Path mPath = new Path();
  @VisibleForTesting final Path mBorderPath = new Path();
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
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
    mPaint.setFilterBitmap(getPaintFilterBitmap());
    canvas.drawPath(mPath, mPaint);
    if (mBorderWidth != 0) {
      mPaint.setColor(DrawableUtils.multiplyColorAlpha(mBorderColor, mAlpha));
      mPaint.setStyle(Paint.Style.STROKE);
      mPaint.setStrokeWidth(mBorderWidth);
      canvas.drawPath(mBorderPath, mPaint);
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

  /** Returns whether or not this drawable rounds as circle. */
  @Override
  public boolean isCircle() {
    return mIsCircle;
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

  /** Gets the radii. */
  @Override
  public float[] getRadii() {
    return mRadii;
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

  /** Gets the border color. */
  @Override
  public int getBorderColor() {
    return mBorderColor;
  }

  /** Gets the border width. */
  @Override
  public float getBorderWidth() {
    return mBorderWidth;
  }

  @Override
  public void setPadding(float padding) {
    if (mPadding != padding) {
      mPadding = padding;
      updatePath();
      invalidateSelf();
    }
  }

  /** Gets the padding. */
  @Override
  public float getPadding() {
    return mPadding;
  }

  /**
   * Sets whether image should be scaled down inside borders.
   *
   * @param scaleDownInsideBorders
   */
  @Override
  public void setScaleDownInsideBorders(boolean scaleDownInsideBorders) {
    if (mScaleDownInsideBorders != scaleDownInsideBorders) {
      mScaleDownInsideBorders = scaleDownInsideBorders;
      updatePath();
      invalidateSelf();
    }
  }

  /** Gets whether image should be scaled down inside borders. */
  @Override
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
   */
  @Override
  public void setPaintFilterBitmap(boolean paintFilterBitmap) {
    if (mPaintFilterBitmap != paintFilterBitmap) {
      mPaintFilterBitmap = paintFilterBitmap;
      invalidateSelf();
    }
  }

  /** Gets whether to set FILTER_BITMAP_FLAG flag to Paint. */
  @Override
  public boolean getPaintFilterBitmap() {
    return mPaintFilterBitmap;
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
    mBorderPath.reset();
    mTempRect.set(getBounds());

    mTempRect.inset(mBorderWidth/2, mBorderWidth/2);
    if (mIsCircle) {
      float radius = Math.min(mTempRect.width(), mTempRect.height())/2;
      mBorderPath.addCircle(mTempRect.centerX(), mTempRect.centerY(), radius, Path.Direction.CW);
    } else {
      for (int i = 0; i < mBorderRadii.length; i++) {
        mBorderRadii[i] = mRadii[i] + mPadding - mBorderWidth/2;
      }
      mBorderPath.addRoundRect(mTempRect, mBorderRadii, Path.Direction.CW);
    }
    mTempRect.inset(-mBorderWidth/2, -mBorderWidth/2);

    float totalPadding = mPadding + (mScaleDownInsideBorders ? mBorderWidth : 0);
    mTempRect.inset(totalPadding, totalPadding);
    if (mIsCircle) {
      float radius = Math.min(mTempRect.width(), mTempRect.height())/2;
      mPath.addCircle(mTempRect.centerX(), mTempRect.centerY(), radius, Path.Direction.CW);
    } else if (mScaleDownInsideBorders) {
      if (mInsideBorderRadii == null) {
        mInsideBorderRadii = new float[8];
      }
      for (int i = 0; i < mInsideBorderRadii.length; i++) {
        mInsideBorderRadii[i] = mRadii[i] - mBorderWidth;
      }
      mPath.addRoundRect(mTempRect, mInsideBorderRadii, Path.Direction.CW);
    } else {
      mPath.addRoundRect(mTempRect, mRadii, Path.Direction.CW);
    }
    mTempRect.inset(-totalPadding, -totalPadding);
  }
}
