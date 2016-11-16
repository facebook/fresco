/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.frame;

/**
 * Frame scheduler used to calculate which frame to display for given animation times.
 */
public interface FrameScheduler {

  int FRAME_NUMBER_DONE = -1;

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
   * For a given animation time, calculate the delay until the next frame should be rendered.
   * This can also return 0 if the next frame should be rendered right now.
   *
   * @param animationTimeMs the current animation time in ms
   * @return the delay in ms until the next frame is due
   */
  long getDelayUntilNextFrameMs(long animationTimeMs);

  /**
   * @return true if the animation is infinite
   */
  boolean isInfiniteAnimation();
}
