/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.support.annotation.Nullable;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;

/**
 * Simple wrapper for an animated image frame back by {@link Movie}. All {@link MovieFrame} for the
 * same {@link MovieAnimatedImage} will be backed by the same {@link Movie} and can therefore not be
 * used in parallel.
 */
class MovieFrame implements AnimatedImageFrame {

  private final Movie mMovie;
  private final MovieScaleHolder mScaleHolder;
  private final int mFrameStart;
  private final int mFrameDuration;
  private @Nullable Canvas mCanvas;

  public MovieFrame(Movie movie, MovieScaleHolder scaleHolder, int frameStart, int frameDuration) {
    mMovie = movie;
    mScaleHolder = scaleHolder;
    mFrameStart = frameStart;
    mFrameDuration = frameDuration;
  }

  @Override
  public void dispose() {}

  @Override
  public void renderFrame(int w, int h, Bitmap bitmap) {
    mMovie.setTime(mFrameStart);

    if (mCanvas == null) {
      mCanvas = new Canvas(bitmap);
    } else {
      mCanvas.setBitmap(bitmap);
    }

    mScaleHolder.updateViewPort(w, h);
    mCanvas.save();
    mCanvas.scale(mScaleHolder.getScale(), mScaleHolder.getScale());
    mMovie.draw(mCanvas, mScaleHolder.getLeft(), mScaleHolder.getTop());
    mCanvas.restore();
  }

  @Override
  public int getDurationMs() {
    return mFrameDuration;
  }

  @Override
  public int getWidth() {
    return mMovie.width();
  }

  @Override
  public int getHeight() {
    return mMovie.height();
  }

  @Override
  public int getXOffset() {
    return 0;
  }

  @Override
  public int getYOffset() {
    return 0;
  }
}
