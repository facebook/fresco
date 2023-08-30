/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import android.graphics.Bitmap
import androidx.annotation.UiThread
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameLoader
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameLoaderFactory

/** Use a [FrameLoader] strategy to render the animaion */
class FrameLoaderStrategy(
    private val animationInformation: AnimationInformation,
    private val frameLoaderFactory: FrameLoaderFactory,
    private val downscaleFrameToDrawableDimensions: Boolean,
) : BitmapFramePreparationStrategy {

  private val animationWidth: Int = animationInformation.width()
  private val animationHeight: Int = animationInformation.height()
  private var frameLoader: FrameLoader? = null
    get() {
      if (field == null) {
        field = frameLoaderFactory.createBufferLoader(animationInformation)
      }
      return field
    }

  @UiThread
  override fun prepareFrames(
      canvasWidth: Int,
      canvasHeight: Int,
      onAnimationLoaded: (() -> Unit)?
  ) {
    // Validate inputs
    if (canvasWidth <= 0 || canvasHeight <= 0 || animationWidth <= 0 || animationHeight <= 0) {
      return
    }

    val frameSize = calculateFrameSize(canvasWidth, canvasHeight)
    frameLoader?.prepareFrames(frameSize.width, frameSize.width, onAnimationLoaded ?: {})
  }

  @UiThread
  override fun getBitmapFrame(
      frameNumber: Int,
      canvasWidth: Int,
      canvasHeight: Int
  ): CloseableReference<Bitmap>? {
    val frameSize = calculateFrameSize(canvasWidth, canvasHeight)
    return frameLoader?.getFrame(frameNumber, frameSize.width, frameSize.height)
  }

  override fun onStop() {
    frameLoader?.onStop()
  }

  override fun clearFrames(): Unit = clear()

  private fun clear() {
    frameLoader?.clear()
    frameLoader = null
  }

  private fun calculateFrameSize(canvasWidth: Int, canvasHeight: Int): FrameSize {
    if (!downscaleFrameToDrawableDimensions) {
      return FrameSize(animationWidth, animationHeight)
    }

    var bitmapWidth: Int = animationWidth
    var bitmapHeight: Int = animationHeight

    // The maximum size for the bitmap is the size of the animation if the canvas is bigger
    if (canvasWidth < animationWidth || canvasHeight < animationHeight) {
      val ratioW = animationWidth.toDouble().div(animationHeight)
      if (canvasHeight > canvasWidth) {
        bitmapHeight = canvasHeight.coerceAtMost(animationHeight)
        bitmapWidth = bitmapHeight.times(ratioW).toInt()
      } else {
        bitmapWidth = canvasWidth.coerceAtMost(animationWidth)
        bitmapHeight = bitmapWidth.div(ratioW).toInt()
      }
    }

    return FrameSize(bitmapWidth, bitmapHeight)
  }
}

private class FrameSize(val width: Int, val height: Int)
