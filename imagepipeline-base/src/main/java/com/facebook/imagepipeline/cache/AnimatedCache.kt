/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.common.util.ByteConstants.MB
import com.facebook.imageutils.BitmapUtil
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Store frames from animations which are being displayed at this moment */
class AnimatedCache(memoryMegaBytes: Int) {

  private val sizeBytes = memoryMegaBytes * MB
  private val lruCache =
      LruCountingMemoryCache<String, AnimationFrames>(
          { it.sizeBytes },
          { it.suggestedTrimRatio },
          {
            MemoryCacheParams(
                maxCacheSize = sizeBytes,
                maxCacheEntries = 2000,
                maxEvictionQueueSize = sizeBytes.times(0.5).toInt(),
                maxEvictionQueueEntries = 1000,
                maxCacheEntrySize = sizeBytes.times(0.1).toInt(),
                paramsCheckIntervalMs = TimeUnit.SECONDS.toMillis(5))
          },
          null,
          false,
          false,
      )

  fun getAnimationFrame(key: String, frameIndex: Int): CloseableReference<Bitmap>? {
    lruCache[key]?.let { cache ->
      if (!cache.isValid) {
        return@let
      }

      val frame = cache.get().getFrame(frameIndex) ?: return@let
      if (frame.isValid) {
        return frame
      }
    }

    return null
  }

  fun getSize(key: String): Int {
    lruCache[key]?.let {
      if (it.isValid) {
        return it.get().sizeBytes
      }
    }

    return 0
  }

  fun saveAnimation(key: String, newFrames: Map<Int, CloseableReference<Bitmap>>) {
    val cachedAnimation = lruCache[key]
    val mergedFrames = newFrames.toMutableMap()

    if (cachedAnimation?.isValid == true) {
      cachedAnimation
          .get()
          .frames
          .filter { !mergedFrames.contains(it.key) }
          .forEach { mergedFrames[it.key] = it.value.clone() }
    }

    lruCache.cache(key, CloseableReference.of(AnimationFrames(mergedFrames)))
  }

  fun removeAnimation(key: String) {
    lruCache[key]?.close()
  }
}

private class AnimationFrames(map: Map<Int, CloseableReference<Bitmap>>) : Closeable {
  val frames = ConcurrentHashMap(map)

  val sizeBytes: Int =
      map.values.sumOf {
        if (it.isValid) {
          BitmapUtil.getSizeInBytes(it.get())
        } else 0
      }

  fun getFrame(frameIndex: Int): CloseableReference<Bitmap>? = frames[frameIndex]

  override fun close() {
    frames.values.forEach { it.close() }
    frames.clear()
  }
}
