/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

import android.graphics.drawable.Drawable

/**
 * Animation listener that can be used to get notified about [AnimatedDrawable2] events. Call
 * [AnimatedDrawable2#setAnimationListener(AnimationListener)] to set a listener.
 */
interface AnimationListener {
  /**
   * Called when the animation is started for the given drawable.
   *
   * @param drawable the affected drawable
   */
  fun onAnimationStart(drawable: Drawable)

  /**
   * Called when the animation is stopped for the given drawable.
   *
   * @param drawable the affected drawable
   */
  fun onAnimationStop(drawable: Drawable)

  /**
   * Called when the animation is reset for the given drawable.
   *
   * @param drawable the affected drawable
   */
  fun onAnimationReset(drawable: Drawable)

  /**
   * Called when the animation is repeated for the given drawable. Animations have a loop count, and
   * frame count, so this is called when the frame count is 0 and the loop count is increased.
   *
   * @param drawable the affected drawable
   */
  fun onAnimationRepeat(drawable: Drawable)

  /**
   * Called when a frame of the animation is about to be rendered.
   *
   * @param drawable the affected drawable
   * @param frameNumber the frame number to be rendered
   */
  fun onAnimationFrame(drawable: Drawable, frameNumber: Int)

  /** Triggered when animation is loaded in memory and ready to play */
  fun onAnimationLoaded() = Unit
}
