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
import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.drawable.AnimatedDrawable2

/** Helper class to create [ValueAnimator]s for [AnimatedDrawable2]. */
object AnimatedDrawable2ValueAnimatorHelper {

  @JvmStatic
  fun createValueAnimator(animatedDrawable: AnimatedDrawable2, maxDurationMs: Int): ValueAnimator? {
    val animator =
        createValueAnimator(
            animatedDrawable, animatedDrawable.loopCount, animatedDrawable.loopDurationMs)
            ?: return null
    val repeatCount = Math.max(maxDurationMs / animatedDrawable.loopDurationMs, 1).toInt()
    animator.repeatCount = repeatCount
    return animator
  }

  @JvmStatic
  fun createValueAnimator(
      animatedDrawable: Drawable,
      loopCount: Int,
      loopDurationMs: Long
  ): ValueAnimator {
    val animator = ValueAnimator()
    animator.setIntValues(0, loopDurationMs.toInt())
    animator.duration = loopDurationMs
    animator.repeatCount =
        if (loopCount != AnimationInformation.LOOP_COUNT_INFINITE) loopCount
        else ValueAnimator.INFINITE
    animator.repeatMode = ValueAnimator.RESTART
    // Use a linear interpolator
    animator.interpolator = null
    val animatorUpdateListener = createAnimatorUpdateListener(animatedDrawable)
    animator.addUpdateListener(animatorUpdateListener)
    return animator
  }

  @JvmStatic
  fun createAnimatorUpdateListener(drawable: Drawable): AnimatorUpdateListener =
      AnimatorUpdateListener { animation: ValueAnimator ->
        drawable.level = (animation.animatedValue as Int)
      }
}
