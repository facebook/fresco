/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.drawable;

import com.facebook.common.logging.FLog;
import com.facebook.fresco.animation.frame.FrameScheduler;

/**
 * {@link com.facebook.fresco.animation.drawable.AnimatedDrawable2.DrawListener} for debugging
 * {@link AnimatedDrawable2}.
 */
public class AnimatedDrawable2DebugDrawListener implements AnimatedDrawable2.DrawListener {

  private static final Class<?> TAG = AnimatedDrawable2DebugDrawListener.class;

  private int mLastFrameNumber = -1;
  private int mSkippedFrames;
  private int mDuplicateFrames;
  private int mDrawCalls;

  @Override
  public void onDraw(
      AnimatedDrawable2 animatedDrawable,
      FrameScheduler frameScheduler,
      int frameNumberToDraw,
      boolean frameDrawn,
      boolean isAnimationRunning,
      long animationStartTimeMs,
      long animationTimeMs,
      long lastFrameAnimationTimeMs,
      long actualRenderTimeStartMs,
      long actualRenderTimeEndMs,
      long startRenderTimeForNextFrameMs,
      long scheduledRenderTimeForNextFrameMs) {
    if (animatedDrawable.getAnimationBackend() == null) {
      return;
    }
    int frameCount = animatedDrawable.getAnimationBackend().getFrameCount();

    long animationTimeDifference = animationTimeMs - lastFrameAnimationTimeMs;
    mDrawCalls++;
    int expectedNextFrameNumber = (mLastFrameNumber + 1) % frameCount;
    if (expectedNextFrameNumber != frameNumberToDraw) {
      // something went wrong...
      if (mLastFrameNumber == frameNumberToDraw) {
        mDuplicateFrames++;
      } else {
        int skippedFrameCount =
            (frameNumberToDraw - expectedNextFrameNumber) % frameCount;
        if (skippedFrameCount < 0) {
          skippedFrameCount += frameCount;
        }
        mSkippedFrames += skippedFrameCount;
      }
    }
    mLastFrameNumber = frameNumberToDraw;
    FLog.d(
        TAG,
        "draw: frame: %2d, drawn: %b, delay: %3d ms, rendering: %3d ms, prev: %3d ms ago, duplicates: %3d, skipped: %3d, draw calls: %4d, anim time: %6d ms, next start: %6d ms, next scheduled: %6d ms",
        frameNumberToDraw,
        frameDrawn,
        animationTimeMs % frameScheduler.getLoopDurationMs() -
            frameScheduler.getTargetRenderTimeMs(frameNumberToDraw),
        actualRenderTimeEndMs - actualRenderTimeStartMs,
        animationTimeDifference,
        mDuplicateFrames,
        mSkippedFrames,
        mDrawCalls,
        animationTimeMs,
        startRenderTimeForNextFrameMs,
        scheduledRenderTimeForNextFrameMs);
  }
}
