/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.debug;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.drawee.debug.listener.ImageLoadingTimeListener;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Drawee Controller overlay that displays debug information. */
public class DebugControllerOverlayDrawable extends Drawable implements ImageLoadingTimeListener {

  private static final String NO_CONTROLLER_ID = "none";

  // Green if the image dimensions are OK
  @VisibleForTesting
  static final int OVERLAY_COLOR_IMAGE_OK = 0x664CAF50;

  // Orange if the image dimensions are a bit off
  @VisibleForTesting
  static final int OVERLAY_COLOR_IMAGE_ALMOST_OK = 0x66FF9800;

  // Red if the image dimensions are too far off
  @VisibleForTesting
  static final int OVERLAY_COLOR_IMAGE_NOT_OK = 0x66F44336;

  // Values are given in per cent. E.g. 0.1 means 10% smaller or larger.
  private static final float IMAGE_SIZE_THRESHOLD_OK = 0.1f;
  private static final float IMAGE_SIZE_THRESHOLD_NOT_OK = 0.5f;

  private static final int OUTLINE_COLOR = 0xFFFF9800;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final int OUTLINE_STROKE_WIDTH_PX = 2;
  private static final int MAX_TEXT_SIZE_PX = 40;
  private static final int MIN_TEXT_SIZE_PX = 10;
  private static final int TEXT_LINE_SPACING_PX = 8;
  private static final int TEXT_PADDING_PX = 10;

  // Debug-text dependent parameters
  private static final int MAX_NUMBER_OF_LINES = 9;
  private static final int MAX_LINE_WIDTH_EM = 8;

  // General information
  private String mControllerId;
  private String mImageId;
  private int mWidthPx;
  private int mHeightPx;
  private int mImageSizeBytes;
  private String mImageFormat;
  private ScaleType mScaleType;
  private HashMap<String, String> mAdditionalData = new HashMap<>();

  // Animations
  private int mFrameCount;
  private int mLoopCount;

  // Text gravity
  private int mTextGravity = Gravity.BOTTOM;

  // Internal helpers
  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Matrix mMatrix = new Matrix(); // avoid local allocation
  private final Rect mRect = new Rect(); // avoid local allocation
  private final RectF mRectF = new RectF(); // avoid local allocation
  private int mStartTextXPx;
  private int mStartTextYPx;
  private int mLineIncrementPx;

  private int mCurrentTextXPx;
  private int mCurrentTextYPx;

  private long mFinalImageTimeMs;
  private String mOrigin;

  public DebugControllerOverlayDrawable() {
    reset();
  }

