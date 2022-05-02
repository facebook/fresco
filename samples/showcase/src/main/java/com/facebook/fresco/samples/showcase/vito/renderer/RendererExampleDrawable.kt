/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.renderer

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.fresco.vito.renderer.ImageRenderer
import com.facebook.fresco.vito.renderer.Shape

open class RendererExampleDrawable(
    private val imageDataModel: ImageDataModel,
    private val shape: Shape,
    private val transformationMatrix: Matrix? = null,
    private var imageColorFilter: ColorFilter? = null,
) : Drawable() {
  override fun draw(canvas: Canvas) {
    ImageRenderer.createImageDataModelRenderCommand(
        imageDataModel,
        shape,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = imageColorFilter },
        transformationMatrix)(canvas)
  }

  override fun setAlpha(alpha: Int) = Unit

  override fun setColorFilter(colorFilter: ColorFilter?) {
    imageColorFilter = colorFilter
  }

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
