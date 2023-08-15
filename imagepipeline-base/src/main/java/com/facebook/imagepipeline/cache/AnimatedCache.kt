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
      animationFrames: AnimationFrames
  ): CloseableReference<AnimationFrames>? {
    return lruCache.cache(key, CloseableReference.of(animationFrames))
  }

  fun removeAnimation(key: String) {
    lruCache.removeAll { cacheKey -> key == cacheKey }
  }

  companion object {
    private const val EVICTION_QUEUE = 50
    private var instance: AnimatedCache? = null

    @JvmStatic
    fun getInstance(memoryMB: Int): AnimatedCache =
        instance ?: AnimatedCache(memoryMB).also { instance = it }
  }
}

/**
 * Represents the bitmaps of one animation.
 *
 * @param bitmapsByFrame are amount of final bitmap that we will render given a frame index. These
 *   amount could be less than original animation if the animation were reduced
 * @param realToCompressIndexMap If animation was reduced, this map describes the equivalence
 *   between original frame number and reduced frame number
 */
class AnimationFrames(
    bitmapsByFrame: Map<Int, CloseableReference<Bitmap>>,
    private val realToCompressIndexMap: Map<Int, Int>
) : Closeable {
  private val concurrentFrames = ConcurrentHashMap(bitmapsByFrame)
  val frames: Map<Int, CloseableReference<Bitmap>>
    get() = concurrentFrames.filter { (_, frame) -> frame.isValid }

  /** Calculate the size of animation */
  val sizeBytes: Int =
      bitmapsByFrame.values.sumOf {
        if (it.isValid) {
          BitmapUtil.getSizeInBytes(it.get())
        } else 0
      }

  /**
   * Return the bitmap (if exists) given a the animation frame.
   *
   * @param frameIndex Frame index in original animation (not reduced animation frame number).
   * @return Bitmap for [frameIndex] if it is available
   */
  fun getFrame(frameIndex: Int): CloseableReference<Bitmap>? {
    val frame =
        if (realToCompressIndexMap.isEmpty()) {
          concurrentFrames[frameIndex]
        } else {
          val reducedIndex = realToCompressIndexMap[frameIndex] ?: return null
          concurrentFrames[reducedIndex]
        }

    return if (frame?.isValid == true) frame else null
  }

  /** Release the stored bitmaps */
  override fun close() {
    concurrentFrames.values.forEach { it.close() }
    concurrentFrames.clear()
  }
}
