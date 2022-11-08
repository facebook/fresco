/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The load strategy handles the logic to extract the frames for the animation.
 *
 * This strategy is based in two worker threads:
 * - 1) firstFrameExecutor. This worker will extract only the first frame for the animation. Then we
 * can draw at least the first frame from the very beginning. This thread has to complete the
 * extraction very fast
 * - 2) animationExtractor. This worker will load all animation frames
 */
class DefaultLoadAnimationStrategy(
    private val firstFrameExecutor: ExecutorService,
    private val animationExtractorExecutor: ExecutorService,
    private val bitmapFrameRenderer: BitmapFrameRenderer,
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val bitmapCache: BitmapFrameCache,
) : BitmapFramePreparationStrategy {

  private val forceStopSignal = AtomicBoolean(false)
  private val fetchingFrames = AtomicBoolean(false)
  private val bitmapConfig = Bitmap.Config.ARGB_8888

  override fun onStop() {
    forceStopSignal.set(true)
  }

  override fun prepareFrames(
      frameCount: Int,
      canvasWidth: Int,
      canvasHeight: Int,
      intrinsicWidth: Int,
      intrinsicHeight: Int,
  ) {
    if (fetchingFrames.getAndSet(true) || forceStopSignal.get()) {
      return
    }

    if (bitmapCache.getCachedFrame(0) == null) {
      firstFrameExecutor.execute {
        val frameCollection = mutableListOf<CloseableReference<Bitmap>>()

        val canvasBitmapFrame =
            generateBaseFrame(canvasWidth, canvasHeight, intrinsicWidth, intrinsicHeight)
        renderAndSaveFrame(0, frameCollection, canvasBitmapFrame)

        bitmapCache.onAnimationPrepared(frameCollection)
        CloseableReference.closeSafely(canvasBitmapFrame)
      }
    }

    animationExtractorExecutor.execute {
      val frameCollection = mutableListOf<CloseableReference<Bitmap>>()
      var canvasBitmapFrame =
          generateBaseFrame(canvasWidth, canvasHeight, intrinsicWidth, intrinsicHeight)
      // Animation frames have to be render incrementally
      (0 until frameCount).forEach { frameNumber ->
        if (forceStopSignal.get()) {
          onPreparationFinished()
          return@execute
        }

        val currentRenderedFrame =
            renderAndSaveFrame(frameNumber, frameCollection, canvasBitmapFrame)
        canvasBitmapFrame = currentRenderedFrame ?: return@execute
      }

      bitmapCache.onAnimationPrepared(frameCollection)
      CloseableReference.closeSafely(canvasBitmapFrame)
      onPreparationFinished()
    }
  }

  private fun onPreparationFinished() {
    forceStopSignal.set(false)
    fetchingFrames.set(false)
  }

  private fun generateBaseFrame(
      canvasWidth: Int,
      canvasHeight: Int,
      intrinsicWidth: Int,
      intrinsicHeight: Int,
  ): CloseableReference<Bitmap> {
    // The maximum size for the bitmap is the size of the animation if the canvas is bigger
    return if (canvasWidth < intrinsicWidth && canvasHeight < intrinsicHeight) {
      platformBitmapFactory.createBitmap(canvasWidth, canvasHeight, bitmapConfig)
    } else {
      platformBitmapFactory.createBitmap(intrinsicWidth, intrinsicHeight, bitmapConfig)
    }
  }

  /** @return current rendered frame */
  private fun renderAndSaveFrame(
      frameNumber: Int,
      frameCollection: MutableList<CloseableReference<Bitmap>>,
      canvasBitmapRef: CloseableReference<Bitmap>
  ): CloseableReference<Bitmap>? {
    val frameRendered = bitmapFrameRenderer.renderFrame(frameNumber, canvasBitmapRef.get())
    if (!frameRendered) {
      // If we couldnt render the frame, then we create a new empty bitmap
      CloseableReference.closeSafely(canvasBitmapRef)
      return null
    }

    // Save rendered bitmap
    val copyFrame = platformBitmapFactory.createBitmap(canvasBitmapRef.get())
    frameCollection.add(copyFrame)

    return canvasBitmapRef
  }

  override fun findNearestFrame(fromFrame: Int, frameCount: Int): CloseableReference<Bitmap>? {
    return (fromFrame downTo 0).asSequence().firstNotNullOfOrNull {
      val frame = bitmapCache.getCachedFrame(it)
      if (frame?.isValid == true) frame else null
    }
  }

  override fun clearFrames() {
    animationExtractorExecutor.execute { bitmapCache.clear() }
  }
}
