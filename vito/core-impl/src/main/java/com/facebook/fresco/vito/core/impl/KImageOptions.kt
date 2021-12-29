/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.renderer.CanvasTransformation
import com.facebook.fresco.vito.renderer.ColorIntImageDataModel
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.fresco.vito.renderer.ImageDataModel

// Models
fun ImageOptions.createPlaceholderModel(resources: Resources): ImageDataModel? =
    toModel(createPlaceholderDrawable(resources))

fun ImageOptions.createOverlayModel(resources: Resources): ImageDataModel? =
    toModel(createOverlayDrawable(resources))

fun ImageOptions.createErrorModel(resources: Resources): ImageDataModel? =
    toModel(createErrorDrawable(resources))

// Drawables
fun ImageOptions.createPlaceholderDrawable(resources: Resources): Drawable? =
    create(resources, placeholderDrawable, placeholderRes)

fun ImageOptions.createProgressDrawable(resources: Resources): Drawable? =
    create(resources, progressDrawable, progressRes)

fun ImageOptions.createErrorDrawable(resources: Resources): Drawable? =
    create(resources, errorDrawable, errorRes)

fun ImageOptions.createOverlayDrawable(resources: Resources): Drawable? =
    create(resources, overlayDrawable, overlayRes)

// Transformations
fun ImageOptions.createPlaceholderCanvasTransformation(): CanvasTransformation? =
    placeholderScaleType?.getCanvasTransformation(placeholderFocusPoint)

fun ImageOptions.createActualImageCanvasTransformation(): CanvasTransformation =
    actualImageScaleType.getCanvasTransformation(actualImageFocusPoint)

fun ImageOptions.createProgressCanvasTransformation(): CanvasTransformation? =
    progressScaleType?.getCanvasTransformation(null)

fun ImageOptions.createErrorCanvasTransformation(): CanvasTransformation? =
    errorScaleType?.getCanvasTransformation(errorFocusPoint)

private fun create(resources: Resources, drawable: Drawable?, drawableRes: Int): Drawable? {
  return drawable ?: if (drawableRes != 0) resources.getDrawable(drawableRes) else null
}

private fun toModel(drawable: Drawable?): ImageDataModel? {
  return when (drawable) {
    null -> null // Empty image
    // Rendering the color directly is much faster, especially if rounding etc. is needed
    is ColorDrawable -> ColorIntImageDataModel(drawable.color)
    else -> DrawableImageDataModel(drawable)
  }
}

fun ScalingUtils.ScaleType.getCanvasTransformation(
    focusPoint: PointF? = null
): CanvasTransformation {
  return object : CanvasTransformation {
    override fun calculateTransformation(
        outTransform: Matrix,
        parentBounds: Rect,
        childWidth: Int,
        childHeight: Int
    ): Matrix {
      getTransform(
          outTransform,
          parentBounds,
          childWidth,
          childHeight,
          focusPoint?.x ?: 0.5f,
          focusPoint?.y ?: 0.5f)
      return outTransform
    }
  }
}
