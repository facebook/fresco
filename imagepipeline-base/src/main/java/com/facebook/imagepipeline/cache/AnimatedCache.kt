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
class AnimatedCache private constructor(memoryMB: Int) {

  private val sizeBytes = memoryMB * MB

  /** % Memory for animations which were recently used, but they are not running now */
  private val evictionRatio = if (memoryMB < 90) 0.0f else 0.30f

  /** 10% Memory is the maximum size of an animation */
  private val maxCacheEntrySize = sizeBytes.times(0.1).toInt()

  private val lruCache =
      LruCountingMemoryCache<String, AnimationFrames>(
          { it.sizeBytes },
          { it.suggestedTrimRatio },
          {
            MemoryCacheParams(
                maxCacheSize = sizeBytes,
                maxCacheEntries = Int.MAX_VALUE,
                maxEvictionQueueSize = sizeBytes.times(evictionRatio).toInt(),
                maxEvictionQueueEntries = EVICTION_QUEUE,
                maxCacheEntrySize = maxCacheEntrySize,
                paramsCheckIntervalMs = TimeUnit.SECONDS.toMillis(5))
          },
          null,
          false,
          false,
      )

  fun getSize(key: String): Int = lruCache.sizeInBytes

  fun findAnimation(key: String): CloseableReference<AnimationFrames>? = lruCache[key]

  fun saveAnimation(
      key: String,
      newFrames: Map<Int, CloseableReference<Bitmap>>
  ): CloseableReference<AnimationFrames>? {
    return lruCache.cache(key, CloseableReference.of(AnimationFrames(newFrames)))
  }

  fun removeAnimation(key: String) {
    lruCache[key]?.close()
  }

  companion object {
    private const val EVICTION_QUEUE = 50
    private var instance: AnimatedCache? = null

    @JvmStatic
    fun getInstance(memoryMB: Int): AnimatedCache =
        instance ?: AnimatedCache(memoryMB).also { instance = it }
  }
}

class AnimationFrames(map: Map<Int, CloseableReference<Bitmap>>) : Closeable {
  val frames = ConcurrentHashMap(map)

  val sizeBytes: Int =
      map.values.sumOf {
        if (it.isValid) {
          BitmapUtil.getSizeInBytes(it.get())
        } else 0
      }

  fun getFrame(frameIndex: Int): CloseableReference<Bitmap>? {
    val frame = frames[frameIndex]
    return if (frame?.isValid == true) frame else null
  }

  override fun close() {
    frames.values.forEach { it.close() }
    frames.clear()
  }
}
