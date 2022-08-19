/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

import android.graphics.drawable.Drawable

/**
 * Base animation listener. This convenience class can be used to simplify the code if the extending
 * class is not interested in all events. Just override the ones you need.
 *
 * See [AnimationListener] for more information.
 */
open class BaseAnimationListener : AnimationListener {
  override fun onAnimationStart(drawable: Drawable) = Unit

  override fun onAnimationStop(drawable: Drawable) = Unit

  override fun onAnimationReset(drawable: Drawable) = Unit

  override fun onAnimationRepeat(drawable: Drawable) = Unit

  override fun onAnimationFrame(drawable: Drawable, frameNumber: Int) = Unit
}
