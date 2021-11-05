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
        paintToReuse: Paint? = null
    ): RenderCommand {
      return when (model) {
        is BitmapImageDataModel ->
            model.createRenderCommand(shape, imageTransformation, paintToReuse)
        is ColorIntImageDataModel ->
            model.createRenderCommand(shape, imageTransformation, paintToReuse)
        is DrawableImageDataModel ->
            model.createRenderCommand(shape, imageTransformation, paintToReuse)
      }
    }

    inline fun BitmapImageDataModel.createRenderCommand(
        shape: Shape,
        imageTransformation: Matrix? = null,
        paintToReuse: Paint? = null
    ): RenderCommand {
      when (shape) {
        is RectShape -> {
          return {
            it.concat(imageTransformation)
            it.drawBitmap(bitmap, 0f, 0f, null)
          }
        }
        is CircleShape ->
            if (!isBitmapCircular) {
              val paint = getBitmapPaint(bitmap, imageTransformation, paintToReuse)
              return { shape.draw(it, paint) }
            } else {
              return {
                it.concat(imageTransformation)
                it.drawBitmap(bitmap, 0f, 0f, null)
              }
            }
        else -> {
          val paint = getBitmapPaint(bitmap, imageTransformation, paintToReuse)
          return { shape.draw(it, paint) }
        }
      }
    }

    inline fun ColorIntImageDataModel.createRenderCommand(
        shape: Shape,
        imageTransformation: Matrix? = null,
        paintToReuse: Paint? = null
    ): RenderCommand {
      // The image transformation is a no-op for solid colors since it is a no-op
      val paint = getColorPaint(this, paintToReuse)
      return { shape.draw(it, paint) }
    }

    inline fun DrawableImageDataModel.createRenderCommand(
        shape: Shape,
        imageTransformation: Matrix? = null,
        paintToReuse: Paint? = null
    ): RenderCommand {
      // TODO(T105148151): account for image transformation
      // We transform by scaling the Canvas, so we let the Drawable draw itself with its
      // preferred dimensions
      when (shape) {
        is RectShape -> return {
              drawable.setBounds(0, 0, width, height)
              drawable.draw(it)
            }

        // TODO(T105148151): properly handle transformations
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

    inline fun getBitmapPaint(
        bitmap: Bitmap,
        shaderTransformation: Matrix? = null,
        paint: Paint? = null
    ): Paint {
      return (reset(paint) ?: Paint(Paint.ANTI_ALIAS_FLAG)).apply {
        shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(shaderTransformation)
      }
    }

    inline fun getDrawablePaint(drawable: Drawable, width: Int, height: Int, paint: Paint): Paint {
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      drawable.draw(Canvas(bitmap))
      return getBitmapPaint(bitmap, null, paint)
    }

    inline fun reset(paint: Paint?): Paint? = paint?.apply { reset() }
  }
}
