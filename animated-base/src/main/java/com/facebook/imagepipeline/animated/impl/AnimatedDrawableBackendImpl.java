/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.impl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * An {@link AnimatedDrawableBackend} that renders {@link AnimatedImage}.
 */
public class AnimatedDrawableBackendImpl implements AnimatedDrawableBackend {

  private final AnimatedDrawableUtil mAnimatedDrawableUtil;

  private final AnimatedImageResult mAnimatedImageResult;
  private final AnimatedImage mAnimatedImage;
  private final Rect mRenderedBounds;
  private final int[] mFrameDurationsMs;
  private final int[] mFrameTimestampsMs;
  private final int mDurationMs;
  private final AnimatedDrawableFrameInfo[] mFrameInfos;
  private final Rect mRenderSrcRect = new Rect();
  private final Rect mRenderDstRect = new Rect();
  private final boolean mDownscaleFrameToDrawableDimensions;

  @GuardedBy("this")
  private @Nullable Bitmap mTempBitmap;

  public AnimatedDrawableBackendImpl(
      AnimatedDrawableUtil animatedDrawableUtil,
      AnimatedImageResult animatedImageResult,
      Rect bounds,
      boolean downscaleFrameToDrawableDimensions) {
    mAnimatedDrawableUtil = animatedDrawableUtil;
    mAnimatedImageResult = animatedImageResult;
    mAnimatedImage = animatedImageResult.getImage();
    mFrameDurationsMs = mAnimatedImage.getFrameDurations();
    mAnimatedDrawableUtil.fixFrameDurations(mFrameDurationsMs);
    mDurationMs = mAnimatedDrawableUtil.getTotalDurationFromFrameDurations(mFrameDurationsMs);
    mFrameTimestampsMs = mAnimatedDrawableUtil.getFrameTimeStampsFromDurations(mFrameDurationsMs);
    mRenderedBounds = getBoundsToUse(mAnimatedImage, bounds);
    mDownscaleFrameToDrawableDimensions = downscaleFrameToDrawableDimensions;
    mFrameInfos = new AnimatedDrawableFrameInfo[mAnimatedImage.getFrameCount()];
    for (int i = 0; i < mAnimatedImage.getFrameCount(); i++) {
      mFrameInfos[i] = mAnimatedImage.getFrameInfo(i);
    }
  }

  private static Rect getBoundsToUse(AnimatedImage image, Rect targetBounds) {
    if (targetBounds == null) {
      return new Rect(0, 0, image.getWidth(), image.getHeight());
    }
    return new Rect(
        0,
        0,
        Math.min(targetBounds.width(), image.getWidth()),
        Math.min(targetBounds.height(), image.getHeight()));
  }

  @Override
  public AnimatedImageResult getAnimatedImageResult() {
    return mAnimatedImageResult;
  }

  @Override
  public int getDurationMs() {
    return mDurationMs;
  }

  @Override
  public int getFrameCount() {
    return mAnimatedImage.getFrameCount();
  }

  @Override
  public int getLoopCount() {
    return mAnimatedImage.getLoopCount();
  }

  @Override
  public int getWidth() {
    return mAnimatedImage.getWidth();
  }

  @Override
  public int getHeight() {
    return mAnimatedImage.getHeight();
  }

  @Override
  public int getRenderedWidth() {
    return mRenderedBounds.width();
  }

  @Override
  public int getRenderedHeight() {
    return mRenderedBounds.height();
  }

  @Override
  public AnimatedDrawableFrameInfo getFrameInfo(int frameNumber) {
    return mFrameInfos[frameNumber];
  }

  @Override
  public int getFrameForTimestampMs(int timestampMs) {
    return mAnimatedDrawableUtil.getFrameForTimestampMs(mFrameTimestampsMs, timestampMs);
  }

  @Override
  public int getTimestampMsForFrame(int frameNumber) {
    Preconditions.checkElementIndex(frameNumber, mFrameTimestampsMs.length);
    return mFrameTimestampsMs[frameNumber];
  }

  @Override
  public int getDurationMsForFrame(int frameNumber) {
    return mFrameDurationsMs[frameNumber];
  }

  @Override
  public int getFrameForPreview() {
    return mAnimatedImageResult.getFrameForPreview();
  }

  @Override
  public AnimatedDrawableBackend forNewBounds(Rect bounds) {
    Rect boundsToUse = getBoundsToUse(mAnimatedImage, bounds);
    if (boundsToUse.equals(mRenderedBounds)) {
      // Actual bounds aren't changed.
      return this;
    }
    return new AnimatedDrawableBackendImpl(
        mAnimatedDrawableUtil, mAnimatedImageResult, bounds, mDownscaleFrameToDrawableDimensions);
  }

