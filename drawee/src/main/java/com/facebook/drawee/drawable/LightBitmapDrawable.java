/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import javax.annotation.Nullable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

/**
 * Light implementation of a BitmapDrawable
 */
public class LightBitmapDrawable extends Drawable {

  @Nullable private Bitmap mBitmap = null;

  private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;

  private static final int DEFAULT_PAINT_FLAGS =
      Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

  // These are scaled to match the target density.
  private int mBitmapWidth;
  private int mBitmapHeight;

  private final Paint mPaint = new Paint(DEFAULT_PAINT_FLAGS);

  /**
   * Create drawable from a bitmap, setting initial target density based on the display metrics of
   * the resources.
   */
  public LightBitmapDrawable(Resources res, Bitmap bitmap) {
    mBitmap = bitmap;
    mTargetDensity = res.getDisplayMetrics().densityDpi;
    computeBitmapSize();
  }

  /**
   * Returns the paint used to render this drawable.
   */
  public final Paint getPaint() {
    return mPaint;
  }

  /**
   * Returns the bitmap used by this drawable to render. May be null.
   */
  @Nullable public final Bitmap getBitmap() {
    return mBitmap;
  }

  private void computeBitmapSize() {
    if (mBitmap != null) {
      mBitmapWidth = mBitmap.getScaledWidth(mTargetDensity);
      mBitmapHeight = mBitmap.getScaledHeight(mTargetDensity);
    } else {
      mBitmapWidth = mBitmapHeight = -1;
    }
  }

  public void setBitmap(Bitmap bitmap) {
    if (mBitmap != bitmap) {
      mBitmap = bitmap;
      computeBitmapSize();
      invalidateSelf();
    }
  }

  /**
   * Enables or disables anti-aliasing for this drawable. Anti-aliasing affects the edges of the
   * bitmap only so it applies only when the drawable is rotated.
   *
   * @param antiAlias True if the bitmap should be anti-aliased, false otherwise.
   * @see #hasAntiAlias()
   */
  public void setAntiAlias(boolean antiAlias) {
    mPaint.setAntiAlias(antiAlias);
    invalidateSelf();
  }

  /**
   * Indicates whether anti-aliasing is enabled for this drawable.
   *
   * @return True if anti-aliasing is enabled, false otherwise.
   * @see #setAntiAlias(boolean)
   */
  public boolean hasAntiAlias() {
    return mPaint.isAntiAlias();
  }

  @Override
  public void setFilterBitmap(boolean filter) {
    mPaint.setFilterBitmap(filter);
    invalidateSelf();
  }

  @Override
  public void setDither(boolean dither) {
    mPaint.setDither(dither);
    invalidateSelf();
  }

  @Override
  public void draw(Canvas canvas) {
    if (mBitmap == null) {
      return;
    }
    canvas.drawBitmap(mBitmap, null, getBounds(), mPaint);
  }

  @Override
  public void setAlpha(int alpha) {
    if (alpha != mPaint.getAlpha()) {
      mPaint.setAlpha(alpha);
      invalidateSelf();
    }
  }

  public int getAlpha() {
    return mPaint.getAlpha();
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mPaint.setColorFilter(cf);
    invalidateSelf();
  }

  public ColorFilter getColorFilter() {
    return mPaint.getColorFilter();
  }

  @Override
  public int getIntrinsicWidth() {
    return mBitmapWidth;
  }

  @Override
  public int getIntrinsicHeight() {
    return mBitmapHeight;
  }

  @Override
  public int getOpacity() {
    return (mBitmap == null || mBitmap.hasAlpha() || mPaint.getAlpha() < 255) ?
        PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
  }
}
