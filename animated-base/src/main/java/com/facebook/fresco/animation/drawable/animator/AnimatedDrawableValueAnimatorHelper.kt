/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable.animator

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.drawable.Drawable
import com.facebook.fresco.animation.drawable.AnimatedDrawable2
import com.facebook.fresco.animation.drawable.animator.AnimatedDrawable2ValueAnimatorHelper.createValueAnimator

/**
 * Helper class to create [ValueAnimator]s for animated drawables. Currently, this class only
 * supports API 11 (Honeycomb) and above.
 *
 * Supported drawable types: - [AnimatedDrawable2]
 */
object AnimatedDrawableValueAnimatorHelper {

  /**
   * Create a value animator for the given animation drawable and max animation duration in ms.
   *
   * @param drawable the drawable to create the animator for
   * @param maxDurationMs the max duration in ms
   * @return the animator to use
   */
  @JvmStatic
  fun createValueAnimator(drawable: Drawable?, maxDurationMs: Int): ValueAnimator? =
      if (drawable is AnimatedDrawable2) {
        AnimatedDrawable2ValueAnimatorHelper.createValueAnimator(
            checkNotNull((drawable as AnimatedDrawable2?)), maxDurationMs)
      } else {
        null
      }

  /**
   * Create a value animator for the given animation drawable.
   *
   * @param drawable the drawable to create the animator for
   * @return the animator to use
   */
  @JvmStatic
  fun createValueAnimator(drawable: Drawable?): ValueAnimator? {
    if (drawable is AnimatedDrawable2) {
      val animatedDrawable2 = drawable
      return createValueAnimator(
          animatedDrawable2, animatedDrawable2.loopCount, animatedDrawable2.loopDurationMs)
    }
    return null
  }

  /**
   * Create an animator update listener to be used to update the drawable to be animated.
   *
   * @param drawable the drawable to create the animator update listener for
   * @return the listener to use
   */
  @JvmStatic
  fun createAnimatorUpdateListener(drawable: Drawable?): AnimatorUpdateListener? =
      if (drawable is AnimatedDrawable2) {
        AnimatedDrawable2ValueAnimatorHelper.createAnimatorUpdateListener(
            checkNotNull((drawable as AnimatedDrawable2?)))
      } else {
        null
      }
}
