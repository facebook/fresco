/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.bitmap;

import javax.annotation.Nullable;

import java.lang.annotation.Retention;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;

import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck;
import com.facebook.fresco.animation.backend.AnimationInformation;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Bitmap animation backend that renders bitmap frames.
 *
 * The given {@link BitmapFrameCache} is used to cache frames and create new bitmaps. {@link
 * AnimationInformation} defines the main animation parameters, like frame and loop count. {@link
 * BitmapFrameRenderer} is used to render frames to the bitmaps aquired from the {@link
 * BitmapFrameCache}.
 */
public class BitmapAnimationBackend implements AnimationBackend,
    AnimationBackendDelegateWithInactivityCheck.InactivityListener {

  public interface FrameListener {

    /**
     * Called when the backend started drawing the given frame.
     *
     * @param backend the backend
     * @param frameNumber the frame number to be drawn
     */
    void onDrawFrameStart(BitmapAnimationBackend backend, int frameNumber);

    /**
     * Called when the given frame has been drawn.
     *
     * @param backend the backend
     * @param frameNumber the frame number that has been drawn
     * @param frameType the {@link FrameType} that has been drawn
     */
    void onFrameDrawn(BitmapAnimationBackend backend, int frameNumber, @FrameType int frameType);

    /**
     * Called when no bitmap could be drawn by the backend for the given frame number.
     *
     * @param backend the backend
     * @param frameNumber the frame number that could not be drawn
     */
    void onFrameDropped(BitmapAnimationBackend backend, int frameNumber);
  }

  /**
   * Frame type that has been drawn. Can be used for logging.
   */
  @Retention(SOURCE)
  @IntDef({
      FRAME_TYPE_CACHED,
      FRAME_TYPE_REUSED,
      FRAME_TYPE_CREATED,
      FRAME_TYPE_FALLBACK,
  })
  public @interface FrameType {
  }

  public static final int FRAME_TYPE_CACHED = 0;
  public static final int FRAME_TYPE_REUSED = 1;
  public static final int FRAME_TYPE_CREATED = 2;
  public static final int FRAME_TYPE_FALLBACK = 3;

  private final PlatformBitmapFactory mPlatformBitmapFactory;
  private final BitmapFrameCache mBitmapFrameCache;
  private final AnimationInformation mAnimationInformation;
  private final BitmapFrameRenderer mBitmapFrameRenderer;
  private final Paint mPaint;

  @Nullable
  private Rect mBounds;
  private int mBitmapWidth;
  private int mBitmapHeight;
  private Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;
  @Nullable
  private FrameListener mFrameListener;

  public BitmapAnimationBackend(
      PlatformBitmapFactory platformBitmapFactory,
      BitmapFrameCache bitmapFrameCache,
      AnimationInformation animationInformation,
      BitmapFrameRenderer bitmapFrameRenderer) {
    mPlatformBitmapFactory = platformBitmapFactory;
    mBitmapFrameCache = bitmapFrameCache;
    mAnimationInformation = animationInformation;
    mBitmapFrameRenderer = bitmapFrameRenderer;

    mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    updateBitmapDimensions();
  }

  /**
   * Set the bitmap config to be used to create new bitmaps.
   *
   * @param bitmapConfig the bitmap config to be used
   */
  public void setBitmapConfig(Bitmap.Config bitmapConfig) {
    mBitmapConfig = bitmapConfig;
  }

  public void setFrameListener(@Nullable FrameListener frameListener) {
    mFrameListener = frameListener;
  }

  @Override
  public int getFrameCount() {
    return mAnimationInformation.getFrameCount();
  }

  @Override
  public int getFrameDurationMs(int frameNumber) {
    return mAnimationInformation.getFrameDurationMs(frameNumber);
  }

  @Override
  public int getLoopCount() {
    return mAnimationInformation.getLoopCount();
  }

  @Override
  public boolean drawFrame(
      Drawable parent,
      Canvas canvas,
      int frameNumber) {
    if (mFrameListener != null) {
      mFrameListener.onDrawFrameStart(this, frameNumber);
    }

    // Draw a cached frame
    CloseableReference<Bitmap> bitmap = mBitmapFrameCache.getCachedFrame(frameNumber);
    if (drawBitmapNotifyAndClose(frameNumber, bitmap, canvas, FRAME_TYPE_CACHED)) {
      return true;
    }

    // Try and reuse a bitmap
    bitmap = mBitmapFrameCache.getBitmapToReuseForFrame(frameNumber, mBitmapWidth, mBitmapHeight);
    // Try to render the frame and draw on the canvas immediately after
    if (renderFrameInBitmap(frameNumber, bitmap) &&
        drawBitmapNotifyAndClose(frameNumber, bitmap, canvas, FRAME_TYPE_REUSED)) {
      return true;
    }

    // Create a new bitmap
    bitmap = mPlatformBitmapFactory.createBitmap(mBitmapWidth, mBitmapHeight, mBitmapConfig);
    // Try to render the frame and draw on the canvas immediately after
    if (renderFrameInBitmap(frameNumber, bitmap) &&
        drawBitmapNotifyAndClose(frameNumber, bitmap, canvas, FRAME_TYPE_CREATED)) {
      return true;
    }

    // Draw a fallback frame if possible
    bitmap = mBitmapFrameCache.getFallbackFrame(frameNumber);
    if (drawBitmapNotifyAndClose(frameNumber, bitmap, canvas, FRAME_TYPE_FALLBACK)) {
      return true;
    }

    // We could not draw anything
    if (mFrameListener != null) {
      mFrameListener.onFrameDropped(this, frameNumber);
    }
    return false;
  }

  @Override
  public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
    mPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    mPaint.setColorFilter(colorFilter);
  }

  @Override
  public void setBounds(@Nullable Rect bounds) {
    mBounds = bounds;
    mBitmapFrameRenderer.setBounds(bounds);
    updateBitmapDimensions();
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
  public int getSizeInBytes() {
    return mBitmapFrameCache.getSizeInBytes();
  }

  @Override
  public void clear() {
    mBitmapFrameCache.clear();
  }

  @Override
  public void onInactive() {
    clear();
  }

  private void updateBitmapDimensions() {
    // Calculate the correct bitmap dimensions
    mBitmapWidth = mBitmapFrameRenderer.getIntrinsicWidth();
    if (mBitmapWidth == INTRINSIC_DIMENSION_UNSET) {
      mBitmapWidth = mBounds == null ? 0 : mBounds.width();
    }

    mBitmapHeight = mBitmapFrameRenderer.getIntrinsicWidth();
    if (mBitmapHeight == INTRINSIC_DIMENSION_UNSET) {
      mBitmapHeight = mBounds == null ? 0 : mBounds.height();
    }
  }

  /**
   * Try to render the frame to the given target bitmap. If the rendering fails, the target bitmap
   * reference will be closed and false is returned. If rendering succeeds, the target bitmap
   * reference can be drawn and has to be manually closed after drawing has been completed.
   *
   * @param frameNumber the frame number to render
   * @param targetBitmap the target bitmap
   * @return true if rendering successful
   */
  private boolean renderFrameInBitmap(
      int frameNumber,
      @Nullable CloseableReference<Bitmap> targetBitmap) {
    if (!CloseableReference.isValid(targetBitmap)) {
      return false;
    }
    // Render the image
    boolean frameRendered =
        mBitmapFrameRenderer.renderFrame(frameNumber, targetBitmap.get());
    if (!frameRendered) {
      CloseableReference.closeSafely(targetBitmap);
    }
    return frameRendered;
  }

  /**
   * Helper method that draws the given bitmap on the canvas respecting the bounds (if set). It will
   * automatically close the given bitmap reference.
   *
   * If rendering was successful, it notifies the cache that the frame has been rendered with the
   * given bitmap. In addition, it will notify the {@link FrameListener} if set.
   *
   * @param frameNumber the current frame number passed to the cache
   * @param bitmapReference the bitmap to draw
   * @param canvas the canvas to draw an
   * @param frameType the {@link FrameType} to be rendered
   * @return true if the bitmap has been drawn
   */
  private boolean drawBitmapNotifyAndClose(
      int frameNumber,
      @Nullable CloseableReference<Bitmap> bitmapReference,
      Canvas canvas,
      @FrameType int frameType) {
    if (!CloseableReference.isValid(bitmapReference)) {
      return false;
    }
    try {
      if (mBounds == null) {
        canvas.drawBitmap(bitmapReference.get(), 0f, 0f, mPaint);
      } else {
        canvas.drawBitmap(bitmapReference.get(), null, mBounds, mPaint);
      }
      // The calee has to clone the reference if it needs to hold on to the bitmap
      mBitmapFrameCache.onFrameRendered(
          frameNumber,
          bitmapReference,
          frameType);
    } finally {
      CloseableReference.closeSafely(bitmapReference);
    }
    if (mFrameListener != null) {
      mFrameListener.onFrameDrawn(this, frameNumber, frameType);
    }
    return true;
  }
}
