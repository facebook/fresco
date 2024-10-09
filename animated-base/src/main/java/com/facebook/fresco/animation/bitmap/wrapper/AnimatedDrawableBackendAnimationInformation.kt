/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.wrapper

import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend

/** [AnimationInformation] that wraps an [AnimatedDrawableBackend]. */
class AnimatedDrawableBackendAnimationInformation(
    private val animatedDrawableBackend: AnimatedDrawableBackend
) : AnimationInformation {

  override fun getFrameCount(): Int = animatedDrawableBackend.frameCount

  override fun getFrameDurationMs(frameNumber: Int): Int =
      animatedDrawableBackend.getDurationMsForFrame(frameNumber)

  override fun getLoopCount(): Int = animatedDrawableBackend.loopCount

  override fun getLoopDurationMs(): Int = animatedDrawableBackend.durationMs

  override fun width(): Int = animatedDrawableBackend.width

  override fun height(): Int = animatedDrawableBackend.height
}
