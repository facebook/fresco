/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.frame;

/**
 * Frame scheduler used to calculate which frame to display for given animation times.
 */
public interface FrameScheduler {

  int FRAME_NUMBER_DONE = -1;

  int NO_NEXT_TARGET_RENDER_TIME = -1;

  /**
   * Get the frame number for the given animation time or {@link #FRAME_NUMBER_DONE}
   * if the animation is over.
   *
   * @param animationTimeMs the animation time to get the frame number for
   * @param lastFrameTimeMs the time of the last draw before
   * @return the frame number to render or {@link #FRAME_NUMBER_DONE}
   */
  int getFrameNumberToRender(long animationTimeMs, long lastFrameTimeMs);

  /**
   * Get the loop duration of 1 full loop.
   *
   * @return the loop duration in ms
   */
  long getLoopDurationMs();

  /**
   * Get the target render time for the given frame number in ms.
   *
   * @param frameNumber the frame number to use
   * @return the target render time
   */
  long getTargetRenderTimeMs(int frameNumber);

  /**
   * For a given animation time, calculate the target render time for the next frame in ms.
   * If the animation is over, this will return {@link #NO_NEXT_TARGET_RENDER_TIME}
   *
   * @param animationTimeMs the current animation time in ms
   * @return the target animation time in ms for the next frame after the given animation time or
   *         {@link #NO_NEXT_TARGET_RENDER_TIME} if the animation is over
   */
  long getTargetRenderTimeForNextFrameMs(long animationTimeMs);

  /**
   * @return true if the animation is infinite
   */
  boolean isInfiniteAnimation();
}
