/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.animated.giflite.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.facebook.animated.giflite.decoder.GifMetadataDecoder;
import com.facebook.fresco.animation.backend.AnimationBackend;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

public class GifAnimationBackend implements AnimationBackend {

  private final GifMetadataDecoder mGifDecoder;
  private final Movie mMovie;
  private final int[] mFrameStartTimes;

  private float mMidX;
  private float mMidY;

  public static GifAnimationBackend create(String filePath) throws IOException {
    InputStream is = null;
    try {
      is = new BufferedInputStream(new FileInputStream(filePath));
      is.mark(Integer.MAX_VALUE);

      GifMetadataDecoder decoder = GifMetadataDecoder.create(is);
      is.reset();

      Movie movie = Movie.decodeStream(is);
      return new GifAnimationBackend(decoder, movie);
    } finally {
      closeSilently(is);
    }
  }

  private GifAnimationBackend(GifMetadataDecoder decoder, Movie movie) {
    mGifDecoder = decoder;
    mMovie = movie;
    mFrameStartTimes = new int[decoder.getFrameCount()];
  }

  @Override
  public boolean drawFrame(Drawable parent, Canvas canvas, int frameNumber) {
    mMovie.setTime(getFrameStartTime(frameNumber));
    mMovie.draw(canvas, mMidX, mMidY);
    return true;
  }

  @Override
  public void setAlpha(int alpha) {
    // unimplemented
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    // unimplemented
  }

  @Override
  public void setBounds(Rect bounds) {
    scale(
        bounds.right - bounds.left /* viewPortWidth */,
        bounds.bottom - bounds.top /* viewPortHeight */,
        mMovie.width() /* sourceWidth */,
        mMovie.height() /* sourceHeight */);
  }

  @Override
  public int getIntrinsicWidth() {
    return mMovie.width();
  }

  @Override
  public int getIntrinsicHeight() {
    return mMovie.height();
  }

  @Override
  public int getSizeInBytes() {
    return 0; // no cached data
  }

  @Override
  public void clear() {
    // unimplemented
  }

  @Override
  public int getFrameCount() {
    return mGifDecoder.getFrameCount();
  }

  @Override
  public int getFrameDurationMs(int frameNumber) {
    return mGifDecoder.getFrameDurationMs(frameNumber);
  }

  @Override
  public int getLoopCount() {
    return mGifDecoder.getLoopCount();
  }

  private int getFrameStartTime(int frameNumber) {
    if (frameNumber == 0) {
      return 0;
    }
    if (mFrameStartTimes[frameNumber] != 0) {
      return mFrameStartTimes[frameNumber];
    }
    for (int i = 0; i < frameNumber; i++) {
      mFrameStartTimes[frameNumber] += mGifDecoder.getFrameDurationMs(i);
    }
    return mFrameStartTimes[frameNumber];
  }

  /**
   * Measures the source, and sets the size based on them. Maintains aspect ratio of source, and
   * ensures that screen is filled in at least one dimension.
   *
   * <p>Adapted from com.facebook.cameracore.common.RenderUtil#calculateFitRect
   *
   * @param viewPortWidth the width of the display
   * @param viewPortHeight the height of the display
   * @param sourceWidth the width of the video
   * @param sourceHeight the height of the video
   */
  private void scale(int viewPortWidth, int viewPortHeight, int sourceWidth, int sourceHeight) {

    float inputRatio = ((float) sourceWidth) / sourceHeight;
    float outputRatio = ((float) viewPortWidth) / viewPortHeight;

    int scaledWidth = viewPortWidth;
    int scaledHeight = viewPortHeight;
    if (outputRatio > inputRatio) {
      // Not enough width to fill the output. (Black bars on left and right.)
      scaledWidth = (int) (viewPortHeight * inputRatio);
      scaledHeight = viewPortHeight;
    } else if (outputRatio < inputRatio) {
      // Not enough height to fill the output. (Black bars on top and bottom.)
      scaledHeight = (int) (viewPortWidth / inputRatio);
      scaledWidth = viewPortWidth;
    }
    float scale = scaledWidth / (float) sourceWidth;

    mMidX = ((viewPortWidth - scaledWidth) / 2f) / scale;
    mMidY = ((viewPortHeight - scaledHeight) / 2f) / scale;
  }

  private static void closeSilently(@Nullable Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException ignored) {
      // ignore
    }
  }
}
