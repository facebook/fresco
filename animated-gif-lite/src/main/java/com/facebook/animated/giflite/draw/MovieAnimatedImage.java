/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.draw;

import android.graphics.Bitmap;
import android.graphics.Movie;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Simple wrapper for an animated image backed by {@link Movie}. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class MovieAnimatedImage implements AnimatedImage {

  private final MovieFrame[] mFrames;
  private final int mSizeInBytes;
  private final int mDuration;
  private final int mLoopCount;
  private final int[] mFrameDurations;
  @Nullable private final Bitmap.Config mAnimatedBitmapConfig;

  public MovieAnimatedImage(MovieFrame[] frames, int sizeInBytes, int duration, int loopCount) {
    this(frames, sizeInBytes, duration, loopCount, null);
  }

  public MovieAnimatedImage(
      MovieFrame[] frames,
      int sizeInBytes,
      int duration,
      int loopCount,
      @Nullable Bitmap.Config animatedBitmapConfig) {
    mFrames = frames;
    mSizeInBytes = sizeInBytes;
    mDuration = duration;
    mLoopCount = loopCount;
    mFrameDurations = new int[mFrames.length];
    for (int i = 0, N = mFrames.length; i < N; i++) {
      mFrameDurations[i] = mFrames[i].getDurationMs();
    }
    mAnimatedBitmapConfig = animatedBitmapConfig;
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
    return mFrameDurations;
  }

  @Override
  public int getLoopCount() {
    return mLoopCount;
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
    MovieFrame frame = mFrames[frameNumber];
    return new AnimatedDrawableFrameInfo(
        frameNumber,
        frame.getXOffset(),
        frame.getYOffset(),
        frame.getWidth(),
        frame.getHeight(),
        AnimatedDrawableFrameInfo.BlendOperation.BLEND_WITH_PREVIOUS,
        mFrames[frameNumber].getDisposalMode());
  }

  @Override
  @Nullable
  public Bitmap.Config getAnimatedBitmapConfig() {
    return mAnimatedBitmapConfig;
  }
}
