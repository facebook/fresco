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
) {

  fun createBufferLoader(
      cacheKey: String,
      bitmapFrameRenderer: BitmapFrameRenderer,
      animationInformation: AnimationInformation
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
        FpsCompressorInfo(maxFpsRender),
        animationInformation,
        bufferLengthMilliseconds)
  }

  companion object {
    private val UNUSED_FRAME_LOADERS = ConcurrentHashMap<String, UnusedFrameLoader>()

    fun saveUnusedFrame(cacheKey: String, frameLoader: FrameLoader) {
      UNUSED_FRAME_LOADERS[cacheKey] = UnusedFrameLoader(frameLoader, Date())
    }

    fun clearUnusedUntil(until: Date) {
      synchronized(UNUSED_FRAME_LOADERS) {
        val oldItems = UNUSED_FRAME_LOADERS.filter { it.value.insertedTime < until }

        oldItems.forEach {
          it.value.frameLoader.clear()
          UNUSED_FRAME_LOADERS.remove(it.key)
        }
      }
    }
  }
}

private class UnusedFrameLoader(val frameLoader: FrameLoader, val insertedTime: Date)
