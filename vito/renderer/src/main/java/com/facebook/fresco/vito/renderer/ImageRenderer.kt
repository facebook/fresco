/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import com.facebook.fresco.vito.renderer.util.ColorUtils

typealias RenderCommand = (Canvas) -> Unit

object ImageRenderer {

  /**
   * Creates an image data model render command.
   *
   * NOTE: The render command can modify the Canvas that it draws on and does not restore the
   * canvas. The Canvas has to be saved & restored manually if required.
   */
  fun createImageDataModelRenderCommand(
      model: ImageDataModel,
      shape: Shape,
      paint: Paint,
      imageTransformation: Matrix? = null
  ): RenderCommand {
    return when (model) {
      is BitmapImageDataModel -> model.createRenderCommand(shape, paint, imageTransformation)
      is ColorIntImageDataModel -> model.createRenderCommand(shape, paint)
      is DrawableImageDataModel -> model.createRenderCommand(shape, paint, imageTransformation)
    }
  }

  inline fun BitmapImageDataModel.createRenderCommand(
      shape: Shape,
      paint: Paint,
      imageTransformation: Matrix? = null
  ): RenderCommand =
      when (shape) {
        is RectShape -> bitmapRenderCommand(paint, bitmap, imageTransformation)
        is CircleShape ->
            if (!isBitmapCircular) {
              paintRenderCommand(shape, paint.setBitmap(bitmap, imageTransformation))
            } else {
              bitmapRenderCommand(paint, bitmap, imageTransformation)
            }
        else -> {
          paintRenderCommand(shape, paint.setBitmap(bitmap, imageTransformation))
        }
      }

  inline fun ColorIntImageDataModel.createRenderCommand(shape: Shape, paint: Paint): RenderCommand {
    // The image transformation is a no-op for solid colors since it remains a solid color
    paint.color = ColorUtils.multiplyColorAlpha(colorInt, paint.alpha)
    return paintRenderCommand(shape, paint)
  }

  inline fun DrawableImageDataModel.createRenderCommand(
      shape: Shape,
      paint: Paint,
      imageTransformation: Matrix? = null,
  ): RenderCommand {
    // We transform by scaling the Canvas, so we let the Drawable draw itself with its
    // preferred dimensions
    when (shape) {
      is RectShape -> return { canvas ->
            if (width > 0 && height > 0) {
              drawable.setBounds(0, 0, width, height)
              canvas.concat(imageTransformation)
            } else {
              // The image dimensions are not set, so we assume the image can stretch to fit the
              // entire rect, so we do not apply the transformation.
              drawable.setBounds(
                  shape.rect.left.toInt(),
                  shape.rect.top.toInt(),
                  shape.rect.right.toInt(),
                  shape.rect.bottom.toInt())
            }
            // Some drawable types (eg VectorDrawable) will always invalidate when colorFilter
            // is modified, so check the current value before we update it
            if (Build.VERSION.SDK_INT < 21 || drawable.colorFilter != paint.colorFilter) {
              drawable.colorFilter = paint.colorFilter
            }
            drawable.alpha = paint.alpha
            drawable.draw(canvas)
          }
      else -> {
        return { canvas ->
          drawable.setBounds(0, 0, width, height)
          if (Build.VERSION.SDK_INT < 21 || drawable.colorFilter != null) {
            // The Paint handles the color filter
            drawable.colorFilter = null
          }
          val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
          drawable.draw(Canvas(bitmap))
          paint.setBitmap(bitmap, imageTransformation)
          shape.draw(canvas, paint)
        }
      }
    }
  }

  inline fun bitmapRenderCommand(
      paint: Paint,
      bitmap: Bitmap,
      imageTransformation: Matrix?
  ): RenderCommand = { canvas ->
    canvas.concat(imageTransformation)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
  }

  inline fun paintRenderCommand(shape: Shape, paint: Paint): RenderCommand = { canvas ->
    shape.draw(canvas, paint)
  }

  inline fun Paint.setBitmap(
      bitmap: Bitmap,
      shaderTransformation: Matrix? = null,
  ): Paint {
    shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    shader.setLocalMatrix(shaderTransformation)
    return this
  }
}