  @Override
  public synchronized int getMemoryUsage() {
    int bytes = 0;
    if (mTempBitmap != null) {
      bytes += mAnimatedDrawableUtil.getSizeOfBitmap(mTempBitmap);
    }
    bytes += mAnimatedImage.getSizeInBytes();
    return bytes;
  }

  @Override
  public CloseableReference<Bitmap> getPreDecodedFrame(int frameNumber) {
    return mAnimatedImageResult.getDecodedFrame(frameNumber);
  }

  @Override
  public boolean hasPreDecodedFrame(int index) {
    return mAnimatedImageResult.hasDecodedFrame(index);
  }

  @Override
  public void renderFrame(int frameNumber, Canvas canvas) {
    AnimatedImageFrame frame  = mAnimatedImage.getFrame(frameNumber);
    try {
      if (mAnimatedImage.doesRenderSupportScaling()) {
        renderImageSupportsScaling(canvas, frame);
      } else {
        renderImageDoesNotSupportScaling(canvas, frame);
      }
    } finally {
      frame.dispose();
    }
  }

  private synchronized void prepareTempBitmapForThisSize(int width, int height) {
    // Different gif frames can be different size,
    // So we need to ensure we can fit next frame to temporary bitmap
    if (mTempBitmap != null
        && (mTempBitmap.getWidth() < width || mTempBitmap.getHeight() < height)) {
      clearTempBitmap();
    }
    if (mTempBitmap == null) {
      mTempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }
    mTempBitmap.eraseColor(Color.TRANSPARENT);
  }

  private void renderImageSupportsScaling(Canvas canvas, AnimatedImageFrame frame) {
    double xScale = (double) mRenderedBounds.width() / (double) mAnimatedImage.getWidth();
    double yScale = (double) mRenderedBounds.height() / (double) mAnimatedImage.getHeight();

    int frameWidth = (int) Math.round(frame.getWidth() * xScale);
    int frameHeight = (int) Math.round(frame.getHeight() * yScale);
    int xOffset = (int) (frame.getXOffset() * xScale);
    int yOffset = (int) (frame.getYOffset() * yScale);

    synchronized (this) {
      int renderedWidth = mRenderedBounds.width();
      int renderedHeight = mRenderedBounds.height();
      // Update the temp bitmap to be >= rendered dimensions
      prepareTempBitmapForThisSize(renderedWidth, renderedHeight);
      frame.renderFrame(frameWidth, frameHeight, mTempBitmap);
      // Temporary bitmap can be bigger than frame, so we should draw only rendered area of bitmap
      mRenderSrcRect.set(0, 0, renderedWidth, renderedHeight);
      mRenderDstRect.set(xOffset, yOffset, xOffset + renderedWidth, yOffset + renderedHeight);

      canvas.drawBitmap(mTempBitmap, mRenderSrcRect, mRenderDstRect, null);
    }
  }

  private void renderImageDoesNotSupportScaling(Canvas canvas, AnimatedImageFrame frame) {
    int frameWidth, frameHeight, xOffset, yOffset;
    if (mDownscaleFrameToDrawableDimensions) {
      final int fittedWidth = Math.min(frame.getWidth(), canvas.getWidth());
      final int fittedHeight = Math.min(frame.getHeight(), canvas.getHeight());

      final float scaleX = (float) frame.getWidth() / (float) fittedWidth;
      final float scaleY = (float) frame.getHeight() / (float) fittedHeight;
      final float scale = Math.max(scaleX, scaleY);

      frameWidth = (int) (frame.getWidth() / scale);
      frameHeight = (int) (frame.getHeight() / scale);
      xOffset = (int) (frame.getXOffset() / scale);
      yOffset = (int) (frame.getYOffset() / scale);
    } else {
      frameWidth = frame.getWidth();
      frameHeight = frame.getHeight();
      xOffset = frame.getXOffset();
      yOffset = frame.getYOffset();
    }

    synchronized (this) {
      prepareTempBitmapForThisSize(frameWidth, frameHeight);
      frame.renderFrame(frameWidth, frameHeight, mTempBitmap);

      canvas.save();
      canvas.translate(xOffset, yOffset);
      canvas.drawBitmap(mTempBitmap, 0, 0, null);
      canvas.restore();
    }
  }

  @Override
  public synchronized void dropCaches() {
    clearTempBitmap();
  }

  private synchronized void clearTempBitmap() {
    if (mTempBitmap != null) {
      mTempBitmap.recycle();
      mTempBitmap = null;
    }
  }
}
