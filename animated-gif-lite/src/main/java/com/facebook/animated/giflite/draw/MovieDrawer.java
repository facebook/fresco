/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.animated.giflite.draw;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import javax.annotation.Nullable;

/**
 * Pronounced Draw-er Draws frames of a {@link Movie} to a bitmap. All methods are synchronized, so
 * can be used in parallel. The underlying {@link #mMovie} is not threadsafe, and should therefore
 * not be accessed outside of {@link MovieDrawer}. Attempts to optimize work done by the drawing
 * {@link Canvas} by detecting if the underlying {@link Bitmap} has changed.
 */
public class MovieDrawer {

  private final Movie mMovie;
  private final MovieScaleHolder mScaleHolder;
  private final Canvas mCanvas;

  private @Nullable Bitmap mPreviousBitmap;

  public MovieDrawer(Movie movie) {
    mMovie = movie;
    mScaleHolder = new MovieScaleHolder(movie.width(), movie.height());
    mCanvas = new Canvas();
  }

  public synchronized void drawFrame(int movieTime, int w, int h, Bitmap bitmap) {
    mMovie.setTime(movieTime);

    if (mPreviousBitmap != null && mPreviousBitmap.isRecycled()) {
      mPreviousBitmap = null;
    }
    if (mPreviousBitmap != bitmap) {
      mPreviousBitmap = bitmap;
      mCanvas.setBitmap(bitmap);
    }

    mScaleHolder.updateViewPort(w, h);

    mCanvas.save();
    mCanvas.scale(mScaleHolder.getScale(), mScaleHolder.getScale());
    mMovie.draw(mCanvas, mScaleHolder.getLeft(), mScaleHolder.getTop());
    mCanvas.restore();
  }
}
