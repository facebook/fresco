/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

import java.util.Arrays;

/**
 * Drawable that draws underlying drawable with rounded corners.
 */
public class RoundedCornersDrawable extends ForwardingDrawable implements Rounded {

  public enum Type {
    /**
     * Draws rounded corners on top of the underlying drawable by overlaying a solid color which
     * is specified by {@code setOverlayColor}. This option should only be used when the
     * background beneath the underlying drawable is static and of the same solid color.
     */
    OVERLAY_COLOR,

    /**
     * Clips the drawable to be rounded. This option is not supported right now but is expected to
     * be made available in the future.
     */
    CLIPPING
  }

  @VisibleForTesting Type mType = Type.OVERLAY_COLOR;
  @VisibleForTesting final float[] mRadii = new float[8];
  @VisibleForTesting final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  @VisibleForTesting boolean mIsCircle = false;
  @VisibleForTesting float mBorderWidth = 0;
  @VisibleForTesting int mBorderColor = Color.TRANSPARENT;
  @VisibleForTesting int mOverlayColor = Color.TRANSPARENT;
  private final Path mPath = new Path();
  private final RectF mTempRectangle = new RectF();

  /**
   * Creates a new RoundedCornersDrawable with given underlying drawable.
   *
   * @param drawable underlying drawable
   */
  public RoundedCornersDrawable(Drawable drawable) {
    super(Preconditions.checkNotNull(drawable));
  }

  /**
   * Sets the type of rounding process
   *
   * @param type type of rounding process
   */
  public void setType(Type type) {
    mType = type;
    invalidateSelf();
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
   * Sets radius to be used for rounding
   *
   * @param radius corner radius in pixels
   */
  @Override
  public void setRadius(float radius) {
    Arrays.fill(mRadii, radius);
    updatePath();
    invalidateSelf();
  }

  /**
   * Sets radii values to be used for rounding.
   * Each corner receive two radius values [X, Y]. The corners are ordered
   * top-left, top-right, bottom-right, bottom-left
   *
   * @param radii Array of 8 values, 4 pairs of [X,Y] radii
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
   * Sets the overlay color.
   *
   * @param overlayColor the color to filled outside the rounded corners
   */
  public void setOverlayColor(int overlayColor) {
    mOverlayColor = overlayColor;
    invalidateSelf();
  }

  /**
   * Sets the border
   * @param color of the border
   * @param width of the border
   */
  @Override
  public void setBorder(int color, float width) {
    mBorderColor = color;
    mBorderWidth = width;
    updatePath();
    invalidateSelf();
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    updatePath();
  }

  private void updatePath() {
    mPath.reset();
    mTempRectangle.set(getBounds());
    mTempRectangle.inset(mBorderWidth/2, mBorderWidth/2);
    if (mIsCircle) {
      mPath.addCircle(
          mTempRectangle.centerX(),
          mTempRectangle.centerY(),
          Math.min(mTempRectangle.width(), mTempRectangle.height())/2,
          Path.Direction.CW);
    } else {
      mPath.addRoundRect(mTempRectangle, mRadii, Path.Direction.CW);
    }
    mTempRectangle.inset(-mBorderWidth/2, -mBorderWidth/2);
  }

  @Override
  public void draw(Canvas canvas) {
    Rect bounds = getBounds();
    switch (mType) {
      case CLIPPING:
        // clip, note: doesn't support anti-aliasing
        int saveCount = canvas.save();
        mPath.setFillType(Path.FillType.EVEN_ODD);
        canvas.clipPath(mPath);
        super.draw(canvas);
        canvas.restoreToCount(saveCount);
        break;
      case OVERLAY_COLOR:
        super.draw(canvas);
        mPaint.setColor(mOverlayColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        canvas.drawPath(mPath, mPaint);

        if (mIsCircle) {
          // INVERSE_EVEN_ODD will only draw inverse circle within its bounding box, so we need to
          // fill the rest manually if the bounds are not square.
          float paddingH = (bounds.width() - bounds.height() + mBorderWidth) / 2f;
          float paddingV = (bounds.height() - bounds.width() + mBorderWidth) / 2f;
          if (paddingH > 0) {
            canvas.drawRect(bounds.left, bounds.top, bounds.left + paddingH, bounds.bottom, mPaint);
            canvas.drawRect(
                bounds.right - paddingH,
                bounds.top,
                bounds.right,
                bounds.bottom,
                mPaint);
          }
          if (paddingV > 0) {
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + paddingV, mPaint);
            canvas.drawRect(
                bounds.left,
                bounds.bottom - paddingV,
                bounds.right,
                bounds.bottom,
                mPaint);
          }
        }
        break;
    }

    if (mBorderColor != Color.TRANSPARENT) {
      mPaint.setStyle(Paint.Style.STROKE);
      mPaint.setColor(mBorderColor);
      mPaint.setStrokeWidth(mBorderWidth);
      mPath.setFillType(Path.FillType.EVEN_ODD);
      canvas.drawPath(mPath, mPaint);
    }
  }
}
