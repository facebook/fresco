/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.draw;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;

/**
 * Simple wrapper for an animated image frame back by {@link MovieDrawer}. All {@link MovieFrame}
 * for the same {@link MovieAnimatedImage} will be backed by the same {@link MovieDrawer}.
 */
public class MovieFrame implements AnimatedImageFrame {

  private final MovieDrawer mMovieDrawer;
  private final int mFrameStart;
  private final int mFrameDuration;
  private final int mFrameWidth;
  private final int mFrameHeight;
  private final AnimatedDrawableFrameInfo.DisposalMethod mDisposalMethod;

  public MovieFrame(
      MovieDrawer movieDrawer,
      int frameStart,
      int frameDuration,
      int frameWidth,
      int frameHeight,
      AnimatedDrawableFrameInfo.DisposalMethod disposalMode) {
    mMovieDrawer = movieDrawer;
    mFrameStart = frameStart;
    mFrameDuration = frameDuration;
    mFrameWidth = frameWidth;
    mFrameHeight = frameHeight;
    mDisposalMethod = disposalMode;
  }

  @Override
  public void dispose() {}

  @Override
  public void renderFrame(int w, int h, Bitmap bitmap) {
    mMovieDrawer.drawFrame(mFrameStart, w, h, bitmap);
  }

  @Override
  public int getDurationMs() {
    return mFrameDuration;
  }

  @Override
  public int getWidth() {
    return mFrameWidth;
  }

  @Override
  public int getHeight() {
    return mFrameHeight;
  }

  @Override
  public int getXOffset() {
    return 0;
  }

  @Override
  public int getYOffset() {
    return 0;
  }

  public AnimatedDrawableFrameInfo.DisposalMethod getDisposalMode() {
    return mDisposalMethod;
  }
}
