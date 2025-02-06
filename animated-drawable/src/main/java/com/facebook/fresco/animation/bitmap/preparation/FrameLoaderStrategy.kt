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
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.AnimationCoordinator
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.DynamicRenderingFps
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameLoader
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameLoaderFactory
import java.util.concurrent.TimeUnit

/** Use a [FrameLoader] strategy to render the animaion */
class FrameLoaderStrategy(
    source: String?,
    private val animationInformation: AnimationInformation,
    private val bitmapFrameRenderer: BitmapFrameRenderer,
    private val frameLoaderFactory: FrameLoaderFactory,
    private val downscaleFrameToDrawableDimensions: Boolean,
) : BitmapFramePreparationStrategy {

  private val cacheKey = source ?: this.hashCode().toString()
  private val animationWidth: Int = animationInformation.width()
  private val animationHeight: Int = animationInformation.height()
  private var frameLoader: FrameLoader? = null
    get() {
      if (field == null) {
        field =
            frameLoaderFactory.createBufferLoader(
                cacheKey, bitmapFrameRenderer, animationInformation)
      }
      return field
    }

  private val maxAnimationFps = animationInformation.fps()
  private var currentFps = maxAnimationFps

  private val dynamicFpsRender =
      object : DynamicRenderingFps {
        override val animationFps: Int = maxAnimationFps

        override val renderingFps: Int
          get() = currentFps

        override fun setRenderingFps(renderingFps: Int) {
          if (renderingFps != currentFps) {
            currentFps = renderingFps.coerceIn(1, maxAnimationFps)
            frameLoader?.compressToFps(currentFps)
          }
        }
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
    frameLoader?.prepareFrames(frameSize.width, frameSize.height, onAnimationLoaded ?: {})
  }

  @UiThread
  override fun getBitmapFrame(
      frameNumber: Int,
      canvasWidth: Int,
      canvasHeight: Int
  ): CloseableReference<Bitmap>? {
    val frameSize = calculateFrameSize(canvasWidth, canvasHeight)
    val frame = frameLoader?.getFrame(frameNumber, frameSize.width, frameSize.height)
    frame?.let { AnimationCoordinator.onRenderFrame(dynamicFpsRender, it) }
    return frame?.bitmapRef
  }

  override fun onStop() {
    frameLoader?.onStop()
    clearFrames()
  }

  override fun clearFrames() {
    frameLoader?.let { FrameLoaderFactory.saveUnusedFrame(cacheKey, it) }
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

  private fun AnimationInformation.fps(): Int =
      TimeUnit.SECONDS.toMillis(1).div(loopDurationMs.div(frameCount)).coerceAtLeast(1).toInt()
}

private class FrameSize(val width: Int, val height: Int)
