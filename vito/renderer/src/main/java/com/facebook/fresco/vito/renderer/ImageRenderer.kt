/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer

import android.graphics.*
import android.graphics.drawable.Drawable

typealias RenderCommand = (Canvas) -> Unit

class ImageRenderer {
  companion object {

    fun draw(
        canvas: Canvas,
        renderCommand: RenderCommand,
        clipRect: Rect,
        transformationMatrix: Matrix? = null
    ) {
      // Scale if needed
      if (transformationMatrix != null) {
        val saveCount = canvas.save()
        canvas.clipRect(clipRect)
        canvas.concat(transformationMatrix)

        renderCommand(canvas)

        canvas.restoreToCount(saveCount)
      } else {
        renderCommand(canvas)
      }
    }

    fun createImageDataModelRenderCommand(
        model: ImageDataModel,
        shape: Shape,
        paintToReuse: Paint? = null
    ): RenderCommand {
      return when (model) {
        is BitmapImageDataModel -> model.createRenderCommand(shape, paintToReuse)
        is ColorIntImageDataModel -> model.createRenderCommand(shape, paintToReuse)
        is DrawableImageDataModel -> model.createRenderCommand(shape, paintToReuse)
      }
    }

    inline fun BitmapImageDataModel.createRenderCommand(
        shape: Shape,
        paintToReuse: Paint? = null
    ): RenderCommand {
      when (shape) {
        is RectShape -> {
          return { it.drawBitmap(bitmap, 0f, 0f, null) }
        }
        is CircleShape ->
            if (!isBitmapCircular) {
              val paint = getBitmapPaint(bitmap, paintToReuse)
              return { shape.draw(it, paint) }
            } else {
              return { it.drawBitmap(bitmap, 0f, 0f, null) }
            }
        else -> {
          val paint = getBitmapPaint(bitmap)
          return { shape.draw(it, paint) }
        }
      }
    }

    inline fun ColorIntImageDataModel.createRenderCommand(
        shape: Shape,
        paintToReuse: Paint? = null
    ): RenderCommand {
      val paint = getColorPaint(this, paintToReuse)
      return { shape.draw(it, paint) }
    }

    inline fun DrawableImageDataModel.createRenderCommand(
        shape: Shape,
        paintToReuse: Paint? = null
    ): RenderCommand {
      // We transform by scaling the Canvas, so we let the Drawable draw itself with its
      // preferred dimensions
      when (shape) {
        is RectShape -> return {
              drawable.setBounds(0, 0, width, height)
              drawable.draw(it)
            }

        // TODO: we allocate a bitmap and need to do the scaling math properly,
        // right now this does not render properly
        else -> {
          val paint = paintToReuse ?: Paint(Paint.ANTI_ALIAS_FLAG)
          return {
            drawable.setBounds(0, 0, width, height)
            shape.draw(it, getDrawablePaint(drawable, width, height, paint))
          }
        }
      }
    }

    inline fun getColorPaint(model: ColorIntImageDataModel, paint: Paint? = null): Paint {
      return (reset(paint) ?: Paint(Paint.ANTI_ALIAS_FLAG)).apply { color = model.colorInt }
    }

    inline fun getBitmapPaint(bitmap: Bitmap, paint: Paint? = null): Paint {
      return (reset(paint) ?: Paint(Paint.ANTI_ALIAS_FLAG)).apply {
        shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
      }
    }

    inline fun getDrawablePaint(drawable: Drawable, width: Int, height: Int, paint: Paint): Paint {
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      drawable.draw(Canvas(bitmap))
      return getBitmapPaint(bitmap, paint)
    }

    inline fun reset(paint: Paint?): Paint? = paint?.apply { reset() }
  }
}
