/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.frame;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.fresco.animation.backend.AnimationBackend;

/**
 * Frame scheduler that maps time values to frames.
 */
public class DropFramesFrameScheduler implements FrameScheduler {

  private static final int UNSET = -1;

  private final AnimationBackend mAnimationBackend;

  private long mLoopDurationMs = UNSET;

  public DropFramesFrameScheduler(AnimationBackend animationBackend) {
    mAnimationBackend = animationBackend;
  }

  @Override
  public int getFrameNumberToRender(long animationTimeMs, long lastFrameTimeMs) {
    if (!isInfiniteAnimation()) {
      long loopCount = animationTimeMs / getLoopDurationMs();
      if (loopCount > mAnimationBackend.getLoopCount()) {
        return FRAME_NUMBER_DONE;
      }
    }
    long timeInCurrentLoopMs = animationTimeMs % getLoopDurationMs();
    return getFrameNumberWithinLoop(timeInCurrentLoopMs);
  }

  @Override
  public long getLoopDurationMs() {
    if (mLoopDurationMs != UNSET) {
      return mLoopDurationMs;
    }
    mLoopDurationMs = 0;
    int frameCount = mAnimationBackend.getFrameCount();
    for (int i = 0; i < frameCount; i++) {
      mLoopDurationMs += mAnimationBackend.getFrameDurationMs(i);
    }
    return mLoopDurationMs;
  }

  @Override
  public long getTargetRenderTimeMs(int frameNumber) {
    long targetRenderTimeMs = 0;
    for (int i = 0; i < frameNumber; i++) {
      targetRenderTimeMs += mAnimationBackend.getFrameDurationMs(frameNumber);
    }
    return targetRenderTimeMs;
  }

  @Override
  public long getDelayUntilNextFrameMs(long animationTimeMs) {
    long loopDurationMs = getLoopDurationMs();
    while (animationTimeMs > loopDurationMs) {
      animationTimeMs -= loopDurationMs;
    }
    int currentFrame = 0;
    while (animationTimeMs > 0) {
      animationTimeMs -= mAnimationBackend.getFrameDurationMs(currentFrame++);
    }
    return -animationTimeMs;
  }

  @Override
  public boolean isInfiniteAnimation() {
    return mAnimationBackend.getLoopCount() == AnimationBackend.LOOP_COUNT_INFINITE;
  }

  @VisibleForTesting
  int getFrameNumberWithinLoop(long timeInCurrentLoopMs) {
    int frame = 0;
    long currentDuration = 0;
    do {
      currentDuration += mAnimationBackend.getFrameDurationMs(frame);
      frame++;
    } while (timeInCurrentLoopMs >= currentDuration);
    return frame - 1;
  }
}
