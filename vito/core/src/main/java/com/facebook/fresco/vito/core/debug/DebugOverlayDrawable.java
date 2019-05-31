/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.debug;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import androidx.annotation.ColorInt;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class DebugOverlayDrawable extends Drawable {

  private static final int OUTLINE_COLOR = 0xFFFF9800;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final int OUTLINE_STROKE_WIDTH_PX = 2;
  private static final int MAX_TEXT_SIZE_PX = 72;
  private static final int MIN_TEXT_SIZE_PX = 12;
  private static final int TEXT_LINE_SPACING_PX = 8;
  private static final int TEXT_PADDING_PX = 10;
  private static final int INITIAL_MAX_LINE_LENGTH = 4;

  private @ColorInt int mBackgroundColor = 0x88DB6130;
  private int mTextGravity = Gravity.BOTTOM;

  // Internal helpers
  private final HashMap<String, String> mDebugData = new HashMap<>();
  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private int mMaxLineLength = INITIAL_MAX_LINE_LENGTH;
  private int mStartTextXPx;
  private int mStartTextYPx;
  private int mLineIncrementPx;
  private int mCurrentTextXPx;
  private int mCurrentTextYPx;

  public DebugOverlayDrawable() {
    reset();
  }

  public void addDebugData(String key, String value) {
    mDebugData.put(key, value);
    mMaxLineLength = Math.max(value.length(), mMaxLineLength);
  }

  public void reset() {
    mDebugData.clear();
    mMaxLineLength = INITIAL_MAX_LINE_LENGTH;
    invalidateSelf();
  }

  public void setBackgroundColor(@ColorInt int overlayColor) {
    mBackgroundColor = overlayColor;
  }

  /**
   * Set a text gravity to indicate where the debug text should be drawn.
   *
   * @see Gravity
   * @param textGravity the gravity to use
   */
  public void setTextGravity(int textGravity) {
    mTextGravity = textGravity;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    prepareDebugTextParameters(bounds);
  }

  @Override
  public void draw(Canvas canvas) {
    Rect bounds = getBounds();

    // Draw bounding box
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeWidth(OUTLINE_STROKE_WIDTH_PX);
    mPaint.setColor(OUTLINE_COLOR);
    canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);

    // Draw overlay
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setColor(mBackgroundColor);
    canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);

    // Draw text
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setStrokeWidth(0);
    mPaint.setColor(TEXT_COLOR);
    // Reset the test position
    mCurrentTextXPx = mStartTextXPx;
    mCurrentTextYPx = mStartTextYPx;

    for (Map.Entry<String, String> entry : mDebugData.entrySet()) {
      addDebugText(canvas, "%s: %s", entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(ColorFilter cf) {}

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  private void prepareDebugTextParameters(Rect bounds) {
    int textSizePx = Math.min(bounds.width() / mMaxLineLength, bounds.height() / mDebugData.size());
    textSizePx = Math.min(MAX_TEXT_SIZE_PX, Math.max(MIN_TEXT_SIZE_PX, textSizePx));
    mPaint.setTextSize(textSizePx);

    mLineIncrementPx = textSizePx + TEXT_LINE_SPACING_PX;
    if (mTextGravity == Gravity.BOTTOM) {
      mLineIncrementPx *= -1;
    }
    mStartTextXPx = bounds.left + TEXT_PADDING_PX;
    mStartTextYPx =
        mTextGravity == Gravity.BOTTOM
            ? bounds.bottom - TEXT_PADDING_PX
            : bounds.top + TEXT_PADDING_PX + MIN_TEXT_SIZE_PX;
  }

  protected void addDebugText(Canvas canvas, String text, @Nullable Object... args) {
    if (args == null) {
      canvas.drawText(text, mCurrentTextXPx, mCurrentTextYPx, mPaint);
    } else {
      canvas.drawText(String.format(text, args), mCurrentTextXPx, mCurrentTextYPx, mPaint);
    }
    mCurrentTextYPx += mLineIncrementPx;
  }
}