  public void reset() {
    mWidthPx = -1;
    mHeightPx = -1;
    mImageSizeBytes = -1;
    mAdditionalData = new HashMap<>();
    mFrameCount = -1;
    mLoopCount = -1;
    mImageFormat = null;
    setControllerId(null);
    mFinalImageTimeMs = -1;
    mOrigin = null;
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

  public void setImageId(@Nullable String imageId) {
    mImageId = imageId;
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

  public void setOrigin(String s) {
    mOrigin = s;
    invalidateSelf();
  }

  /**
   *
   * @param imageSizeBytes the image size in bytes
   */
  public void setImageSize(int imageSizeBytes) {
    mImageSizeBytes = imageSizeBytes;
  }

  public void addAdditionalData(String key, String value) {
    mAdditionalData.put(key, value);
  }

  public void setImageFormat(@Nullable String imageFormat) {
    mImageFormat = imageFormat;
  }

  public void setScaleType(ScaleType scaleType) {
    mScaleType = scaleType;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);

    // Update the text parameters since the size changed. If you modify the debug text, make sure
    // to also update MAX_NUMBER_OF_LINES and MAX_LINE_WIDTH_EM. The line width has been estimated
    // for a reasonable max line width on average.
    prepareDebugTextParameters(bounds, MAX_NUMBER_OF_LINES, MAX_LINE_WIDTH_EM);
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
    mPaint.setColor(determineOverlayColor(mWidthPx, mHeightPx, mScaleType));
    canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);

    // Draw text
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setStrokeWidth(0);
    mPaint.setColor(TEXT_COLOR);
    // Reset the test position
    mCurrentTextXPx = mStartTextXPx;
    mCurrentTextYPx = mStartTextYPx;

    if (mImageId != null) {
      addDebugText(canvas, "IDs: %s, %s", mControllerId, mImageId);
    } else {
      addDebugText(canvas, "ID: %s", mControllerId);
    }
    addDebugText(canvas, "D: %dx%d", bounds.width(), bounds.height());
    addDebugText(canvas, "I: %dx%d", mWidthPx, mHeightPx);
    addDebugText(canvas, "I: %d KiB", (mImageSizeBytes / 1024));
    if (mImageFormat != null) {
      addDebugText(canvas, "i format: %s", mImageFormat);
    }
    if (mFrameCount > 0) {
      addDebugText(canvas, "anim: f %d, l %d", mFrameCount, mLoopCount);
    }
    if (mScaleType != null) {
      addDebugText(canvas, "scale: %s", mScaleType);
    }
    if (mFinalImageTimeMs >= 0) {
      addDebugText(canvas, "t: %d ms", mFinalImageTimeMs);
    }
    if (mOrigin != null) {
      addDebugText(canvas, "origin: %s", mOrigin);
    }
    for (Map.Entry<String, String> entry : mAdditionalData.entrySet()) {
      addDebugText(canvas, "%s: %s", entry.getKey(), entry.getValue());
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

  @VisibleForTesting
  int determineOverlayColor(
      int imageWidth,
      int imageHeight,
      @Nullable ScaleType scaleType) {
    int visibleDrawnAreaWidth = getBounds().width();
    int visibleDrawnAreaHeight = getBounds().height();

    if (visibleDrawnAreaWidth <= 0 ||
        visibleDrawnAreaHeight <= 0 ||
        imageWidth <= 0 ||
        imageHeight <= 0) {
      return OVERLAY_COLOR_IMAGE_NOT_OK;
    }

    if (scaleType != null) {
      // Apply optional scale type in order to get boundaries of the actual area to be filled
      mRect.left = mRect.top = 0;
      mRect.right = visibleDrawnAreaWidth;
      mRect.bottom = visibleDrawnAreaHeight;

      mMatrix.reset();

      // We can ignore the focus point as it has no influence on the scale, but only the translation
      scaleType.getTransform(mMatrix, mRect, imageWidth, imageHeight, 0f, 0f);

      mRectF.left = mRectF.top = 0;
      mRectF.right = imageWidth;
      mRectF.bottom = imageHeight;

      mMatrix.mapRect(mRectF);

      final int drawnAreaWidth = (int) mRectF.width();
      final int drawnAreaHeight = (int) mRectF.height();

      visibleDrawnAreaWidth = Math.min(visibleDrawnAreaWidth, drawnAreaWidth);
      visibleDrawnAreaHeight = Math.min(visibleDrawnAreaHeight, drawnAreaHeight);
    }

    // Update the thresholds for the overlay color
    float scaledImageWidthThresholdOk = visibleDrawnAreaWidth * IMAGE_SIZE_THRESHOLD_OK;
    float scaledImageWidthThresholdNotOk = visibleDrawnAreaWidth * IMAGE_SIZE_THRESHOLD_NOT_OK;
    float scaledImageHeightThresholdOk = visibleDrawnAreaHeight * IMAGE_SIZE_THRESHOLD_OK;
    float scaledImageHeightThresholdNotOk = visibleDrawnAreaHeight * IMAGE_SIZE_THRESHOLD_NOT_OK;

    // Calculate the dimension differences
    int absWidthDifference = Math.abs(imageWidth - visibleDrawnAreaWidth);
    int absHeightDifference = Math.abs(imageHeight - visibleDrawnAreaHeight);

    // Return corresponding color
    if (absWidthDifference < scaledImageWidthThresholdOk &&
        absHeightDifference < scaledImageHeightThresholdOk) {
      return OVERLAY_COLOR_IMAGE_OK;
    } else if (absWidthDifference < scaledImageWidthThresholdNotOk &&
        absHeightDifference < scaledImageHeightThresholdNotOk) {
      return OVERLAY_COLOR_IMAGE_ALMOST_OK;
    }
    return OVERLAY_COLOR_IMAGE_NOT_OK;
  }

  public void setFinalImageTimeMs(long finalImageTimeMs) {
    mFinalImageTimeMs = finalImageTimeMs;
  }

  @Override
  public void onFinalImageSet(long finalImageTimeMs) {
    mFinalImageTimeMs = finalImageTimeMs;
    invalidateSelf();
  }
}
