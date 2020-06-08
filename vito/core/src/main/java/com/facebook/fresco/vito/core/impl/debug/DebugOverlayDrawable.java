/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.Gravity;
import java.util.HashMap;
import java.util.Map;

public class DebugOverlayDrawable extends Drawable {

  private static final int OUTLINE_COLOR = 0xFFFF9800;
  private static final int TEXT_BACKGROUND_COLOR = 0x66000000;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final int IDENTIFIER_COLOR = 0xFF00FF00;
  private static final int OUTLINE_STROKE_WIDTH_PX = 2;
  private static final int MAX_TEXT_SIZE_PX = 72;
  private static final int MIN_TEXT_SIZE_PX = 16;
  private static final int TEXT_LINE_SPACING_PX = 8;
  private static final int TEXT_PADDING_PX = 10;
  private static final int INITIAL_MAX_LINE_LENGTH = 4;
  private static final int MARGIN = TEXT_LINE_SPACING_PX / 2;

  private int mTextGravity = Gravity.BOTTOM;

  // Internal helpers
  private final HashMap<String, Pair<String, Integer>> mDebugData = new HashMap<>();
  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final String mIdentifier;
  private int mMaxLineLength = INITIAL_MAX_LINE_LENGTH;
  private int mStartTextXPx;
  private int mStartTextYPx;
  private int mLineIncrementPx;
  private int mCurrentTextXPx;
  private int mCurrentTextYPx;

  public DebugOverlayDrawable() {
    this("");
  }

  public DebugOverlayDrawable(String identifier) {
    mIdentifier = identifier;
    reset();
  }

  public void addDebugData(String key, String value) {
    addDebugData(key, value, TEXT_COLOR);
  }

  public void addDebugData(String key, String value, Integer color) {
    mDebugData.put(key, new Pair<>(value, color));
    mMaxLineLength = Math.max(value.length(), mMaxLineLength);
    prepareDebugTextParameters(getBounds());
  }

  public void reset() {
    mDebugData.clear();
    mMaxLineLength = INITIAL_MAX_LINE_LENGTH;
    invalidateSelf();
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

    // Draw text
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setStrokeWidth(0);
    mPaint.setColor(TEXT_COLOR);

    // Reset the text position
    mCurrentTextXPx = mStartTextXPx;
    mCurrentTextYPx = mStartTextYPx;

    addDebugText(canvas, "Vito", mIdentifier, IDENTIFIER_COLOR);
    for (Map.Entry<String, Pair<String, Integer>> entry : mDebugData.entrySet()) {
      addDebugText(canvas, entry.getKey(), entry.getValue().first, entry.getValue().second);
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
    if (mDebugData.isEmpty() || mMaxLineLength <= 0) {
      return;
    }
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

  protected void addDebugText(Canvas canvas, String label, String value, Integer color) {
    final String labelColon = label + ": ";
    final float labelColonWidth = mPaint.measureText(labelColon);
    final float valueWidth = mPaint.measureText(value);

    mPaint.setColor(TEXT_BACKGROUND_COLOR);
    canvas.drawRect(
        mCurrentTextXPx - MARGIN,
        mCurrentTextYPx + TEXT_LINE_SPACING_PX,
        mCurrentTextXPx + labelColonWidth + valueWidth + MARGIN,
        mCurrentTextYPx + mLineIncrementPx + TEXT_LINE_SPACING_PX,
        mPaint);

    mPaint.setColor(TEXT_COLOR);
    canvas.drawText(labelColon, mCurrentTextXPx, mCurrentTextYPx, mPaint);
    mPaint.setColor(color);
    canvas.drawText(
        String.valueOf(value), mCurrentTextXPx + labelColonWidth, mCurrentTextYPx, mPaint);

    mCurrentTextYPx += mLineIncrementPx;
  }

  protected void addDebugText(
      Canvas canvas, int x, int y, String label, String value, Integer color) {
    final String labelColon = label + ": ";
    final float labelColonWidth = mPaint.measureText(labelColon);
    final float valueWidth = mPaint.measureText(value);

    mPaint.setColor(TEXT_BACKGROUND_COLOR);
    canvas.drawRect(
        x - MARGIN,
        y + TEXT_LINE_SPACING_PX,
        x + labelColonWidth + valueWidth + MARGIN,
        y + mLineIncrementPx + TEXT_LINE_SPACING_PX,
        mPaint);

    mPaint.setColor(TEXT_COLOR);
    canvas.drawText(labelColon, mCurrentTextXPx, mCurrentTextYPx, mPaint);
    mPaint.setColor(color);
    canvas.drawText(
        String.valueOf(value), mCurrentTextXPx + labelColonWidth, mCurrentTextYPx, mPaint);

    mCurrentTextYPx += mLineIncrementPx;
  }
}
