/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite;

import android.graphics.Movie;
import com.facebook.animated.giflite.decoder.GifMetadataDecoder;
import com.facebook.animated.giflite.draw.MovieAnimatedImage;
import com.facebook.animated.giflite.draw.MovieDrawer;
import com.facebook.animated.giflite.draw.MovieFrame;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;
import java.io.IOException;
import java.io.InputStream;

/** A simple Gif decoder that uses Android's {@link Movie} class to decode Gif images. */
public class GifDecoder implements ImageDecoder {

  @Override
  public CloseableImage decode(
      final EncodedImage encodedImage,
      int length,
      QualityInfo qualityInfo,
      ImageDecodeOptions options) {
    InputStream is = encodedImage.getInputStream();
    try {
      Movie movie = Movie.decodeStream(is);
      MovieDrawer drawer = new MovieDrawer(movie);

      is.reset();

      GifMetadataDecoder decoder = GifMetadataDecoder.create(is);

      MovieFrame[] frames = new MovieFrame[decoder.getFrameCount()];
      int currTime = 0;
      for (int frameNumber = 0, N = frames.length; frameNumber < N; frameNumber++) {
        int frameDuration = decoder.getFrameDurationMs(frameNumber);
        currTime += frameDuration;
        frames[frameNumber] =
            new MovieFrame(
                drawer,
                currTime,
                frameDuration,
                movie.width(),
                movie.height(),
                translateFrameDisposal(decoder.getFrameDisposal(frameNumber)));
      }

      return new CloseableAnimatedImage(
          AnimatedImageResult.forAnimatedImage(
              new MovieAnimatedImage(
                  frames, encodedImage.getSize(), movie.duration(), decoder.getLoopCount())));
    } catch (IOException e) {
      throw new RuntimeException("Error while decoding gif", e);
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }

  private static AnimatedDrawableFrameInfo.DisposalMethod translateFrameDisposal(int raw) {
    switch (raw) {
      case 2: // restore to background
        return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_TO_BACKGROUND;
      case 3: // restore to previous
        return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_TO_PREVIOUS;
      case 1: // do not dispose
        // fallthrough
      default: // unspecified
        return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT;
    }
  }
}
