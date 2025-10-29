/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.postprocessors

import android.graphics.Bitmap
import com.facebook.common.internal.Preconditions
import com.facebook.imagepipeline.filter.IterativeBoxBlurFilter
import com.facebook.imagepipeline.filter.RenderScriptBlurFilter
import com.facebook.imagepipeline.request.BasePostprocessor

/**
 * Custom postprocessor that downscales the image, applies blur, then upscales back to original
 * size. This technique improves performance and creates a stronger blur effect.
 */
class DownScaleBlurPostProcessor
/**
 * Creates an instance of [DownScaleBlurPostProcessor].
 *
 * @param blurRadius The radius of the blur in range 0 < radius <=
 *   [RenderScriptBlurFilter.BLUR_MAX_RADIUS].
 * @param iterations The number of iterations of the blurring algorithm (> 0).
 * @param targetWidth The width to which the image will be upscaled after blurring.
 * @param targetHeight The height to which the image will be upscaled after blurring.
 * @param scaleFactor The factor by which the image is downscaled before blurring (0 < scaleFactor
 *   <= 1.0).
 */
constructor(
    val blurRadius: Int,
    val targetWidth: Int,
    val targetHeight: Int,
    val iterations: Int = DEFAULT_ITERATIONS,
    val scaleFactor: Float = DEFAULT_SCALE_FACTOR,
    val ntscDampeningFactor: Float = 0f,
) : BasePostprocessor() {

  init {
    Preconditions.checkArgument(
        blurRadius > 0 && blurRadius <= RenderScriptBlurFilter.BLUR_MAX_RADIUS,
        "blurRadius must be > 0 and <= ${RenderScriptBlurFilter.BLUR_MAX_RADIUS}, but was $blurRadius",
    )
    Preconditions.checkArgument(iterations > 0, "iterations must be > 0, but was $iterations")
    Preconditions.checkArgument(
        scaleFactor > 0 && scaleFactor <= 1.0f,
        "scaleFactor must be > 0 and <= 1.0, but was $scaleFactor",
    )
    Preconditions.checkArgument(
        targetWidth > 0 && targetHeight > 0,
        "targetWidth and targetHeight must be > 0, but were $targetWidth x $targetHeight",
    )
  }

  override fun process(destBitmap: Bitmap, sourceBitmap: Bitmap) {
    // Calculate downscaled dimensions
    val downscaledWidth = (sourceBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
    val downscaledHeight = (sourceBitmap.height * scaleFactor).toInt().coerceAtLeast(1)

    // Create downscaled bitmap
    val downscaled =
        Bitmap.createScaledBitmap(sourceBitmap, downscaledWidth, downscaledHeight, true)

    try {
      // Apply blur to the downscaled bitmap
      IterativeBoxBlurFilter.boxBlurBitmapInPlace(
          downscaled,
          iterations,
          blurRadius,
          ntscDampeningFactor,
      )

      // Draw the blurred downscaled bitmap onto the destination bitmap (upscaling it)
      val canvas = android.graphics.Canvas(destBitmap)
      val paint = android.graphics.Paint()

      // Enables bilinear filtering when scaling or transforming the bitmap, which results in
      // smoother, less pixelated images during the upscaling process
      paint.isFilterBitmap = true

      canvas.drawBitmap(
          downscaled,
          null,
          android.graphics.Rect(0, 0, targetWidth, targetHeight),
          paint,
      )
    } finally {
      // Clean up the temporary downscaled bitmap
      downscaled.recycle()
    }
  }

  companion object {
    private const val DEFAULT_ITERATIONS = 3
    private const val DEFAULT_SCALE_FACTOR = 0.3f
  }
}
