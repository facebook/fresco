/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
* Drawable that displays a progress bar based on the level.
*/
public class ProgressBarDrawable extends Drawable implements CloneableDrawable {

  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path mPath = new Path();
  private final RectF mRect = new RectF();
  private int mBackgroundColor = 0x80000000;
  private int mColor = 0x800080FF;
  private int mPadding = 10;
  private int mBarWidth = 20;
  private int mLevel = 0;
  private int mRadius = 0;
  private boolean mHideWhenZero = false;
  private boolean mIsVertical = false;

  /** Sets the progress bar color. */
  public void setColor(int color) {
    if (mColor != color) {
      mColor = color;
      invalidateSelf();
    }
  }

  /** Gets the progress bar color. */
  public int getColor() {
    return mColor;
  }

  /** Sets the progress bar background color. */
  public void setBackgroundColor(int backgroundColor) {
    if (mBackgroundColor != backgroundColor) {
      mBackgroundColor = backgroundColor;
      invalidateSelf();
    }
  }

  /** Gets the progress bar background color. */
  public int getBackgroundColor() {
    return mBackgroundColor;
  }

  /** Sets the progress bar padding. */
  public void setPadding(int padding) {
    if (mPadding != padding) {
      mPadding = padding;
      invalidateSelf();
    }
  }

  /** Gets the progress bar padding. */
  @Override
  public boolean getPadding(Rect padding) {
    padding.set(mPadding, mPadding, mPadding, mPadding);
    return mPadding != 0;
  }

  /** Sets the progress bar width. */
  public void setBarWidth(int barWidth) {
    if (mBarWidth != barWidth) {
      mBarWidth = barWidth;
      invalidateSelf();
    }
  }

  /** Gets the progress bar width. */
  public int getBarWidth() {
    return mBarWidth;
  }

  /** Sets whether the progress bar should be hidden when the progress is 0. */
  public void setHideWhenZero(boolean hideWhenZero) {
    mHideWhenZero = hideWhenZero;
  }

  /** Gets whether the progress bar should be hidden when the progress is 0. */
  public boolean getHideWhenZero() {
    return mHideWhenZero;
  }

  /** The progress bar will be displayed as a rounded corner rectangle, sets the radius here. */
  public void setRadius(int radius) {
    if (mRadius != radius) {
      mRadius = radius;
      invalidateSelf();
    }
  }

  /** Gets the radius of the progress bar. */
  public int getRadius() {
    return mRadius;
  }

  /** Sets if the progress bar should be vertical. */
  public void setIsVertical(boolean isVertical) {
    if (mIsVertical != isVertical) {
      mIsVertical = isVertical;
      invalidateSelf();
    }
  }

  /** Gets if the progress bar is vertical. */
  public boolean getIsVertical() {
    return mIsVertical;
  }

  @Override
  protected boolean onLevelChange(int level) {
    mLevel = level;
    invalidateSelf();
    return true;
  }

  @Override
  public void setAlpha(int alpha) {
    mPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mPaint.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return DrawableUtils.getOpacityFromColor(mPaint.getColor());
  }

  @Override
  public void draw(Canvas canvas) {
    if (mHideWhenZero && mLevel == 0) {
      return;
    }
    if (mIsVertical) {
      drawVerticalBar(canvas, 10000, mBackgroundColor);
      drawVerticalBar(canvas, mLevel, mColor);
    } else {
      drawHorizontalBar(canvas, 10000, mBackgroundColor);
      drawHorizontalBar(canvas, mLevel, mColor);
    }
  }

  @Override
  public Drawable cloneDrawable() {
    final ProgressBarDrawable copy = new ProgressBarDrawable();
    copy.mBackgroundColor = mBackgroundColor;
    copy.mColor = mColor;
    copy.mPadding = mPadding;
    copy.mBarWidth = mBarWidth;
    copy.mLevel = mLevel;
    copy.mRadius = mRadius;
    copy.mHideWhenZero = mHideWhenZero;
    copy.mIsVertical = mIsVertical;
    return copy;
  }

  private void drawHorizontalBar(Canvas canvas, int level, int color) {
    Rect bounds = getBounds();
    int length = (bounds.width() - 2 * mPadding) * level / 10000;
    int xpos = bounds.left + mPadding;
    int ypos = bounds.bottom - mPadding - mBarWidth;
    mRect.set(xpos, ypos, xpos + length, ypos + mBarWidth);
    drawBar(canvas, color);
  }

  private void drawVerticalBar(Canvas canvas, int level, int color) {
    Rect bounds = getBounds();
    int length = (bounds.height() - 2 * mPadding) * level / 10000;
    int xpos = bounds.left + mPadding;
    int ypos = bounds.top + mPadding;
    mRect.set(xpos, ypos, xpos + mBarWidth, ypos + length);
    drawBar(canvas, color);
  }

  private void drawBar(Canvas canvas, int color) {
    mPaint.setColor(color);
    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    mPath.reset();
    mPath.setFillType(Path.FillType.EVEN_ODD);
    mPath.addRoundRect(
        mRect,
        Math.min(mRadius, mBarWidth / 2),
        Math.min(mRadius, mBarWidth / 2),
        Path.Direction.CW);
    canvas.drawPath(mPath, mPaint);
  }
}
