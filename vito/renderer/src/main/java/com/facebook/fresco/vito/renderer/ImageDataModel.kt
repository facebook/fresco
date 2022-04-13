/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

sealed class ImageDataModel {
  open val width = -1
  open val height = -1

  open fun setCallback(callback: Drawable.Callback?) = Unit
}

class ColorIntImageDataModel(val colorInt: Int) : ImageDataModel()

class BitmapImageDataModel(val bitmap: Bitmap, val isBitmapCircular: Boolean = false) :
    ImageDataModel() {
  override val width = bitmap.width
  override val height = bitmap.height
}

class DrawableImageDataModel(val drawable: Drawable) : ImageDataModel() {
  override val width = drawable.intrinsicWidth
  override val height = drawable.intrinsicHeight

  override fun setCallback(callback: Drawable.Callback?) {
    drawable.callback = callback
  }
}
