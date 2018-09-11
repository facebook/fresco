/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite;

import android.graphics.Movie;
import android.support.annotation.Nullable;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;

/** Simple wrapper for an animated image backed by {@link Movie}. */
class MovieAnimatedImage implements AnimatedImage {

  private final MovieFrame[] mFrames;
  private final int mSizeInBytes;
  private final int mDuration;
  private @Nullable int[] mFrameDurations;

  public MovieAnimatedImage(MovieFrame[] frames, int sizeInBytes, int duration) {
    mFrames = frames;
    mSizeInBytes = sizeInBytes;
    mDuration = duration;
  }

  @Override
  public void dispose() {}

  @Override
  public int getWidth() {
    return mFrames[0].getWidth();
  }

  @Override
  public int getHeight() {
    return mFrames[0].getHeight();
  }

  @Override
  public int getFrameCount() {
    return mFrames.length;
  }

  @Override
  public int getDuration() {
    return mDuration;
  }

  @Override
  public int[] getFrameDurations() {
    if (mFrameDurations == null) {
      mFrameDurations = new int[mFrames.length];
      for (int i = 0, N = mFrames.length; i < N; i++) {
        mFrameDurations[i] = mFrames[i].getDurationMs();
      }
    }
    return mFrameDurations;
  }

  /**
   * Does not support variable loop count gif images. Will loop forever
   *
   * @return {@link LOOP_COUNT_INFINITE}
   */
  @Override
  public int getLoopCount() {
    return LOOP_COUNT_INFINITE;
  }

  @Override
  public AnimatedImageFrame getFrame(int frameNumber) {
    return mFrames[frameNumber];
  }

  @Override
  public boolean doesRenderSupportScaling() {
    return true;
  }

  @Override
  public int getSizeInBytes() {
    return mSizeInBytes;
  }

  @Override
  public AnimatedDrawableFrameInfo getFrameInfo(int frameNumber) {
    return new AnimatedDrawableFrameInfo(
        frameNumber,
        mFrames[frameNumber].getXOffset(),
        mFrames[frameNumber].getYOffset(),
        mFrames[frameNumber].getWidth(),
        mFrames[frameNumber].getHeight(),
        AnimatedDrawableFrameInfo.BlendOperation.NO_BLEND,
        AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT);
  }
}
