/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapFrameCache

/** Frame preparation strategy to prepare next animation frames. */
interface BitmapFramePreparationStrategy {
  /**
   * Decide whether frames should be prepared ahead of time when a frame is drawn.
   *
   * @param bitmapFramePreparer the preparer to be used to create frames
   * @param bitmapFrameCache the cache to pass to the preparer
   * @param animationBackend the animation backend to prepare frames for
   * @param lastDrawnFrameNumber the last drawn frame number
   */
  fun prepareFrames(
      bitmapFramePreparer: BitmapFramePreparer,
      bitmapFrameCache: BitmapFrameCache,
      animationBackend: AnimationBackend,
      lastDrawnFrameNumber: Int,
      onAnimationLoaded: (() -> Unit)? = null
  ) = Unit

  /**
   * Prepare the frames for the animation giving the size of the animation and the size of the
   * canvas
   *
   * @param onAnimationLoaded This callback is invoked every time that bitmaps are loaded in memory
   *   successfully. This can be triggered several times because animation could be dropped from
   *   memory and then loaded again
   */
  fun prepareFrames(canvasWidth: Int, canvasHeight: Int, onAnimationLoaded: (() -> Unit)?) = Unit

  /** Force stop any task running on the strategy */
  fun onStop() = Unit

  /** Clear the frames from cache */
  fun clearFrames() = Unit

  fun getBitmapFrame(
      frameNumber: Int,
      canvasWidth: Int,
      canvasHeight: Int
  ): CloseableReference<Bitmap>? = null
}
