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

/** Bitmap frame cache used for animated drawables */
class FrescoFrameCache2(
    animatedImageResult: AnimatedImageResult,
    private val animatedDrawableCache: AnimatedCache
) : BitmapFrameCache {

  private val cacheKey: String =
      animatedImageResult.source ?: animatedImageResult.image.hashCode().toString()

  override fun getCachedFrame(frameNumber: Int): CloseableReference<Bitmap>? {
    return animatedDrawableCache.getAnimationFrame(cacheKey, frameNumber)
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
    get() = animatedDrawableCache.getSize(cacheKey)

  override fun clear() {
    return animatedDrawableCache.removeAnimation(cacheKey)
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

  override fun onAnimationPrepared(frameBitmaps: List<CloseableReference<Bitmap>>) {
    val bitmapMap =
        frameBitmaps.mapIndexed { index, closeableReference -> index to closeableReference }.toMap()
    animatedDrawableCache.saveAnimation(cacheKey, bitmapMap)
  }

  override fun setFrameCacheListener(frameCacheListener: FrameCacheListener?) = Unit
}
