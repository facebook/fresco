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
class FrescoFrameCache2(
    animatedImageResult: AnimatedImageResult,
    private val animatedDrawableCache: AnimatedCache
) : BitmapFrameCache {

  private val cacheKey: String =
      animatedImageResult.source ?: animatedImageResult.image.hashCode().toString()
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

    if (frameBitmaps.size >= loadedFramesCount) {
      val cacheRef = animatedDrawableCache.saveAnimation(cacheKey, frameBitmaps)

      // Check if we had enough space to allocate the animation
      if (cacheRef == null) {
        // TODO Find a way to skip frames and try to reallocate again or clear exclusive animations
        // from AnimatedCache
        return false
      } else {
        releaseCache()
        this.animationFrames = cacheRef
      }
    }
    return true
  }

  @Synchronized
  private fun safeAnimationFrames(): AnimationFrames? {
    val animatedCache =
        animationFrames ?: animatedDrawableCache.findAnimation(cacheKey) ?: return null

    // animatedCache instance is shared between this class and AnimatedCache class. Then we need to
    // specify that this instance cannot be modified when we perform .get()
    return synchronized(animatedCache) { if (animatedCache.isValid) animatedCache.get() else null }
  }

  override fun setFrameCacheListener(frameCacheListener: FrameCacheListener?) = Unit
}
