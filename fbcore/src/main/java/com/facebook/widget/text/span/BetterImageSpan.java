/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.widget.text.span;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ReplacementSpan;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A better implementation of image spans that also supports centering images against the text.
 *
 * <p>In order to migrate from ImageSpan, replace {@code new ImageSpan(drawable, alignment)} with
 * {@code new BetterImageSpan(drawable, BetterImageSpan.normalizeAlignment(alignment))}.
 *
 * <p>There are 3 main differences between BetterImageSpan and ImageSpan: 1. Pass in ALIGN_CENTER to
 * center images against the text. 2. ALIGN_BOTTOM no longer unnecessarily increases the size of the
 * text: DynamicDrawableSpan (ImageSpan's parent) adjusts sizes as if alignment was ALIGN_BASELINE
 * which can lead to unnecessary whitespace. 3. BetterImageSpan supports margins around the image.
 */
public class BetterImageSpan extends ReplacementSpan {

  @IntDef({ALIGN_BASELINE, ALIGN_BOTTOM, ALIGN_CENTER})
  @Retention(RetentionPolicy.SOURCE)
  public @interface BetterImageSpanAlignment {}

  public static final int ALIGN_BOTTOM = 0;
  public static final int ALIGN_BASELINE = 1;
  public static final int ALIGN_CENTER = 2;

  /**
   * A helper function to allow dropping in BetterImageSpan as a replacement to ImageSpan, and
   * allowing for center alignment if passed in.
   */
  public static final @BetterImageSpanAlignment int normalizeAlignment(int alignment) {
    switch (alignment) {
      case DynamicDrawableSpan.ALIGN_BOTTOM:
        return ALIGN_BOTTOM;
      case ALIGN_CENTER:
        return ALIGN_CENTER;
      case DynamicDrawableSpan.ALIGN_BASELINE:
      default:
        return ALIGN_BASELINE;
    }
  }

  protected int mWidth;
  protected int mHeight;
  private Rect mBounds;
  private final int mAlignment;
  private final Paint.FontMetricsInt mFontMetricsInt = new Paint.FontMetricsInt();
  private final Drawable mDrawable;
  private final Rect mMargin;

  public BetterImageSpan(Drawable drawable) {
    this(drawable, ALIGN_BASELINE);
  }

  public BetterImageSpan(Drawable drawable, @BetterImageSpanAlignment int verticalAlignment) {
    this(drawable, verticalAlignment, new Rect());
  }

  public BetterImageSpan(
      Drawable drawable, @BetterImageSpanAlignment int verticalAlignment, Rect margin) {
    mDrawable = drawable;
    mAlignment = verticalAlignment;
    mMargin = margin;
    updateBounds();
  }

  public Drawable getDrawable() {
    return mDrawable;
  }

  public Rect getMargin() {
    return mMargin;
  }

  @BetterImageSpanAlignment
  public int getVerticalAlignment() {
    return mAlignment;
  }

  @Override
  public int getSize(
      Paint paint,
      CharSequence text,
      int start,
      int end,
      @Nullable Paint.FontMetricsInt fontMetrics) {
    updateBounds();
    if (fontMetrics == null) {
      return mWidth;
    }

    return calculateLineWidthAndFontHeight(fontMetrics);
  }

  @Override
  public void draw(
      Canvas canvas,
      CharSequence text,
      int start,
      int end,
      float x,
      int top,
      int y,
      int bottom,
      Paint paint) {
    paint.getFontMetricsInt(mFontMetricsInt);
    int iconTop = getIconTop(y, mFontMetricsInt.ascent, mFontMetricsInt.descent, top, bottom);
    float iconLeft = x + mMargin.left;
    canvas.translate(iconLeft, iconTop);
    mDrawable.draw(canvas);
    canvas.translate(-iconLeft, -iconTop);
  }

  public void updateBounds() {
    mBounds = mDrawable.getBounds();

    mWidth = mBounds.width() + mMargin.left + mMargin.right;
    mHeight = mBounds.height();
  }

  protected int getOffsetAboveBaseline(final int ascent, final int descent) {
    switch (mAlignment) {
      case ALIGN_BOTTOM:
        return descent - mHeight - mMargin.bottom;
      case ALIGN_CENTER:
        int textHeight = (descent - ascent) + mMargin.top + mMargin.bottom;
        int offset = (textHeight - mHeight) / 2;
        return ascent + offset - mMargin.bottom;
      case ALIGN_BASELINE:
      default:
        return -mHeight - mMargin.bottom;
    }
  }

  protected int getIconTop(
      final int baseline, final int ascent, final int descent, final int top, final int bottom) {
    return baseline + getOffsetAboveBaseline(mFontMetricsInt.ascent, mFontMetricsInt.descent);
  }

  /** Returns the width of the image span and increases the height if font metrics are available. */
  protected int calculateLineWidthAndFontHeight(Paint.FontMetricsInt fontMetrics) {
    int offsetAbove = getOffsetAboveBaseline(fontMetrics.ascent, fontMetrics.descent);
    int offsetBelow = mHeight + offsetAbove;

    if (mAlignment == ALIGN_CENTER) {
      offsetAbove -= mMargin.top;
      offsetBelow += mMargin.bottom;
    } else {
      offsetAbove -= mMargin.top;
    }

    updateFontHeight(fontMetrics, offsetAbove, offsetBelow);

    return mWidth;
  }

  protected void updateFontHeight(
      Paint.FontMetricsInt fontMetrics, final int offsetAbove, final int offsetBelow) {
    if (offsetAbove < fontMetrics.ascent) {
      fontMetrics.ascent = offsetAbove;
    }

    if (offsetAbove < fontMetrics.top) {
      fontMetrics.top = offsetAbove;
    }

    if (offsetBelow > fontMetrics.descent) {
      fontMetrics.descent = offsetBelow;
    }

    if (offsetBelow > fontMetrics.bottom) {
      fontMetrics.bottom = offsetBelow;
    }
  }
}
