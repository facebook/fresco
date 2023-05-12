/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.annotation.UiThread
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.preparation.loadframe.AnimationLoaderExecutor
import com.facebook.fresco.animation.bitmap.preparation.loadframe.LoadFrameOutput
import com.facebook.fresco.animation.bitmap.preparation.loadframe.LoadFrameTask
import com.facebook.fresco.animation.bitmap.preparation.loadframe.LoadFrameTaskFactory
import java.io.Closeable
import java.util.SortedSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

/**
 * Balanced strategy consist on retrieving the animation frames balancing between RAM and CPU.
 * - RAM: Bitmap cache allocates the bitmaps in memory
 * - CPU: OnDemand threads load in advance the next missing bitmap in cache
 *
 * This strategy will increase the CPU usage in order to reduce the RAM allocation. Depending on the
 * asset FPS, the RAM could decrease 15-30%. [ON_DEMAND_PREPARATION_TIME_MS] indicates how many ms
 * needs the OnDemand thread pool to load the next frame:
 * - greater onDemant preparation => lower CPU usage + greater RAM usage
 *
 * Due to this strategy will be unique per animation, it is highly recommended
 * [ON_DEMAND_PREPARATION_TIME_MS] >= 200ms
 */
class BalancedAnimationStrategy(
    animationInformation: AnimationInformation,
    onDemandPreparationMs: Int,
    private val loadFrameTaskFactory: LoadFrameTaskFactory,
    private val bitmapCache: BitmapFrameCache,
    private val downscaleFrameToDrawableDimensions: Boolean,
) : BitmapFramePreparationStrategy {

  private val fetchingFrames = AtomicBoolean(false)
  private val fetchingOnDemand = AtomicBoolean(false)
  private val framesCached: Boolean
    get() = bitmapCache.isAnimationReady()
  private val onDemandFrames: SortedSet<Int> = sortedSetOf()
  private var nextPrepareFrames = SystemClock.uptimeMillis()

  private val frameCount: Int = animationInformation.frameCount
  private val animationWidth: Int = animationInformation.width()
  private val animationHeight: Int = animationInformation.height()
  private val onDemandRatio: Int
  private var onDemandBitmap: OnDemandFrame? = null

  init {
    val avgFrameDurationMs = animationInformation.loopDurationMs.div(frameCount)
    val optimisticRatio = ceil(onDemandPreparationMs.div(avgFrameDurationMs.toFloat())).toInt()
    onDemandRatio = optimisticRatio.coerceAtLeast(2)
  }

  private fun isFirstFrameReady() = bitmapCache.getCachedFrame(0)?.isValid == true

  @UiThread
  override fun prepareFrames(canvasWidth: Int, canvasHeight: Int) {
    // Validate inputs
    if (canvasWidth <= 0 || canvasHeight <= 0 || animationWidth <= 0 || animationHeight <= 0) {
      return
    }

    // Validate status
    if (framesCached || fetchingFrames.get() || SystemClock.uptimeMillis() < nextPrepareFrames) {
      return
    }

    fetchingFrames.set(true)
    val frameSize = calculateFrameSize(canvasWidth, canvasHeight)

    val task =
        if (!isFirstFrameReady()) {
          loadFrameTaskFactory.createFirstFrameTask(
              frameSize.width,
              frameSize.height,
              object : LoadFrameOutput {
                override fun onSuccess(frames: Map<Int, CloseableReference<Bitmap>>) {
                  val cachedSucceed = bitmapCache.onAnimationPrepared(frames)
                  if (!cachedSucceed) {
                    nextPrepareFrames = SystemClock.uptimeMillis() + FETCH_FIRST_CACHE_DELAY_MS
                  }

                  // Once first frame is loaded, then load the rest of frames
                  AnimationLoaderExecutor.execute(loadAllFrames(frameSize))
                }

                override fun onFail() {
                  bitmapCache.clear()
                  fetchingFrames.set(false)
                }
              })
        } else {
          loadAllFrames(frameSize)
        }

    AnimationLoaderExecutor.execute(task)
  }
  private fun loadAllFrames(frameSize: Size): LoadFrameTask {
    return loadFrameTaskFactory.createLoadFullAnimationTask(
        frameSize.width,
        frameSize.height,
        frameCount,
        object : LoadFrameOutput {
          override fun onSuccess(frames: Map<Int, CloseableReference<Bitmap>>) {
            onDemandFrames.clear()
            onDemandFrames.addAll(frames.filter { isOnDemandFrame(it.key) }.map { it.key })

            val memoryFrames = frames.filter { !onDemandFrames.contains(it.key) }

            val cachedSucceed = bitmapCache.onAnimationPrepared(memoryFrames)
            if (!cachedSucceed) {
              nextPrepareFrames = SystemClock.uptimeMillis() + FETCH_FULL_ANIMATION_CACHE_DELAY_MS
            }

            fetchingFrames.set(false)
          }

          override fun onFail() {
            bitmapCache.clear()
            fetchingFrames.set(false)
          }
        })
  }

  @UiThread
  override fun getBitmapFrame(
      frameNumber: Int,
      canvasWidth: Int,
      canvasHeight: Int
  ): CloseableReference<Bitmap>? {
    // Find the bitmap in cache memory (RAM)
    val cache = bitmapCache.getCachedFrame(frameNumber)
    if (cache?.isValid == true) {
      prepareNextOnDemandFrame(frameNumber)
      return cache
    }

    // Check if bitmap should be cached
    if (!isOnDemandFrame(frameNumber)) {
      prepareFrames(canvasWidth, canvasHeight)
    }

    if (onDemandBitmap?.isValidFor(frameNumber) == true) {
      return onDemandBitmap?.bitmap
    }

    return findNearestFrame(frameNumber)
  }

  private fun prepareNextOnDemandFrame(lastFrameRendered: Int) {
    if (fetchingOnDemand.getAndSet(true)) {
      return
    }

    val nextFrame = findNextOnDemandFrame(lastFrameRendered)
    if (nextFrame != null && onDemandBitmap?.isValidFor(nextFrame) != true) {
      val onDemandTask =
          loadFrameTaskFactory.createOnDemandTask(
              nextFrame,
              { bitmapCache.getCachedFrame(it) },
              { bitmap ->
                if (bitmap != null) {
                  onDemandBitmap = OnDemandFrame(nextFrame, bitmap)
                }
                fetchingOnDemand.set(false)
              })
      AnimationLoaderExecutor.execute(onDemandTask)
    } else {
      fetchingOnDemand.set(false)
    }
  }

  /** Calculate if [frameNumber] have to be rendered by onDemand (CPU) */
  private fun isOnDemandFrame(frameNumber: Int): Boolean {
    // If onDemandRatio > frame count, means that we disable onDemand generation
    if (onDemandRatio > frameCount) {
      return false
    }
    // Compare with 1 because we don't want to onDemand the first frame
    return frameNumber % onDemandRatio == 1
  }

  private fun findNearestFrame(fromFrame: Int): CloseableReference<Bitmap>? {
    return (fromFrame downTo 0).asSequence().firstNotNullOfOrNull {
      val frame = bitmapCache.getCachedFrame(it)
      if (frame?.isValid == true) frame else null
    }
  }

  override fun onStop() {
    onDemandBitmap?.close()
    bitmapCache.clear()
  }

  override fun clearFrames() {
    bitmapCache.clear()
  }

  /** Find next frame since [from] index which is missing in cache */
  @UiThread
  private fun findNextOnDemandFrame(from: Int): Int? {
    if (onDemandFrames.isEmpty()) {
      return null
    }

    return onDemandFrames.firstOrNull { it > from } ?: onDemandFrames.first()
  }

  private fun calculateFrameSize(canvasWidth: Int, canvasHeight: Int): Size {
    if (!downscaleFrameToDrawableDimensions) {
      return Size(animationWidth, animationHeight)
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

    return Size(bitmapWidth, bitmapHeight)
  }

  companion object {
    private val FETCH_FIRST_CACHE_DELAY_MS = 500
    private val FETCH_FULL_ANIMATION_CACHE_DELAY_MS = TimeUnit.SECONDS.toMillis(5)
  }
}

private class Size(val width: Int, val height: Int)

private class OnDemandFrame(val frameNumber: Int, val bitmap: CloseableReference<Bitmap>) :
    Closeable {
  fun isValidFor(frameNumber: Int): Boolean = this.frameNumber == frameNumber && bitmap.isValid

  override fun close() {
    bitmap.close()
  }
}
