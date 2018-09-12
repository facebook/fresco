/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.draw;

class MovieScaleHolder {

  private final int mMovieWidth;
  private final int mMovieHeight;
  private int mViewPortWidth;
  private int mViewPortHeight;
  private float mScale = 1f;
  private float mLeft = 0;
  private float mTop = 0;

  public MovieScaleHolder(int movieWidth, int movieHeight) {
    mMovieWidth = movieWidth;
    mMovieHeight = movieHeight;
  }

  public synchronized float getScale() {
    return mScale;
  }

  public synchronized float getLeft() {
    return mLeft;
  }

  public synchronized float getTop() {
    return mTop;
  }

  public synchronized void updateViewPort(int viewPortWidth, int viewPortHeight) {
    if (mViewPortWidth == viewPortWidth && mViewPortHeight == viewPortHeight) {
      return;
    }
    mViewPortWidth = viewPortWidth;
    mViewPortHeight = viewPortHeight;
    determineScaleAndPosition();
  }

  private synchronized void determineScaleAndPosition() {

    float inputRatio = mMovieWidth / mMovieHeight;
    float outputRatio = mViewPortWidth / mViewPortHeight;

    int width = mViewPortWidth;
    int height = mViewPortHeight;
    if (outputRatio > inputRatio) {
      // Not enough width to fill the output. (Black bars on left and right.)
      width = (int) (mViewPortHeight * inputRatio);
    } else if (outputRatio < inputRatio) {
      // Not enough height to fill the output. (Black bars on top and bottom.)
      height = (int) (mViewPortWidth / inputRatio);
    }

    if (mViewPortWidth > mMovieWidth) {
      mScale = mMovieWidth / (float) mViewPortWidth;
    } else if (mMovieWidth > mViewPortWidth) {
      mScale = mViewPortWidth / (float) mMovieWidth;
    } else {
      mScale = 1f;
    }

    mLeft = ((mViewPortWidth - width) / 2f) / mScale;
    mTop = ((mViewPortHeight - height) / 2f) / mScale;
  }
}
