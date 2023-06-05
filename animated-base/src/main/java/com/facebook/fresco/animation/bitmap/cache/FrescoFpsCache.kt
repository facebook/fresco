/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.cache

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend.FrameType
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.BitmapFrameCache.FrameCacheListener
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.cache.AnimatedCache
import com.facebook.imagepipeline.cache.AnimationFrames

/** Bitmap frame cache used for animated drawables */
class FrescoFpsCache(
    animatedImageResult: AnimatedImageResult,
    private val maxFps: Int,
    private val animatedDrawableCache: AnimatedCache
) : BitmapFrameCache {

  /** Unique reference for this animation asset */
  private val cacheKey: String =
      animatedImageResult.source ?: animatedImageResult.image.hashCode().toString()

  /** Duration in ms of the animation */
  private val assetDurationMs = animatedImageResult.image.duration.toLong()

  /** Reference to the loaded animation */
  private var animationFrames: CloseableReference<AnimationFrames>? =
      animatedDrawableCache.findAnimation(cacheKey)

  override fun getCachedFrame(frameNumber: Int): CloseableReference<Bitmap>? {
    return safeAnimationFrames()?.getFrame(frameNumber)
  }

  override fun getFallbackFrame(frameNumber: Int): CloseableReference<Bitmap>? {
    return null
  }

  override fun getBitmapToReuseForFrame(
      frameNumber: Int,
      width: Int,
      height: Int
  ): CloseableReference<Bitmap>? {
    return null
  }

  override fun contains(frameNumber: Int): Boolean {
    return getCachedFrame(frameNumber) != null
  }

  override val sizeInBytes: Int
    get() = safeAnimationFrames()?.sizeBytes ?: 0

  override fun clear() {
    releaseCache()
  }

  private fun releaseCache() {
    animationFrames?.close()
    animationFrames = null
  }

  override fun isAnimationReady(): Boolean = safeAnimationFrames()?.frames.orEmpty().size > 1

  override fun onFrameRendered(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  ) = Unit

  override fun onFramePrepared(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  ) = Unit

  override fun onAnimationPrepared(frameBitmaps: Map<Int, CloseableReference<Bitmap>>): Boolean {
    val loadedFramesCount = safeAnimationFrames()?.frames.orEmpty().size

    // If saved quality is higher than new, then skip
    if (frameBitmaps.size < loadedFramesCount) {
      return true
    }

    val cacheRef = compressAnimation(frameBitmaps)
    if (cacheRef != null) {
      releaseCache()
      this.animationFrames = cacheRef
    }

    return cacheRef != null
  }

  /**
   * Creates a AnimationFrame reference allocated in AnimatedCache. This method compress the
   * animation based on the maximum fps allowed. The animation will reduce the number of frames
   * until it can be allocated in memory
   *
   * @param frameBitmaps has the bitmaps of the animation. FrameNumber -> Bitmap
   * @return AnimationFrame reference if the animation was saved in memory. Returns null if
   *   animation couldn't be allocated
   */
  private fun compressAnimation(
      frameBitmaps: Map<Int, CloseableReference<Bitmap>>
  ): CloseableReference<AnimationFrames>? {
    var fps = maxFps
    var animationFrames: CloseableReference<AnimationFrames>? = null

    while (animationFrames == null && fps > 1) {
      val realToCompressIndex = calculateReducedIndexes(frameBitmaps.size, fps)
      val compressedAnimation = releaseCompressedBitmaps(frameBitmaps, realToCompressIndex)
      val animation = AnimationFrames(compressedAnimation, realToCompressIndex)

      animationFrames = animatedDrawableCache.saveAnimation(cacheKey, animation)
      fps -= FPS_COMPRESSION_STEP
    }

    return animationFrames
  }

  /**
   * Create a Map<AssetBitmapIndex, CompressBitmapIndex> calculated based on the maximum FPS allowed
   *
   * @param animationFramesCount Number of frames extracted from the animation asset
   * @param fpsTarget Maximum fps
   * @return Map of equivalences between the original frame number and compress frame number
   */
  private fun calculateReducedIndexes(animationFramesCount: Int, fpsTarget: Int): Map<Int, Int> {
    val maxAllowedFrames = fpsTarget.times(assetDurationMs.div(1000f)).coerceAtLeast(0f)

    return if (maxAllowedFrames >= animationFramesCount) {
      (0 until animationFramesCount).associateWith { it }
    } else {
      val offset = maxAllowedFrames.div(animationFramesCount.toFloat())
      (0 until animationFramesCount).associateWith { it.times(offset).toInt() }
    }
  }

  /**
   * Compress animation releasing those bitmaps which wont be in the final animation
   *
   * @param frameBitmaps relation frameNumber->bitmap of the original animation
   * @param realToReducedIndex map of equivalences between originalFrameNumber->compressFrameNumber
   * @return map associating compressFrameNumber->bitmap
   */
  private fun releaseCompressedBitmaps(
      frameBitmaps: Map<Int, CloseableReference<Bitmap>>,
      realToReducedIndex: Map<Int, Int>
  ): Map<Int, CloseableReference<Bitmap>> {
    val compressedAnim = mutableMapOf<Int, CloseableReference<Bitmap>>()
    frameBitmaps.forEach { (i, bitmapRef) ->
      val reducedIndex = realToReducedIndex[i] ?: return@forEach
      if (compressedAnim.contains(reducedIndex)) {
        // Release this bitmap because it wont be used
        CloseableReference.closeSafely(bitmapRef)
      } else {
        compressedAnim[reducedIndex] = bitmapRef
      }
    }
    return compressedAnim
  }

  /**
   * Return animation frames based on the cacheKey. Check if current animationFrames exists,
   * otherwise fetch into AnimationCache in case the animation was loaded previously and they are
   * valid
   *
   * @return animation frames which contains the bitmaps of the animation
   */
  @Synchronized
  private fun safeAnimationFrames(): AnimationFrames? {
    val animatedCache =
        animationFrames ?: animatedDrawableCache.findAnimation(cacheKey) ?: return null

    // animatedCache instance is shared between this class and AnimatedCache class. Then we need to
    // specify that this instance cannot be modified when we perform .get()
    return synchronized(animatedCache) { if (animatedCache.isValid) animatedCache.get() else null }
  }

  override fun setFrameCacheListener(frameCacheListener: FrameCacheListener?) = Unit

  companion object {
    private const val FPS_COMPRESSION_STEP = 4
  }
}
