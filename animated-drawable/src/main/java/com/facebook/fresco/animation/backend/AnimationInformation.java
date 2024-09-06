/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.backend;

import com.facebook.infer.annotation.Nullsafe;

/** Basic animation metadata: Frame and loop count & duration */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface AnimationInformation {

  /**
   * Loop count to be returned by {@link #getLoopCount()} when the animation should be repeated
   * indefinitely.
   */
  int LOOP_COUNT_INFINITE = 0;

  /**
   * Get the number of frames for the animation
   *
   * @return the number of frames
   */
  int getFrameCount();

  /**
   * Get the frame duration for a given frame number in milliseconds.
   *
   * @param frameNumber the frame to get the duration for
   * @return the duration in ms
   */
  int getFrameDurationMs(int frameNumber);

  /**
   * Loop duration in ms
   *
   * @return duration in ms
   */
  int getLoopDurationMs();

  /**
   * @return Animation asset width
   */
  int width();

  /**
   * @return Animation asset height
   */
  int height();

  /**
   * Get the number of loops the animation has or {@link #LOOP_COUNT_INFINITE} for infinite looping.
   *
   * @return the loop count or {@link #LOOP_COUNT_INFINITE}
   */
  int getLoopCount();
}
