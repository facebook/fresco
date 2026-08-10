/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.ondemandanimation

import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.fresco.animation.bitmap.preparation.loadframe.FpsCompressorInfo
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

class FrameLoaderFactory(
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val maxFpsRender: Int,
    private val bufferLengthMilliseconds: Int,
    private val enableBufferFrameLoaderFix: Boolean = false,
    private val frameLoaderListener: FrameLoaderListener? = null,
    private val enableSingleFrameRendering: Boolean = false,
    val enableUnusedFrameLoaderCleanupSync: Boolean = false,
    val enableUnusedFrameLoaderCleanupSyncAndClear: Boolean = false,
    private val shouldRoundUpFractionalFrameBudget: Boolean = false,
) {

  fun createBufferLoader(
      cacheKey: String,
      bitmapFrameRenderer: BitmapFrameRenderer,
      animationInformation: AnimationInformation,
  ): FrameLoader {
    synchronized(UNUSED_FRAME_LOADERS) {
      val unusedFrameLoader = UNUSED_FRAME_LOADERS[cacheKey]
      if (unusedFrameLoader != null) {
        UNUSED_FRAME_LOADERS.remove(cacheKey)
        return unusedFrameLoader.frameLoader
      }
    }

    return BufferFrameLoader(
        platformBitmapFactory,
        bitmapFrameRenderer,
        FpsCompressorInfo(maxFpsRender, shouldRoundUpFractionalFrameBudget),
        animationInformation,
        bufferLengthMilliseconds,
        enableBufferFrameLoaderFix,
        frameLoaderListener,
        enableSingleFrameRendering,
    )
  }

  companion object {
    private val UNUSED_FRAME_LOADERS = ConcurrentHashMap<String, UnusedFrameLoader>()

    fun saveUnusedFrame(
        cacheKey: String,
        frameLoader: FrameLoader,
        enableSynchronization: Boolean,
        enableSyncAndClear: Boolean,
    ) {
      // Clearing a displaced loader is only safe under the map lock (otherwise it races
      // createBufferLoader() handing that loader out for reuse), so sync-and-clear implies
      // synchronization.
      if (!enableSynchronization && !enableSyncAndClear) {
        UNUSED_FRAME_LOADERS[cacheKey] = UnusedFrameLoader(frameLoader, Date())
        return
      }
      // Match the locking of clearUnusedUntil()/createBufferLoader(): a bare put() can be clobbered
      // by their remove(key), orphaning a just-saved loader that is then never cleared.
      synchronized(UNUSED_FRAME_LOADERS) {
        val previous = UNUSED_FRAME_LOADERS.put(cacheKey, UnusedFrameLoader(frameLoader, Date()))
        // Those two methods are the only callers of clear() and both reach loaders through this
        // map, so overwriting an entry without clearing it leaks the loader's buffered frames.
        if (enableSyncAndClear) {
          previous?.frameLoader?.takeIf { it !== frameLoader }?.clear()
        }
      }
    }

    fun clearUnusedUntil(until: Date) {
      synchronized(UNUSED_FRAME_LOADERS) {
        val oldItems = UNUSED_FRAME_LOADERS.filter { it.value.insertedTime < until }

        oldItems.forEach { entry ->
          entry.value.frameLoader.clear()
          UNUSED_FRAME_LOADERS.remove(entry.key)
        }
      }
    }
  }
}

private class UnusedFrameLoader(val frameLoader: FrameLoader, val insertedTime: Date)
