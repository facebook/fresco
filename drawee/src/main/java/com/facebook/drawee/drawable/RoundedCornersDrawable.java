/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import java.util.Arrays;
import javax.annotation.Nullable;

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
  private final RectF mBounds = new RectF();
  @Nullable private RectF mInsideBorderBounds;
  @Nullable private Matrix mInsideBorderTransform;
  private final float[] mRadii = new float[8];
  @VisibleForTesting final float[] mBorderRadii = new float[8];
  @VisibleForTesting final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private boolean mIsCircle = false;
  private float mBorderWidth = 0;
  private int mBorderColor = Color.TRANSPARENT;
  private int mOverlayColor = Color.TRANSPARENT;
  private float mPadding = 0;
  private boolean mScaleDownInsideBorders = false;
  private boolean mPaintFilterBitmap = false;
  private final Path mPath = new Path();
  private final Path mBorderPath = new Path();
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

  /** Returns whether or not this drawable rounds as circle. */
  @Override
  public boolean isCircle() {
    return mIsCircle;
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

  /** Gets the radii. */
  @Override
  public float[] getRadii() {
    return mRadii;
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

  /** Gets the overlay color. */
  public int getOverlayColor() {
    return mOverlayColor;
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
    mPadding = padding;
    updatePath();
    invalidateSelf();
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
    mScaleDownInsideBorders = scaleDownInsideBorders;
    updatePath();
    invalidateSelf();
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

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    updatePath();
  }

  private void updatePath() {
    mPath.reset();
    mBorderPath.reset();
    mTempRectangle.set(getBounds());

    mTempRectangle.inset(mPadding, mPadding);
    mPath.addRect(mTempRectangle, Path.Direction.CW);
    if (mIsCircle) {
      mPath.addCircle(
              mTempRectangle.centerX(),
              mTempRectangle.centerY(),
              Math.min(mTempRectangle.width(), mTempRectangle.height())/2,
              Path.Direction.CW);
    } else {
      mPath.addRoundRect(mTempRectangle, mRadii, Path.Direction.CW);
    }
    mTempRectangle.inset(-mPadding, -mPadding);

    mTempRectangle.inset(mBorderWidth/2, mBorderWidth/2);
    if (mIsCircle) {
      float radius = Math.min(mTempRectangle.width(), mTempRectangle.height())/2;
      mBorderPath.addCircle(
          mTempRectangle.centerX(), mTempRectangle.centerY(), radius, Path.Direction.CW);
    } else {
      for (int i = 0; i < mBorderRadii.length; i++) {
        mBorderRadii[i] = mRadii[i] + mPadding - mBorderWidth/2;
      }
      mBorderPath.addRoundRect(mTempRectangle, mBorderRadii, Path.Direction.CW);
    }
    mTempRectangle.inset(-mBorderWidth/2, -mBorderWidth/2);
  }

  @Override
  public void draw(Canvas canvas) {
    mBounds.set(getBounds());
    switch (mType) {
      case CLIPPING:
        int saveCount = canvas.save();
        // clip, note: doesn't support anti-aliasing
        mPath.setFillType(Path.FillType.EVEN_ODD);
        canvas.clipPath(mPath);
        super.draw(canvas);
        canvas.restoreToCount(saveCount);
        break;
      case OVERLAY_COLOR:
        if (mScaleDownInsideBorders) {
          if (mInsideBorderBounds == null) {
            mInsideBorderBounds = new RectF(mBounds);
            mInsideBorderTransform = new Matrix();
          } else {
            mInsideBorderBounds.set(mBounds);
          }
          mInsideBorderBounds.inset(mBorderWidth, mBorderWidth);
          mInsideBorderTransform.setRectToRect(
              mBounds, mInsideBorderBounds, Matrix.ScaleToFit.FILL);

          saveCount = canvas.save();
          canvas.clipRect(mBounds);
          canvas.concat(mInsideBorderTransform);
          super.draw(canvas);
          canvas.restoreToCount(saveCount);
        } else {
          super.draw(canvas);
        }

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mOverlayColor);
        mPaint.setStrokeWidth(0f);
        mPaint.setFilterBitmap(getPaintFilterBitmap());
        mPath.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(mPath, mPaint);

        if (mIsCircle) {
          // INVERSE_EVEN_ODD will only draw inverse circle within its bounding box, so we need to
          // fill the rest manually if the bounds are not square.
          float paddingH = (mBounds.width() - mBounds.height() + mBorderWidth) / 2f;
          float paddingV = (mBounds.height() - mBounds.width() + mBorderWidth) / 2f;
          if (paddingH > 0) {
            canvas.drawRect(mBounds.left, mBounds.top, mBounds.left + paddingH, mBounds.bottom, mPaint);
            canvas.drawRect(
                mBounds.right - paddingH,
                mBounds.top,
                mBounds.right,
                mBounds.bottom,
                mPaint);
          }
          if (paddingV > 0) {
            canvas.drawRect(mBounds.left, mBounds.top, mBounds.right, mBounds.top + paddingV, mPaint);
            canvas.drawRect(
                mBounds.left,
                mBounds.bottom - paddingV,
                mBounds.right,
                mBounds.bottom,
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
      canvas.drawPath(mBorderPath, mPaint);
    }
  }
}
