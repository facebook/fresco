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
import com.facebook.imagepipeline.animated.base.AnimatedImage
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.cache.AnimatedCache
import com.facebook.imagepipeline.cache.AnimationFrames
import java.util.concurrent.TimeUnit

/** Bitmap frame cache used for animated drawables */
class FrescoFpsCache(
    private val animatedImageResult: AnimatedImageResult,
    private val fpsCompressorInfo: FpsCompressorInfo,
    private val animatedDrawableCache: AnimatedCache
) : BitmapFrameCache {

  /** Unique reference for this animation asset */
  private val cacheKey: String =
      animatedImageResult.source ?: animatedImageResult.image.hashCode().toString()

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
      animationFrames = cacheRef
    }

    return cacheRef != null
  }

  private fun compressAnimation(
      frameBitmaps: Map<Int, CloseableReference<Bitmap>>
  ): CloseableReference<AnimationFrames>? {
    var fps = animatedImageResult.image.fps()
    var animationFrames: CloseableReference<AnimationFrames>? = null

    while (animationFrames == null && fps > 1) {
      val compressionResult =
          fpsCompressorInfo.compress(animatedImageResult.image, frameBitmaps, fps)
      val animation =
          AnimationFrames(compressionResult.compressedAnim, compressionResult.realToReducedIndex)
      animationFrames = animatedDrawableCache.saveAnimation(cacheKey, animation)

      if (animationFrames != null) {
        compressionResult.removedFrames.forEach { it.close() }
      }

      fps -= FPS_COMPRESSION_STEP
    }

    return animationFrames
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

  private fun AnimatedImage.fps(): Int {
    val frameMs = duration.div(frameCount.coerceAtLeast(1))
    return TimeUnit.SECONDS.toMillis(1).div(frameMs.coerceAtLeast(1)).toInt()
  }

  companion object {
    private const val FPS_COMPRESSION_STEP = 4
  }
}
