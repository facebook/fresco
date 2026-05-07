/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.postprocessors

import android.graphics.Bitmap
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.internal.Preconditions
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.filter.IterativeBoxBlurFilter
import com.facebook.imagepipeline.filter.RenderScriptBlurFilter
import com.facebook.imagepipeline.request.BasePostprocessor
import java.util.Locale

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

  // Precompute a stable cache key so identical postprocess requests can reuse cached results.
  private val cacheKey: CacheKey =
      SimpleCacheKey(
          // Build a unique string incorporating all parameters that affect output.
          // Using Locale null ensures default formatting without locale-specific variations.
          String.format(
              null as Locale?,
              "DownScaleBlur;r%d;w%d;h%d;i%d;s%.2f;n%.2f",
              blurRadius, // blur radius
              targetWidth, // final upscaled width
              targetHeight, // final upscaled height
              iterations, // number of blur iterations
              scaleFactor, // downscale factor before blur
              ntscDampeningFactor, // optional NTSC dampening factor for luminance adjustment
          )
      )

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

  /**
   * Creates a destination bitmap at the target dimensions and processes the blur.
   *
   * This override ensures the output bitmap size matches targetWidth × targetHeight, regardless of
   * the source bitmap dimensions. Without this override, the pipeline creates a destination bitmap
   * at the source dimensions, causing the blurred content to only occupy a portion of the bitmap.
   */
  override fun process(
      sourceBitmap: Bitmap,
      bitmapFactory: PlatformBitmapFactory,
  ): CloseableReference<Bitmap> {
    // Create destination bitmap at TARGET dimensions (not source dimensions)
    val destBitmapRef =
        bitmapFactory.createBitmapInternal(
            targetWidth,
            targetHeight,
            sourceBitmap.config ?: Bitmap.Config.ARGB_8888,
        )

    try {
      // Call the blur processing logic
      process(destBitmapRef.get(), sourceBitmap)
      return CloseableReference.cloneOrNull(destBitmapRef)
          ?: throw IllegalStateException("Failed to clone destination bitmap reference")
    } finally {
      CloseableReference.closeSafely(destBitmapRef)
    }
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

      // Draw to fill the ENTIRE destination bitmap
      // This works correctly whether destBitmap is created by our override (targetWidth ×
      // targetHeight)
      // or by the base class (source dimensions)
      canvas.drawBitmap(
          downscaled,
          null,
          android.graphics.Rect(0, 0, destBitmap.width, destBitmap.height),
          paint,
      )
    } finally {
      // Clean up the temporary downscaled bitmap
      downscaled.recycle()
    }
  }

  // Return the precomputed cache key to the pipeline.
  override fun getPostprocessorCacheKey(): CacheKey = cacheKey

  companion object {
    private const val DEFAULT_ITERATIONS = 3
    private const val DEFAULT_SCALE_FACTOR = 0.3f
  }
}
