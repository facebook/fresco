/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer

import android.graphics.*

typealias RenderCommand = (Canvas) -> Unit

class ImageRenderer {
  companion object {

    /**
     * Creates an image data model render command.
     *
     * NOTE: The render command can modify the Canvas that it draws on and does not restore the
     * canvas. The Canvas has to be saved & restored manually if required.
     */
    fun createImageDataModelRenderCommand(
        model: ImageDataModel,
        shape: Shape,
        imageTransformation: Matrix? = null,
        paintToReuse: Paint? = null,
        colorFilter: ColorFilter? = null,
    ): RenderCommand {
      return when (model) {
        is BitmapImageDataModel ->
            model.createRenderCommand(shape, imageTransformation, paintToReuse, colorFilter)
        is ColorIntImageDataModel ->
            model.createRenderCommand(shape, imageTransformation, paintToReuse, colorFilter)
        is DrawableImageDataModel ->
            model.createRenderCommand(shape, imageTransformation, paintToReuse, colorFilter)
      }
    }

    inline fun BitmapImageDataModel.createRenderCommand(
        shape: Shape,
        imageTransformation: Matrix? = null,
        paintToReuse: Paint? = null,
        colorFilter: ColorFilter? = null,
    ): RenderCommand {
      when (shape) {
        is RectShape -> {
          val colorFilterPaint =
              if (colorFilter != null) getPaint(paintToReuse, colorFilter) else null
          return {
            it.concat(imageTransformation)
            it.drawBitmap(bitmap, 0f, 0f, colorFilterPaint)
          }
        }
        is CircleShape ->
            if (!isBitmapCircular) {
              val paint = getBitmapPaint(bitmap, imageTransformation, paintToReuse, colorFilter)
              return { shape.draw(it, paint) }
            } else {
              val colorFilterPaint =
                  if (colorFilter != null) getPaint(paintToReuse, colorFilter) else null
              return {
                it.concat(imageTransformation)
                it.drawBitmap(bitmap, 0f, 0f, colorFilterPaint)
              }
            }
        else -> {
          val paint = getBitmapPaint(bitmap, imageTransformation, paintToReuse, colorFilter)
          return { shape.draw(it, paint) }
        }
      }
    }

    inline fun ColorIntImageDataModel.createRenderCommand(
        shape: Shape,
        imageTransformation: Matrix? = null,
        paintToReuse: Paint? = null,
        colorFilter: ColorFilter? = null,
    ): RenderCommand {
      // The image transformation is a no-op for solid colors since it is a no-op
      val paint = getPaint(paintToReuse, colorFilter).apply { color = colorInt }
      return { shape.draw(it, paint) }
    }

    inline fun DrawableImageDataModel.createRenderCommand(
        shape: Shape,
        imageTransformation: Matrix? = null,
        paintToReuse: Paint? = null,
        colorFilter: ColorFilter? = null,
    ): RenderCommand {
      // We transform by scaling the Canvas, so we let the Drawable draw itself with its
      // preferred dimensions
      when (shape) {
        is RectShape -> return {
              if (width >= 0 && height >= 0) {
                drawable.setBounds(0, 0, width, height)
                it.concat(imageTransformation)
              } else {
                // The image dimensions are not set, so we assume the image can stretch to fit the
                // entire rect, so we do not apply the transformation.
                drawable.setBounds(
                    shape.rect.left.toInt(),
                    shape.rect.top.toInt(),
                    shape.rect.right.toInt(),
                    shape.rect.bottom.toInt())
              }
              drawable.colorFilter = colorFilter
              drawable.draw(it)
            }
        else -> {
          return {
            drawable.setBounds(0, 0, width, height)
            drawable.colorFilter = null // The Paint handles the color filter
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawable.draw(Canvas(bitmap))
            shape.draw(it, getBitmapPaint(bitmap, imageTransformation, paintToReuse, colorFilter))
          }
        }
      }
    }

    inline fun getBitmapPaint(
        bitmap: Bitmap,
        shaderTransformation: Matrix? = null,
        paintToReuse: Paint? = null,
        colorFilter: ColorFilter? = null
    ): Paint {
      return getPaint(paintToReuse, colorFilter).apply {
        shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(shaderTransformation)
      }
    }

    inline fun getPaint(paintToReuse: Paint? = null, colorFilter: ColorFilter? = null): Paint {
      return (paintToReuse?.apply { reset() } ?: Paint(Paint.ANTI_ALIAS_FLAG)).apply {
        this.colorFilter = colorFilter
      }
    }
  }
}
