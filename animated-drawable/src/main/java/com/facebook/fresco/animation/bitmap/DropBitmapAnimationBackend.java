/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;

import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck;
import com.facebook.fresco.animation.backend.AnimationInformation;
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparationStrategy;
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparer;
import com.facebook.fresco.animation.bitmap.preparation.DropBitmapFramePreparer;
import com.facebook.fresco.animation.bitmap.preparation.FixedNumberBitmapFramePreparationStrategy;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;

import java.lang.annotation.Retention;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Different from {@link BitmapAnimationBackend}
 * 1. add AnimatedDrawable2 to update the dropped time offset;
 * 2. cancel {@code FRAME_TYPE_REUSED}、{@code FRAME_TYPE_CREATED} which may
 * cause ui stuck
 * 3. add sDropExecutor, a last in first out Executor
 * 4. use {@link DropBitmapFramePreparer}
 * 5. mPreviewRef can be AnimatedImageResult#mPreviewBitmap
 */
public class DropBitmapAnimationBackend implements AnimationBackend,
        AnimationBackendDelegateWithInactivityCheck.InactivityListener {

  /**
   * Frame type that has been drawn. Can be used for logging.
   */
  @Retention(SOURCE)
  @IntDef({
          FRAME_TYPE_UNKNOWN,
          FRAME_TYPE_CACHED,
          FRAME_TYPE_FALLBACK,
          FRAME_TYPE_PREVIEW
  })
  public @interface FrameType {

  }

  public static final int FRAME_TYPE_UNKNOWN = -1;
  public static final int FRAME_TYPE_CACHED = 0;
  public static final int FRAME_TYPE_FALLBACK = 1;
  public static final int FRAME_TYPE_PREVIEW = 2;

  private static final long NO_VALUE = -1;

  private final AnimatedDrawable2 mAnimatabledDrawable;
  private final BitmapFrameCache mBitmapFrameCache;
  private final AnimationInformation mAnimationInformation;
  private final BitmapFrameRenderer mBitmapFrameRenderer;
  @Nullable
  private final BitmapFramePreparationStrategy mBitmapFramePreparationStrategy;
  @Nullable
  private final BitmapFramePreparer mBitmapFramePreparer;
  private CloseableReference<Bitmap> mPreviewRef;
  private final Paint mPaint;

  @Nullable
  private Rect mBounds;
  private int mBitmapWidth;
  private int mBitmapHeight;
  private long mFrameOffset = 0;
  private long mLastNoDrawTime = NO_VALUE;

  private static ExecutorService sDropExecutor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS,
          new PriorityBlockingQueue<Runnable>());

  public DropBitmapAnimationBackend(
          PlatformBitmapFactory factory,
          AnimatedDrawable2 animatable,
          BitmapFrameCache bitmapFrameCache,
          AnimationInformation animationInformation,
          BitmapFrameRenderer bitmapFrameRenderer,
          CloseableReference<Bitmap> previewBitmap) {
    mAnimatabledDrawable = animatable;
    mBitmapFrameCache = bitmapFrameCache;
    mAnimationInformation = animationInformation;
    mBitmapFrameRenderer = bitmapFrameRenderer;
    mBitmapFramePreparationStrategy = new FixedNumberBitmapFramePreparationStrategy(2);
    mBitmapFramePreparer = new DropBitmapFramePreparer(factory, bitmapFrameRenderer,
            Bitmap.Config.ARGB_8888, sDropExecutor);
    mPreviewRef = previewBitmap;

    mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    updateBitmapDimensions();
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
    boolean drawn = drawFrameOrFallback(canvas, frameNumber, FRAME_TYPE_CACHED);
    if (!drawn) {
      long nowTime = now();
      if (mLastNoDrawTime != NO_VALUE) {
        mFrameOffset += mLastNoDrawTime - nowTime;
        updateOffset();
      }
      mLastNoDrawTime = nowTime;
    } else {
      mLastNoDrawTime = NO_VALUE;
    }

    // Prepare next frames
    if (mBitmapFramePreparationStrategy != null && mBitmapFramePreparer != null) {
      mBitmapFramePreparationStrategy.prepareFrames(
              mBitmapFramePreparer,
              mBitmapFrameCache,
              this,
              frameNumber);
    }

    return drawn;
  }

  private void updateOffset() {
    mAnimatabledDrawable.setFrameSchedulingOffsetMs(mFrameOffset);
  }

  private long now() {
    return SystemClock.uptimeMillis();
  }

  private boolean drawFrameOrFallback(Canvas canvas, int frameNumber, @FrameType int frameType) {
    CloseableReference<Bitmap> bitmapReference = null;
    boolean drawn;
    int nextFrameType = FRAME_TYPE_UNKNOWN;

    try {
      switch (frameType) {
        case FRAME_TYPE_CACHED:
          bitmapReference = mBitmapFrameCache.getCachedFrame(frameNumber);
          drawn = drawBitmapAndCache(frameNumber, bitmapReference, canvas, FRAME_TYPE_CACHED);
          nextFrameType = FRAME_TYPE_FALLBACK;
          break;

        case FRAME_TYPE_FALLBACK:
          bitmapReference = mBitmapFrameCache.getFallbackFrame(frameNumber);
          drawn = drawBitmapAndCache(frameNumber, bitmapReference, canvas, FRAME_TYPE_FALLBACK);
          nextFrameType = FRAME_TYPE_PREVIEW;
          break;

        case FRAME_TYPE_PREVIEW:
          drawn = drawPreviewIfNeeded(canvas);
          break;

        default:
          return false;
      }
    } finally {
      CloseableReference.closeSafely(bitmapReference);
    }

    if (drawn || nextFrameType == FRAME_TYPE_UNKNOWN) {
      return drawn;
    } else {
      return drawFrameOrFallback(canvas, frameNumber, nextFrameType);
    }
  }

  private boolean drawPreviewIfNeeded(Canvas canvas) {
    Bitmap cover;
    if (mPreviewRef == null || (cover = mPreviewRef.get()) == null) {
      return false;
    }
    if (mBounds == null) {
      canvas.drawBitmap(cover, 0f, 0f, mPaint);
    } else {
      canvas.drawBitmap(cover, null, mBounds, mPaint);
    }
    return true;
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
    CloseableReference.closeSafely(mPreviewRef);
  }

  @Override
  public void onInactive() {
    clear();
  }

  private void updateBitmapDimensions() {
    // Calculate the correct bitmap dimensions
    mBitmapWidth = mBitmapFrameRenderer.getIntrinsicWidth();
    if (mBitmapWidth == INTRINSIC_DIMENSION_UNSET) {
      mBitmapWidth = mBounds == null ? INTRINSIC_DIMENSION_UNSET : mBounds.width();
    }

    mBitmapHeight = mBitmapFrameRenderer.getIntrinsicHeight();
    if (mBitmapHeight == INTRINSIC_DIMENSION_UNSET) {
      mBitmapHeight = mBounds == null ? INTRINSIC_DIMENSION_UNSET : mBounds.height();
    }
  }

  /**
   * Helper method that draws the given bitmap on the canvas respecting the bounds (if set).
   * <p>
   * If rendering was successful, it notifies the cache that the frame has been rendered with the
   * given bitmap.
   *
   * @param frameNumber     the current frame number passed to the cache
   * @param bitmapReference the bitmap to draw
   * @param canvas          the canvas to draw an
   * @param frameType       the {@link FrameType} to be rendered
   * @return true if the bitmap has been drawn
   */
  private boolean drawBitmapAndCache(
          int frameNumber,
          @Nullable CloseableReference<Bitmap> bitmapReference,
          Canvas canvas,
          @FrameType int frameType) {
    if (!CloseableReference.isValid(bitmapReference)) {
      return false;
    }
    if (mBounds == null) {
      canvas.drawBitmap(bitmapReference.get(), 0f, 0f, mPaint);
    } else {
      canvas.drawBitmap(bitmapReference.get(), null, mBounds, mPaint);
    }

    // Notify the cache that a frame has been rendered.
    // We should not cache fallback frames since they do not represent the actual frame.
    if (frameType != FRAME_TYPE_FALLBACK) {
      mBitmapFrameCache.onFrameRendered(
              frameNumber,
              bitmapReference,
              frameType);
    }

    return true;
  }
}
