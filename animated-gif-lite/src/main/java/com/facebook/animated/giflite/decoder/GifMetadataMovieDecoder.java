/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.animated.giflite.decoder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import java.util.ArrayList;
import java.util.List;

/* package-private */ class GifMetadataMovieDecoder implements GifMetadataDecoder {

  private static final int FRAME_TIME_AT_60_FPS = 17; // ~17ms per frame at 60fps

  private final Movie mMovie;
  private final List<Integer> mFrameDurations = new ArrayList<>();

  GifMetadataMovieDecoder(Movie movie) {
    mMovie = movie;
  }

  @Override
  public void decode() {
    determineFrameDurations();
  }

  @Override
  public int getFrameCount() {
    return mFrameDurations.size();
  }

  @Override
  public int getLoopCount() {
    return AnimatedImage.LOOP_COUNT_INFINITE;
  }

  @Override
  public AnimatedDrawableFrameInfo.DisposalMethod getFrameDisposal(int frameNumber) {
    return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT;
  }

  @Override
  public int getFrameDurationMs(int frameNumber) {
    return mFrameDurations.get(frameNumber);
  }

  /** Determines the start time for every frame in {@link Movie} in millis. */
  private void determineFrameDurations() {
    /*
     * Movie does not have an interface to query for frames. Instead, what is in use here is a
     * hack.
     * It appears that Movie#setTime(long) will return true if there is a new frame needs to be
     * drawn. This requires that we must draw the frame when setTime returns true, or it will
     * continue to return true for all times. We can do this with minimal performance impact by
     * drawing to a 1x1 bitmap.
     */
    Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);

    List<Integer> frameStartTimes = new ArrayList<>();
    for (int time = 0; time < mMovie.duration(); time += FRAME_TIME_AT_60_FPS) {
      if (mMovie.setTime(time) || time == 0) {
        mMovie.draw(canvas, 0, 0);
        frameStartTimes.add(time);
      }
    }

    for (int i = 0, N = frameStartTimes.size() - 1; i < N; i++) {
      mFrameDurations.add(frameStartTimes.get(i + 1) - frameStartTimes.get(i));
    }
    mFrameDurations.add(mMovie.duration() - frameStartTimes.get(frameStartTimes.size() - 1));
  }
}
