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
import com.facebook.imageutils.BitmapUtil
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** Bitmap frame cache used for animated drawables */
class FrescoFrameCache2 : BitmapFrameCache {

  private val cache: ConcurrentMap<Int, CloseableReference<Bitmap>> = ConcurrentHashMap()

  override fun getCachedFrame(frameNumber: Int): CloseableReference<Bitmap>? {
    return if (cache.containsKey(frameNumber)) {
      cache[frameNumber]
    } else null
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
    return cache.containsKey(frameNumber)
  }

  override val sizeInBytes: Int
    get() = cache.values.filter { it.isValid }.sumOf { BitmapUtil.getSizeInBytes(it.get()) }

  override fun clear() {
    cache.values.forEach { CloseableReference.closeSafely(it) }
    cache.clear()
  }

  override fun onFrameRendered(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  ) {}

  override fun onFramePrepared(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  ) {
    cache[frameNumber] = bitmapReference
  }

  override fun setFrameCacheListener(frameCacheListener: FrameCacheListener?) {}
}
