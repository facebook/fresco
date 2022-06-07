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
import javax.annotation.concurrent.GuardedBy

/** Simple bitmap cache that keeps the last frame and reuses it if possible. */
class KeepLastFrameCache : BitmapFrameCache {

  private var lastFrameNumber = FRAME_NUMBER_UNSET
  private var frameCacheListener: FrameCacheListener? = null

  @GuardedBy("this") private var lastBitmapReference: CloseableReference<Bitmap>? = null

  @Synchronized
  override fun getCachedFrame(frameNumber: Int): CloseableReference<Bitmap>? =
      if (lastFrameNumber == frameNumber) {
        CloseableReference.cloneOrNull(lastBitmapReference)
      } else {
        null
      }

  @Synchronized
  override fun getFallbackFrame(frameNumber: Int): CloseableReference<Bitmap>? =
      CloseableReference.cloneOrNull(lastBitmapReference)

  @Synchronized
  override fun getBitmapToReuseForFrame(
      frameNumber: Int,
      width: Int,
      height: Int
  ): CloseableReference<Bitmap>? =
      try {
        CloseableReference.cloneOrNull(lastBitmapReference)
      } finally {
        closeAndResetLastBitmapReference()
      }

  @Synchronized
  override fun contains(frameNumber: Int): Boolean =
      frameNumber == lastFrameNumber && CloseableReference.isValid(lastBitmapReference)

  @get:Synchronized
  override val sizeInBytes: Int
    get() =
        if (lastBitmapReference == null) 0
        else BitmapUtil.getSizeInBytes(lastBitmapReference!!.get())

  @Synchronized
  override fun clear() {
    closeAndResetLastBitmapReference()
  }

  @Synchronized
  override fun onFrameRendered(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  ) {
    if (lastBitmapReference != null && bitmapReference.get() == lastBitmapReference?.get()) {
      return
    }
    CloseableReference.closeSafely(lastBitmapReference)
    if (lastFrameNumber != FRAME_NUMBER_UNSET) {
      frameCacheListener?.onFrameEvicted(this, lastFrameNumber)
    }
    lastBitmapReference = CloseableReference.cloneOrNull(bitmapReference)
    frameCacheListener?.onFrameCached(this, frameNumber)
    lastFrameNumber = frameNumber
  }

  override fun onFramePrepared(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  ) = Unit

  override fun setFrameCacheListener(frameCacheListener: FrameCacheListener?) {
    this.frameCacheListener = frameCacheListener
  }

  @Synchronized
  private fun closeAndResetLastBitmapReference() {
    if (lastFrameNumber != FRAME_NUMBER_UNSET) {
      frameCacheListener?.onFrameEvicted(this, lastFrameNumber)
    }
    CloseableReference.closeSafely(lastBitmapReference)
    lastBitmapReference = null
    lastFrameNumber = FRAME_NUMBER_UNSET
  }

  companion object {
    private const val FRAME_NUMBER_UNSET = -1
  }
}
