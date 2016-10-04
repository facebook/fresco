/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.debug;

import javax.annotation.Nullable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

/**
 * Drawee Controller overlay that displays debug information.
 */
public class DebugControllerOverlayDrawable extends Drawable {

  private static final String NO_CONTROLLER_ID = "none";

  private static final int OVERLAY_COLOR = 0x66FF9800;
  private static final int OUTLINE_COLOR = 0xFFFF9800;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final int OUTLINE_STROKE_WIDTH_PX = 2;
  private static final int MAX_TEXT_SIZE_PX = 40;
  private static final int MIN_TEXT_SIZE_PX = 12;
  private static final int TEXT_LINE_SPACING_PX = 8;
  private static final int TEXT_PADDING_PX = 10;

  // Debug-text dependent parameters
  private static final int NUMBER_OF_LINES = 6;
  private static final int MAX_LINE_WIDTH_EM = 8;

  // General information
  private String mControllerId;
  private int mWidthPx;
  private int mHeightPx;
  private int mImageSizeBytes;
  private String mImageFormat;

  // Animations
  private int mFrameCount;
  private int mLoopCount;

  // Text gravity
  private int mTextGravity = Gravity.BOTTOM;

  // Internal helpers
  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private int mStartTextXPx;
  private int mStartTextYPx;
  private int mLineIncrementPx;

  private int mCurrentTextXPx;
  private int mCurrentTextYPx;

  public DebugControllerOverlayDrawable() {
    reset();
  }

  public void reset() {
    mWidthPx = -1;
    mHeightPx = -1;
    mImageSizeBytes = -1;
    mFrameCount = -1;
    mLoopCount = -1;
    mImageFormat = null;
    setControllerId(null);
    invalidateSelf();
  }

  /**
   * The text gravity / direction for the debug text.
   * Currently supported: {@link Gravity#BOTTOM} and {@link Gravity#TOP}.
   * If bottom is used, the text lines will also be drawn from bottom to top.
   * Default: bottom
   * @param textGravity the text gravity to use
   */
  public void setTextGravity(int textGravity) {
    mTextGravity = textGravity;
    invalidateSelf();
  }

  public void setControllerId(@Nullable String controllerId) {
    mControllerId = controllerId != null ? controllerId : NO_CONTROLLER_ID;
    invalidateSelf();
  }

  public void setDimensions(int widthPx, int heightPx) {
    mWidthPx = widthPx;
    mHeightPx = heightPx;
    invalidateSelf();
  }

  public void setAnimationInfo(int frameCount, int loopCount) {
    mFrameCount = frameCount;
    mLoopCount = loopCount;
    invalidateSelf();
  }

  /**
   *
   * @param imageSizeBytes the image size in bytes
   */
  public void setImageSize(int imageSizeBytes) {
    mImageSizeBytes = imageSizeBytes;
  }

  public void setImageFormat(@Nullable String imageFormat) {
    mImageFormat = imageFormat;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);

    // Update the text parameters since the size changed
    // If you modify the debug text, make sure to also update NUMBER_OF_LINES and MAX_LINE_WIDTH_EM.
    // The line width has been estimated for a reasonable max line width on average
    prepareDebugTextParameters(bounds, NUMBER_OF_LINES, MAX_LINE_WIDTH_EM);
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
    mPaint.setColor(OVERLAY_COLOR);
    canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);

    // Draw text
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setStrokeWidth(0);
    mPaint.setColor(TEXT_COLOR);
    // Reset the test position
    mCurrentTextXPx = mStartTextXPx;
    mCurrentTextYPx = mStartTextYPx;

    addDebugText(canvas, "ID: %s", mControllerId);
    addDebugText(canvas, "D: %dx%d", bounds.width(), bounds.height());
    addDebugText(canvas, "I: %dx%d", mWidthPx, mHeightPx);
    addDebugText(canvas, "I: %d KiB", (mImageSizeBytes / 1024));
    if (mImageFormat != null) {
      addDebugText(canvas, "i format: %s", mImageFormat);
    }
    if (mFrameCount > 0) {
      addDebugText(canvas, "anim: f %d, l %d", mFrameCount, mLoopCount);
    }
  }

  @Override
  public void setAlpha(int alpha) {
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  private void prepareDebugTextParameters(
      Rect bounds,
      int numberOfLines,
      int maxLineLengthEm) {
    int textSizePx = Math.min(bounds.width() / maxLineLengthEm, bounds.height() / numberOfLines);
    textSizePx = Math.min(MAX_TEXT_SIZE_PX, Math.max(MIN_TEXT_SIZE_PX, textSizePx));
    mPaint.setTextSize(textSizePx);

    mLineIncrementPx = textSizePx + TEXT_LINE_SPACING_PX;
    if (mTextGravity == Gravity.BOTTOM) {
      mLineIncrementPx *= -1;
    }
    mStartTextXPx = bounds.left + TEXT_PADDING_PX;
    mStartTextYPx = mTextGravity == Gravity.BOTTOM
        ? bounds.bottom - TEXT_PADDING_PX
        : bounds.top + TEXT_PADDING_PX + MIN_TEXT_SIZE_PX;
  }

  private void addDebugText(Canvas canvas, String text, @Nullable Object... args) {
    if (args == null) {
      canvas.drawText(text, mCurrentTextXPx, mCurrentTextYPx, mPaint);
    } else {
      canvas.drawText(String.format(text, args), mCurrentTextXPx, mCurrentTextYPx, mPaint);
    }
    mCurrentTextYPx += mLineIncrementPx;
  }
}
