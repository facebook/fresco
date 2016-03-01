/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.testing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;

/**
 * Implementation of {@link AnimatedDrawableBackend} for unit tests.
 */
public class TestAnimatedDrawableBackend implements AnimatedDrawableBackend {

  private final int mWidth;
  private final int mHeight;
  private final int[] mFrameDurations;
  private final int[] mAccumulatedDurationsMs;

  private int mDropCachesCallCount;

  public TestAnimatedDrawableBackend(int width, int height, int[] frameDurations) {
    mWidth = width;
    mHeight = height;
    mFrameDurations = frameDurations;
    mAccumulatedDurationsMs = new int[mFrameDurations.length];
    int accumulatedDurationMs = 0;
    for (int i = 0; i < mAccumulatedDurationsMs.length; i++) {
      mAccumulatedDurationsMs[i] = accumulatedDurationMs + mFrameDurations[i];
      accumulatedDurationMs = mAccumulatedDurationsMs[i];
    }
  }

  public static int pixelValue(int frameNumber, int x, int y) {
    return ((frameNumber & 0xff) << 16) | ((x & 0xff) << 8) | ((y & 0xff));
  }

  @Override
  public AnimatedImageResult getAnimatedImageResult() {
    return null;
  }

  @Override
  public int getDurationMs() {
    return mAccumulatedDurationsMs[mAccumulatedDurationsMs.length - 1];
  }

  @Override
  public int getFrameCount() {
    return mFrameDurations.length;
  }

  @Override
  public int getLoopCount() {
    return 0;
  }

  @Override
  public int getWidth() {
    return mWidth;
  }

  @Override
  public int getHeight() {
    return mHeight;
  }

  @Override
  public int getRenderedWidth() {
    return mWidth;
  }

  @Override
  public int getRenderedHeight() {
    return mHeight;
  }

  @Override
  public AnimatedDrawableFrameInfo getFrameInfo(int frameNumber) {
    return new AnimatedDrawableFrameInfo(
        frameNumber,
        0,
        0,
        mWidth,
        mHeight,
        false,
        AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT);
  }

  @Override
  public void renderFrame(int frameNumber, Canvas canvas) {
    int[] pixels = new int[mWidth * mHeight];
    for (int i = 0; i < pixels.length; i++) {
      // We store the frame number in the R, the x in the G, and the y in the B.
      int x = i % mWidth;
      int y = i / mWidth;
      pixels[i] = pixelValue(frameNumber, x, y);
    }
    Bitmap bitmap = Bitmap.createBitmap(pixels, mWidth, mHeight, Bitmap.Config.ARGB_8888);
    canvas.drawBitmap(bitmap, 0, 0, null);
  }

  @Override
  public int getFrameForTimestampMs(int timestampMs) {
    int accumulator = 0;
    for (int i = 0; i < mFrameDurations.length; i++) {
      if (timestampMs < accumulator + mFrameDurations[i]) {
        return i;
      }
      accumulator += mFrameDurations[i];
    }
    return mFrameDurations.length - 1;
  }

  @Override
  public int getTimestampMsForFrame(int frameNumber) {
    return frameNumber == 0 ? 0 : mAccumulatedDurationsMs[frameNumber - 1];
  }

  @Override
  public int getDurationMsForFrame(int frameNumber) {
    return mFrameDurations[frameNumber];
  }

  @Override
  public int getFrameForPreview() {
    return 0;
  }

  @Override
  public AnimatedDrawableBackend forNewBounds(Rect bounds) {
    return this;
  }

  @Override
  public int getMemoryUsage() {
    return 0;
  }

  @Override
  public CloseableReference<Bitmap> getPreDecodedFrame(int frameNumber) {
    return null;
  }

  @Override
  public boolean hasPreDecodedFrame(int frameNumber) {
    return false;
  }

  public int getDropCachesCallCount() {
    return mDropCachesCallCount;
  }

  @Override
  public void dropCaches() {
    mDropCachesCallCount++;
  }
}
