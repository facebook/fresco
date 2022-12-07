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
import java.util.concurrent.atomic.AtomicBoolean

/** Bitmap frame cache used for animated drawables */
class FrescoFrameCache2(
    animatedImageResult: AnimatedImageResult,
    private val animatedDrawableCache: AnimatedCache
) : BitmapFrameCache {

  private val cacheKey: String =
      animatedImageResult.source ?: animatedImageResult.image.hashCode().toString()
  private var animatedCache: CloseableReference<AnimationFrames>? =
      animatedDrawableCache.findAnimation(cacheKey)
  private val isCleared = AtomicBoolean(true)

  override fun getCachedFrame(frameNumber: Int): CloseableReference<Bitmap>? {
    if (isCleared.getAndSet(false)) {
      animatedCache = animatedDrawableCache.findAnimation(cacheKey)
    }

    return if (animatedCache?.isValid == true) {
      animatedCache?.get()?.getFrame(frameNumber)
    } else {
      releaseCache()
      null
    }
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
    get() = if (animatedCache?.isValid == true) animatedCache?.get()?.sizeBytes ?: 0 else 0

  override fun clear() {
    isCleared.set(true)
    releaseCache()
  }

  private fun releaseCache() {
    animatedCache?.close()
    animatedCache = null
  }

  override fun isAnimationReady(): Boolean {
    return if (animatedCache?.isValid == true) animatedCache?.get()?.frames.orEmpty().size > 1
    else false
  }

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
    if (frameBitmaps.size >= animatedCache?.get()?.frames.orEmpty().size) {
      val cacheRef = animatedDrawableCache.saveAnimation(cacheKey, frameBitmaps)

      // Check if we had enough space to allocate the animation
      if (cacheRef == null) {
        // TODO Find a way to skip frames and try to reallocate again or clear exclusive animations
        // from AnimatedCache
        return false
      } else {
        releaseCache()
        animatedCache = cacheRef
      }
    }
    return true
  }

  override fun setFrameCacheListener(frameCacheListener: FrameCacheListener?) = Unit
}
