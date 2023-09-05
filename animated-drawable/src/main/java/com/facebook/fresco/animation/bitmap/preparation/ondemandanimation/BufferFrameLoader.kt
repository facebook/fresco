/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.fresco.animation.bitmap.preparation.loadframe.AnimationLoaderExecutor
import com.facebook.fresco.animation.bitmap.preparation.loadframe.FpsCompressorInfo
import com.facebook.fresco.animation.bitmap.preparation.loadframe.LoadFramePriorityTask
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import java.util.ArrayDeque
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
    private val fpsCompressor: FpsCompressorInfo,
    override val animationInformation: AnimationInformation
) : FrameLoader {

  private val bufferSize = animationInformation.fps() * BUFFER_SECOND_SIZE
  private val bufferFramesHash = ConcurrentHashMap<Int, CloseableReference<Bitmap>>()
  private var bufferFramesSequence: List<Int> = emptyList()

  private val isFetching = AtomicBoolean(false)

  private val frameSequence = CircularList(animationInformation.frameCount)
  private var lastRenderedFrameNumber: Int = -1
  private var compressionFrameMap: Map<Int, Int> = emptyMap()
  private var renderableFrameIndexes: Set<Int> = emptySet()

  init {
    compressToFps(animationInformation.fps())
  }

  @UiThread
  override fun getFrame(frameNumber: Int, width: Int, height: Int): FrameResult {
    val cachedFrameIndex =
        compressionFrameMap[frameNumber]
            ?: return findNearestFrame(frameNumber)?.bitmap.let {
              val bitmapRef = it?.cloneOrNull()
              FrameResult(
                  bitmapRef,
                  if (bitmapRef == null) FrameResult.FrameType.MISSING
                  else FrameResult.FrameType.NEAREST)
            }

    lastRenderedFrameNumber = cachedFrameIndex

    val cachedFrame = bufferFramesHash[cachedFrameIndex]?.cloneOrNull()
    if (cachedFrame != null) {

      if (frameSequence.isTargetAhead(
          from = getThreshold(), target = cachedFrameIndex, lenght = bufferSize)) {
        loadNextFrameIfNeeded(width, height)
      }
      return FrameResult(cachedFrame, FrameResult.FrameType.SUCCESS)
    }

    loadNextFrameIfNeeded(width, height)
    return findNearestFrame(cachedFrameIndex)?.bitmap.let {
      val bitmapRef = it?.cloneOrNull()
      FrameResult(
          bitmapRef,
          if (bitmapRef == null) FrameResult.FrameType.MISSING else FrameResult.FrameType.NEAREST)
    }
  }

  private fun getThreshold() =
      if (bufferFramesSequence.isEmpty()) bufferSize.times(THRESHOLD_PERCENTAGE).toInt()
      else {
        val middlePoint = bufferFramesSequence.size.times(THRESHOLD_PERCENTAGE).toInt()
        bufferFramesSequence[middlePoint]
      }

  @UiThread
  override fun prepareFrames(width: Int, height: Int, onAnimationLoaded: () -> Unit) {
    loadNextFrameIfNeeded(width, height)
    onAnimationLoaded()
  }

  override fun compressToFps(fps: Int) {
    val durationMs =
        animationInformation.loopDurationMs.times(animationInformation.loopCount.coerceAtLeast(1))
    compressionFrameMap =
        fpsCompressor.calculateReducedIndexes(
            durationMs = durationMs,
            frameCount = animationInformation.frameCount,
            targetFps = fps.coerceAtMost(animationInformation.fps()))

    renderableFrameIndexes = compressionFrameMap.values.toSet()
  }

  /** Left only the last rendered bitmap on the buffer */
  override fun onStop() {
    val nearestFrame = findNearestFrame(lastRenderedFrameNumber)

    val indexesToClose = bufferFramesHash.keys.minus(nearestFrame?.frameNumber).filterNotNull()
    indexesToClose.forEach { frameNumber ->
      CloseableReference.closeSafely(bufferFramesHash[frameNumber])
      bufferFramesHash.remove(frameNumber)
    }
  }

  /** Release all bitmaps */
  override fun clear() {
    bufferFramesHash.values.forEach { CloseableReference.closeSafely(it) }
    bufferFramesHash.clear()
    lastRenderedFrameNumber = -1
  }

  private fun loadNextFrameIfNeeded(width: Int, height: Int) {
    if (isFetching.getAndSet(true)) {
      return
    }

    AnimationLoaderExecutor.execute(
        object : LoadFramePriorityTask {
          override val priority = LoadFramePriorityTask.Priority.HIGH

          override fun run() {
            do {
              val targetFrame = lastRenderedFrameNumber.coerceAtLeast(0)
              val success = extractDemandedFrame(targetFrame, width, height)
            } while (!success)
            isFetching.set(false)
          }
        })
  }

  @WorkerThread
  private fun extractDemandedFrame(
      targetFrame: Int,
      width: Int,
      height: Int,
      count: Int = 0
  ): Boolean {
    val nextWindow =
        frameSequence.sublist(targetFrame, bufferSize).filter {
          renderableFrameIndexes.contains(it)
        }
    val nextWindowIndexes = nextWindow.toSet()
    val oldFramesNumbers = ArrayDeque(bufferFramesHash.keys.minus(nextWindowIndexes))

    // Load new frames
    nextWindow.forEach { newFrameNumber ->
      if (bufferFramesHash[newFrameNumber] != null) {
        return@forEach
      }

      if (lastRenderedFrameNumber != -1 && !nextWindowIndexes.contains(lastRenderedFrameNumber)) {
        return false
      }

      val deprecatedFrameNumber = oldFramesNumbers.pollFirst()
      val deprecatedFrameBitmap =
          if (deprecatedFrameNumber != null) bufferFramesHash[deprecatedFrameNumber] else null

      val newFrame =
          if (deprecatedFrameBitmap == null) obtainFrame(null, newFrameNumber, width, height)
          else obtainFrame(deprecatedFrameBitmap, newFrameNumber, width, height)

      CloseableReference.closeSafely(deprecatedFrameBitmap)
      deprecatedFrameNumber?.let { bufferFramesHash.remove(it) }

      bufferFramesHash[newFrameNumber] = newFrame
    }

    bufferFramesSequence = nextWindow
    return true
  }

  private fun obtainFrame(
      oldBitmapRef: CloseableReference<Bitmap>?,
      targetFrame: Int,
      width: Int,
      height: Int
  ): CloseableReference<Bitmap> {
    val nearestFrame = findNearestFrame(targetFrame)
    val nearestBitmap = nearestFrame?.bitmap?.cloneOrNull()

    if (nearestFrame != null && nearestBitmap != null) {
      val copyOldBitmap = oldBitmapRef?.cloneOrNull()

      val copyBitmap =
          copyOldBitmap?.set(nearestBitmap.get())
              ?: platformBitmapFactory.createBitmap(nearestBitmap.get())
      CloseableReference.closeSafely(nearestBitmap)

      updateBitmap(copyBitmap.get(), nearestFrame.frameNumber, targetFrame)
      return copyBitmap
    }

    val bitmap = renderFirstBitmap(width, height)
    updateBitmap(bitmap.get(), 0, targetFrame)
    return bitmap
  }

  private fun findNearestFrame(targetFrame: Int): AnimationBitmapFrame? {
    return (0..frameSequence.size).firstNotNullOfOrNull { position ->
      val closestFrame = frameSequence.getPosition(targetFrame - position)
      bufferFramesHash[closestFrame]
          ?.takeIf { it.isValid }
          ?.let { AnimationBitmapFrame(closestFrame, it) }
    }
  }

  private fun renderFirstBitmap(width: Int, height: Int): CloseableReference<Bitmap> {
    val base = platformBitmapFactory.createBitmap(width, height)
    bitmapFrameRenderer.renderFrame(0, base.get())
    return base
  }

  private fun updateBitmap(fromBitmap: Bitmap, from: Int, dest: Int) {
    if (from > dest) {
      fromBitmap.clear()
      (0..dest).forEach { bitmapFrameRenderer.renderFrame(it, fromBitmap) }
    } else if (from < dest) {
      (from + 1..dest).forEach { bitmapFrameRenderer.renderFrame(it, fromBitmap) }
    }
  }

  private fun Bitmap.clear() {
    Canvas(this).drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
  }

  private fun CloseableReference<Bitmap>.set(src: Bitmap): CloseableReference<Bitmap> {
    if (isValid) {
      val canvas = Canvas(get())
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
      canvas.drawBitmap(src, 0f, 0f, null)
    }
    return this
  }

  private fun AnimationInformation.fps(): Int =
      TimeUnit.SECONDS.toMillis(1).div(loopDurationMs.div(frameCount)).coerceAtLeast(1).toInt()

  companion object {

    /**
     * Used to calculate the threshold frame for triggering the next buffer load from the last
     * render frame
     */
    private const val THRESHOLD_PERCENTAGE = 0.5f

    /**
     * Used to calculate how many bitmaps are needed to render this animation. The seconds are
     * multiplied with the FPS of the animation to get required of bitmaps.
     */
    private const val BUFFER_SECOND_SIZE = 1
  }
}
