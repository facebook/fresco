/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.frame

/** Frame scheduler used to calculate which frame to display for given animation times. */
interface FrameScheduler {

  /**
   * Get the frame number for the given animation time or [FRAME_NUMBER_DONE] if the animation is
   * over.
   *
   * @param animationTimeMs the animation time to get the frame number for
   * @param lastFrameTimeMs the time of the last draw before
   * @return the frame number to render or [FRAME_NUMBER_DONE]
   */
  fun getFrameNumberToRender(animationTimeMs: Long, lastFrameTimeMs: Long): Int

  /**
   * Get the loop duration of 1 full loop.
   *
   * @return the loop duration in ms
   */
  val loopDurationMs: Long

  /**
   * Get the target render time for the given frame number in ms.
   *
   * @param frameNumber the frame number to use
   * @return the target render time
   */
  fun getTargetRenderTimeMs(frameNumber: Int): Long

  /**
   * For a given animation time, calculate the target render time for the next frame in ms. If the
   * animation is over, this will return [NO_NEXT_TARGET_RENDER_TIME]
   *
   * @param animationTimeMs the current animation time in ms
   * @return the target animation time in ms for the next frame after the given animation time or
   *   [NO_NEXT_TARGET_RENDER_TIME] if the animation is over
   */
  fun getTargetRenderTimeForNextFrameMs(animationTimeMs: Long): Long

  /** @return true if the animation is infinite */
  val isInfiniteAnimation: Boolean

  companion object {
    const val FRAME_NUMBER_DONE: Int = -1

    const val NO_NEXT_TARGET_RENDER_TIME: Int = -1
  }
}
