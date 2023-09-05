/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

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
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameResult.FrameType
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This frame store every animation bitmap in an internal cache and they are provided in O(1). If
 * the bitmap is missing, it will provide the nearest bitmap and will execute a background thread to
 * collect and save all the animation bitmaps.
 */
class CacheFrameLoader(
    private val loadFrameTaskFactory: LoadFrameTaskFactory,
    private val bitmapCache: BitmapFrameCache,
    override val animationInformation: AnimationInformation
) : FrameLoader {

  private val framesCached: Boolean
    get() = bitmapCache.isAnimationReady()

  private val fetchingFrames = AtomicBoolean(false)
  private var nextPrepareFrames = SystemClock.uptimeMillis()

  private fun isFirstFrameReady() = bitmapCache.getCachedFrame(0)?.isValid == true

  @UiThread
  override fun getFrame(frameNumber: Int, width: Int, height: Int): FrameResult {
    val cache = bitmapCache.getCachedFrame(frameNumber)
    if (cache?.isValid == true) {
      return FrameResult(cache, FrameType.SUCCESS)
    }

    prepareFrames(width, height) {}

    return findNearestFrame(frameNumber)?.let { FrameResult(it, FrameType.NEAREST) }
        ?: FrameResult(null, FrameType.MISSING)
  }

  @UiThread
  override fun prepareFrames(width: Int, height: Int, onAnimationLoaded: () -> Unit) {
    // Validate status
    if (framesCached || fetchingFrames.get() || SystemClock.uptimeMillis() < nextPrepareFrames) {
      if (framesCached) {
        onAnimationLoaded.invoke()
      }
      return
    }

    fetchingFrames.set(true)

    val task =
        if (!isFirstFrameReady()) {
          loadFirstFrame(width, height, onAnimationLoaded)
        } else {
          loadAllFrames(width, height, onAnimationLoaded)
        }

    AnimationLoaderExecutor.execute(task)
  }

  override fun clear() {
    bitmapCache.clear()
  }

  private fun loadFirstFrame(width: Int, height: Int, onAnimationLoaded: () -> Unit) =
      loadFrameTaskFactory.createFirstFrameTask(
          width,
          height,
          object : LoadFrameOutput {
            override fun onSuccess(frames: Map<Int, CloseableReference<Bitmap>>) {
              val cachedSucceed = bitmapCache.onAnimationPrepared(frames)
              if (!cachedSucceed) {
                nextPrepareFrames = SystemClock.uptimeMillis() + FETCH_FIRST_CACHE_DELAY_MS
              }

              // Once first frame is loaded, then load the rest of frames
              AnimationLoaderExecutor.execute(loadAllFrames(width, height, onAnimationLoaded))
            }

            override fun onFail() {
              bitmapCache.clear()
              fetchingFrames.set(false)
            }
          })

  private fun loadAllFrames(width: Int, height: Int, notifyOnLoad: (() -> Unit)?): LoadFrameTask {
    return loadFrameTaskFactory.createLoadFullAnimationTask(
        width,
        height,
        animationInformation.frameCount,
        object : LoadFrameOutput {
          override fun onSuccess(frames: Map<Int, CloseableReference<Bitmap>>) {
            val cachedSucceed = bitmapCache.onAnimationPrepared(frames)
            if (!cachedSucceed) {
              nextPrepareFrames = SystemClock.uptimeMillis() + FETCH_FULL_ANIMATION_CACHE_DELAY_MS
            }

            notifyOnLoad?.invoke()
            fetchingFrames.set(false)
          }

          override fun onFail() {
            bitmapCache.clear()
            fetchingFrames.set(false)
          }
        })
  }

  private fun findNearestFrame(fromFrame: Int): CloseableReference<Bitmap>? =
      (fromFrame downTo 0).firstNotNullOfOrNull {
        val frame = bitmapCache.getCachedFrame(it)
        if (frame?.isValid == true) frame else null
      }

  companion object {
    private const val FETCH_FIRST_CACHE_DELAY_MS = 500
    private val FETCH_FULL_ANIMATION_CACHE_DELAY_MS = TimeUnit.SECONDS.toMillis(5)
  }
}
