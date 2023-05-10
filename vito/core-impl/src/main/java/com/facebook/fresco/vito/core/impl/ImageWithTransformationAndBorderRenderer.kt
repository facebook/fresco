/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.fresco.vito.renderer.ImageRenderer
import com.facebook.fresco.vito.renderer.RenderCommand
import com.facebook.fresco.vito.renderer.Shape

class ImageWithTransformationAndBorderRenderer {
  companion object {
    fun createRenderCommand(
        model: ImageDataModel,
        roundingOptions: RoundingOptions?,
        borderOptions: BorderOptions?,
        canvasTransform: Matrix?,
        bounds: Rect,
        paint: Paint,
        alpha: Int = 255
    ): RenderCommand {
      val borderPadding: Float = borderOptions?.padding ?: 0f
      val borderWidth: Float = borderOptions?.width ?: 0f
      val hasBorders: Boolean = borderWidth > 0f
      val scaleDownInsideBorders = borderOptions?.scaleDownInsideBorders ?: false
      val layerBounds = RectF(bounds)
      val cornerRadiusAdjustment = borderWidth + borderPadding

      val imageTransform: Matrix?
      val imageShape: Shape
      if (hasBorders && (scaleDownInsideBorders || borderPadding != 0f)) {
        val insideBorderBounds =
            if (scaleDownInsideBorders) {
              RectF(layerBounds).apply { inset(cornerRadiusAdjustment, cornerRadiusAdjustment) }
            } else {
              RectF(layerBounds).apply { inset(borderWidth, borderWidth) }
            }
        val insideBorderTransform =
            Matrix().apply {
              setRectToRect(layerBounds, insideBorderBounds, Matrix.ScaleToFit.FILL)
            }
        imageTransform = Matrix(canvasTransform).apply { postConcat(insideBorderTransform) }
        imageShape =
            ShapeCalculator.getShape(insideBorderBounds, roundingOptions, -cornerRadiusAdjustment)
      } else {
        imageTransform = canvasTransform
        imageShape = ShapeCalculator.getShape(layerBounds, roundingOptions, -cornerRadiusAdjustment)
      }

      val imageRenderCommand =
          ImageRenderer.createImageDataModelRenderCommand(model, imageShape, paint, imageTransform)

      val imageClipRect =
          if (model.width > 0 && model.height > 0) {
            // Prevent repeated pixels from CLAMP shader tile mode by clipping the rect to the
            // transformed
            // image dimensions
            // We could optimize this to only clip if we actually would have repeated pixels.
            val imageRect = RectF(0f, 0f, model.width.toFloat(), model.height.toFloat())
            imageTransform?.mapRect(imageRect)
            imageRect
          } else {
            null
          }

      val borderShape =
          if (hasBorders) {
            val halfBorder = borderWidth / 2
            val borderBounds = RectF(layerBounds).apply { inset(halfBorder, halfBorder) }
            val borderShape = ShapeCalculator.getShape(borderBounds, roundingOptions, -halfBorder)
            borderShape
          } else {
            null
          }

      return { canvas ->
        val saveCount = canvas.save()
        canvas.clipRect(bounds)
        if (imageClipRect != null) {
          canvas.clipRect(imageClipRect)
        }
        imageRenderCommand(canvas)
        canvas.restoreToCount(saveCount)

        if (borderShape != null) {
          borderOptions?.apply { BorderRenderer.renderBorder(canvas, this, borderShape, alpha) }
        }
      }
    }
  }
}
