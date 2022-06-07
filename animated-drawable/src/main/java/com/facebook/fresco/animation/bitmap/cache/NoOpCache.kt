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

/** No-op bitmap cache that doesn't do anything. */
class NoOpCache : BitmapFrameCache {

  override fun getCachedFrame(frameNumber: Int): CloseableReference<Bitmap>? = null

  override fun getFallbackFrame(frameNumber: Int): CloseableReference<Bitmap>? = null

  override fun getBitmapToReuseForFrame(
      frameNumber: Int,
      width: Int,
      height: Int
  ): CloseableReference<Bitmap>? = null

  override fun contains(frameNumber: Int): Boolean = false

  override val sizeInBytes: Int = 0

  override fun clear() {
    // no-op
  }

  override fun onFrameRendered(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  ) {
    // no-op
  }

  override fun onFramePrepared(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  ) {
    // Does not cache anything
  }

  override fun setFrameCacheListener(frameCacheListener: FrameCacheListener?) {
    // Does not cache anything
  }
}
