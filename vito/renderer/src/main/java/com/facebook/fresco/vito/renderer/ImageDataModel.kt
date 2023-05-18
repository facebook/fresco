/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable

sealed class ImageDataModel {
  open val width = -1
  open val height = -1
  open val defaultPaintFlags = Paint.ANTI_ALIAS_FLAG

  open fun setCallback(callback: Drawable.Callback?) = Unit
  open fun onAttach() = Unit
  open fun onDetach() = Unit
}

class ColorIntImageDataModel(val colorInt: Int) : ImageDataModel()

class BitmapImageDataModel(val bitmap: Bitmap, val isBitmapCircular: Boolean = false) :
    ImageDataModel() {
  override val width = bitmap.width
  override val height = bitmap.height
  override val defaultPaintFlags: Int = Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG
}

open class DrawableImageDataModel(val drawable: Drawable) : ImageDataModel() {
  override val width = if (drawable is NinePatchDrawable) -1 else drawable.intrinsicWidth
  override val height = if (drawable is NinePatchDrawable) -1 else drawable.intrinsicHeight

  override fun setCallback(callback: Drawable.Callback?) {
    drawable.callback = callback
  }
}

class AnimatedDrawableImageDataModel(
    drawable: Drawable,
    val animatable: Animatable,
    val isAutoPlay: Boolean
) : DrawableImageDataModel(drawable) {
  override fun onAttach() {
    if (isAutoPlay) {
      animatable.start()
    }
  }
  override fun onDetach() {
    // We only update if we started the animation
    if (isAutoPlay) {
      animatable.stop()
    }
  }
}
