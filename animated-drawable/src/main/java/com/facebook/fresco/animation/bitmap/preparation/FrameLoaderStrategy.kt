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

  private val hasReusableCacheKey = source != null
  private val cacheKey = source ?: this.hashCode().toString()
  private val animationWidth: Int = animationInformation.width()
  private val animationHeight: Int = animationInformation.height()
  private var frameLoader: FrameLoader? = null
    get() {
      if (field == null) {
        field =
            frameLoaderFactory.createBufferLoader(
                cacheKey,
                bitmapFrameRenderer,
                animationInformation,
            )
      }
      return field
    }

  private val maxAnimationFps = animationInformation.fps()
  private var currentFps = maxAnimationFps
  private var isRunning = true

  private val dynamicFpsRender =
      object : DynamicRenderingFps {
        override val animationFps: Int = maxAnimationFps

        override val renderingFps: Int
          get() = currentFps

        override fun setRenderingFps(renderingFps: Int) {
          if (renderingFps != currentFps && isRunning) {
            currentFps = renderingFps.coerceIn(1, maxAnimationFps)
            frameLoader?.compressToFps(currentFps)
          }
        }
      }

  @UiThread
  override fun prepareFrames(
      canvasWidth: Int,
      canvasHeight: Int,
      onAnimationLoaded: (() -> Unit)?,
  ) {
    // Validate inputs
    if (canvasWidth <= 0 || canvasHeight <= 0 || animationWidth <= 0 || animationHeight <= 0) {
      return
    }
    isRunning = true

    val frameSize = calculateFrameSize(canvasWidth, canvasHeight)
    frameLoader?.prepareFrames(frameSize.width, frameSize.height, onAnimationLoaded ?: {})
  }

  @UiThread
  override fun getBitmapFrame(
      frameNumber: Int,
      canvasWidth: Int,
      canvasHeight: Int,
  ): CloseableReference<Bitmap>? {
    if (canvasWidth <= 0 || canvasHeight <= 0 || animationWidth <= 0 || animationHeight <= 0) {
      return null
    }
    val frameSize = calculateFrameSize(canvasWidth, canvasHeight)
    val frame = frameLoader?.getFrame(frameNumber, frameSize.width, frameSize.height)
    frame?.let { AnimationCoordinator.onRenderFrame(dynamicFpsRender, it) }
    isRunning = true
    return frame?.bitmapRef
  }

  override fun onStop() {
    frameLoader?.onStop()
    clearFrames()
  }

  override fun clearFrames() {
    frameLoader?.let {
      if (hasReusableCacheKey) {
        FrameLoaderFactory.saveUnusedFrame(
            cacheKey,
            it,
            frameLoaderFactory.enableUnusedFrameLoaderCleanupSync,
            frameLoaderFactory.enableUnusedFrameLoaderCleanupSyncAndClear,
        )
      } else {
        it.clear()
      }
    }
    frameLoader = null
    isRunning = false
  }

  private fun calculateFrameSize(canvasWidth: Int, canvasHeight: Int): FrameSize {
    if (!downscaleFrameToDrawableDimensions) {
      return FrameSize(animationWidth, animationHeight)
    }

    val scale = minOf(
        1.0,
        canvasWidth.toDouble().div(animationWidth),
        canvasHeight.toDouble().div(animationHeight),
    )
    return FrameSize(
        animationWidth.times(scale).toInt().coerceAtLeast(1),
        animationHeight.times(scale).toInt().coerceAtLeast(1),
    )
  }

  private fun AnimationInformation.fps(): Int =
      TimeUnit.SECONDS.toMillis(1).div(loopDurationMs.div(frameCount)).coerceAtLeast(1).toInt()
}

private class FrameSize(val width: Int, val height: Int)
