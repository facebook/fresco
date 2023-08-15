/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

import android.graphics.Bitmap
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.fresco.animation.bitmap.preparation.loadframe.AnimationLoaderExecutor
import com.facebook.fresco.animation.bitmap.preparation.loadframe.LoadFramePriorityTask
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This frame loader uses a fixed number of bitmap. The buffer loads the next bunch of frames when
 * the animation render an specific threshold frame
 */
class BufferFrameLoader(
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val bitmapFrameRenderer: BitmapFrameRenderer,
    override val animationInformation: AnimationInformation
) : FrameLoader {

  private val bufferSize = animationInformation.fps()
  private val bufferFramesHash = ConcurrentHashMap<Int, CloseableReference<Bitmap>>()
  private var bufferFramesSequence: List<Int> = emptyList()

  private val isFetching = AtomicBoolean(false)

  private val frameSequence = CircularList(animationInformation.frameCount)
  private var lastRenderedFrameNumber: Int = -1

  @UiThread
  override fun getFrame(frameNumber: Int, width: Int, height: Int): CloseableReference<Bitmap>? {
    lastRenderedFrameNumber = frameNumber

    val cachedFrame = bufferFramesHash[frameNumber]
    if (cachedFrame?.isValid == true) {

      if (frameSequence.isTargetAhead(
          from = getThreshold(), target = frameNumber, lenght = bufferSize)) {
        loadNextFrameIfNeeded(frameNumber, width, height)
      }

      return cachedFrame
    }

    loadNextFrameIfNeeded(frameNumber, width, height)
    return nearestFrame(frameNumber)?.bitmap
  }

  private fun getThreshold() =
      if (bufferFramesSequence.isEmpty()) bufferSize.times(THRESHOLD_PERCENTAGE).toInt()
      else {
        val middlePoint = bufferFramesSequence.size.times(THRESHOLD_PERCENTAGE).toInt()
        bufferFramesSequence[middlePoint]
      }

  @UiThread
  override fun prepareFrames(width: Int, height: Int, onAnimationLoaded: () -> Unit) {
    loadNextFrameIfNeeded(0, width, height)
    onAnimationLoaded()
  }

  /** Left only the last rendered bitmap on the buffer */
  override fun onStop() {
    val nearestFrame = nearestFrame(lastRenderedFrameNumber)

    val indexesToClose = bufferFramesHash.keys.minus(nearestFrame?.frameNumber)
    indexesToClose.forEach {
      bufferFramesHash[it]?.close()
      bufferFramesHash.remove(it)
    }
  }

  /** Release all bitmaps */
  override fun clear() {
    bufferFramesHash.values.forEach { it.close() }
    bufferFramesHash.clear()
    lastRenderedFrameNumber = -1
  }

  private fun loadNextFrameIfNeeded(targetFrame: Int, width: Int, height: Int) {
    if (isFetching.getAndSet(true)) {
      return
    }

    AnimationLoaderExecutor.execute(
        object : LoadFramePriorityTask {
          override val priority = LoadFramePriorityTask.Priority.HIGH

          override fun run() {
            extractDemandedFrame(targetFrame, width, height)
            isFetching.set(false)
          }
        })
  }

  @WorkerThread
  private fun extractDemandedFrame(targetFrame: Int, width: Int, height: Int) {
    val nextWindow = frameSequence.sublist(targetFrame, bufferSize)

    // Load new frames
    nextWindow.forEach { newFrameNumber ->
      if (bufferFramesHash[newFrameNumber] != null) {
        return@forEach
      }
      bufferFramesHash[newFrameNumber] = createNewFrame(newFrameNumber, width, height)
    }

    // Close old frames
    bufferFramesHash.keys.minus(nextWindow.toSet()).forEach {
      bufferFramesHash[it]?.close()
      bufferFramesHash.remove(it)
    }

    bufferFramesSequence = nextWindow
  }

  private fun createNewFrame(
      targetFrame: Int,
      width: Int,
      height: Int
  ): CloseableReference<Bitmap> {
    nearestFrame(targetFrame)?.let { closerFrame ->
      val copyBitmap = platformBitmapFactory.createBitmap(closerFrame.bitmap.get())
      updateBitmap(copyBitmap.get(), closerFrame.frameNumber, targetFrame)
      return copyBitmap
    }

    val bitmap = renderFirstBitmap(width, height)
    updateBitmap(bitmap.get(), 0, targetFrame)
    return bitmap
  }

  private fun nearestFrame(targetFrame: Int): AnimationBitmapFrame? {
    (0..frameSequence.size).forEach {
      val closestFrame = frameSequence.getPosition(targetFrame - it)
      val frame = bufferFramesHash[closestFrame] ?: return@forEach
      if (frame.isValid) {
        return AnimationBitmapFrame(closestFrame, frame)
      }
    }

    return null
  }

  private fun renderFirstBitmap(width: Int, height: Int): CloseableReference<Bitmap> {
    val base = platformBitmapFactory.createBitmap(width, height)
    bitmapFrameRenderer.renderFrame(0, base.get())
    return base
  }

  private fun updateBitmap(fromBitmap: Bitmap, from: Int, dest: Int) {
    if (from > dest) {
      (0..dest).forEach { bitmapFrameRenderer.renderFrame(it, fromBitmap) }
    } else if (from < dest) {
      (from + 1..dest).forEach { bitmapFrameRenderer.renderFrame(it, fromBitmap) }
    }
  }

  private fun AnimationInformation.fps(): Int =
      TimeUnit.SECONDS.toMillis(1).div(loopDurationMs.div(frameCount)).coerceAtLeast(1).toInt()

  companion object {

    /**
     * Used to calculate the threshold frame for triggering the next buffer load from the last
     * render frame
     */
    private const val THRESHOLD_PERCENTAGE = 0.5f
  }
}
