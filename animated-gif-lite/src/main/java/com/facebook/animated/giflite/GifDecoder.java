/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;
import java.util.ArrayList;
import java.util.List;

/** A simple Gif decoder that uses Android's {@link Movie} class to decode Gif images. */
public class GifDecoder implements ImageDecoder {

  private static final int FRAME_TIME_AT_60_FPS = 17; // ~17ms per frame at 60fps

  @Override
  public CloseableImage decode(
      final EncodedImage encodedImage,
      int length,
      QualityInfo qualityInfo,
      ImageDecodeOptions options) {

    Movie movie = Movie.decodeStream(encodedImage.getInputStream());
    MovieDrawer drawer = new MovieDrawer(movie);

    List<Integer> frameStartTimes = getMovieFrameStartTimes(movie);

    MovieFrame[] frames = new MovieFrame[frameStartTimes.size()];
    for (int i = 0, N = frameStartTimes.size(); i < N; i++) {
      int frameStart = frameStartTimes.get(i);
      int frameDuration = (i == N - 1 ? movie.duration() : frameStartTimes.get(i + 1)) - frameStart;
      frames[i] = new MovieFrame(drawer, frameStart, frameDuration, movie.width(), movie.height());
    }

    return new CloseableAnimatedImage(
        AnimatedImageResult.forAnimatedImage(
            new MovieAnimatedImage(frames, encodedImage.getSize(), movie.duration())));
  }

  /**
   * Determines the start time for every frame in {@link Movie} in millis.
   *
   * @param movie the movie to get the frame start times from
   * @return a list of start times for each frame
   */
  private static List<Integer> getMovieFrameStartTimes(Movie movie) {
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
    for (int time = 0; time < movie.duration(); time += FRAME_TIME_AT_60_FPS) {
      if (movie.setTime(time) || time == 0) {
        frameStartTimes.add(time);
        movie.draw(canvas, 0, 0);
      }
    }
    bitmap.recycle();
    return frameStartTimes;
  }
}
