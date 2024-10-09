/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.wrapper

import android.graphics.Bitmap
import android.graphics.Rect
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor.Callback

/** [BitmapFrameRenderer] that wraps around an [AnimatedDrawableBackend]. */
class AnimatedDrawableBackendFrameRenderer(
    private val bitmapFrameCache: BitmapFrameCache,
    private var animatedDrawableBackend: AnimatedDrawableBackend,
    private val isNewRenderImplementation: Boolean
) : BitmapFrameRenderer {

  private var animatedImageCompositor: AnimatedImageCompositor

  private val callback: AnimatedImageCompositor.Callback =
      object : AnimatedImageCompositor.Callback {
        override fun onIntermediateResult(frameNumber: Int, bitmap: Bitmap) {
          // We currently don't cache intermediate bitmaps here
        }

        override fun getCachedBitmap(frameNumber: Int): CloseableReference<Bitmap>? =
            bitmapFrameCache.getCachedFrame(frameNumber)
      }

  override fun setBounds(bounds: Rect?) {
    val newBackend = animatedDrawableBackend.forNewBounds(bounds)
    if (newBackend !== animatedDrawableBackend) {
      animatedDrawableBackend = newBackend
      animatedImageCompositor =
          AnimatedImageCompositor(animatedDrawableBackend, isNewRenderImplementation, callback)
    }
  }

  override val intrinsicWidth: Int
    get() = animatedDrawableBackend.width

  override val intrinsicHeight: Int
    get() = animatedDrawableBackend.height

  init {
    animatedImageCompositor =
        AnimatedImageCompositor(
            this@AnimatedDrawableBackendFrameRenderer.animatedDrawableBackend,
            isNewRenderImplementation,
            callback)
  }

  override fun renderFrame(frameNumber: Int, targetBitmap: Bitmap): Boolean {
    try {
      animatedImageCompositor.renderFrame(frameNumber, targetBitmap)
    } catch (exception: IllegalStateException) {
      FLog.e(TAG, exception, "Rendering of frame unsuccessful. Frame number: %d", frameNumber)
      return false
    }
    return true
  }

  companion object {
    private val TAG: Class<*> = AnimatedDrawableBackendFrameRenderer::class.java
  }
}
