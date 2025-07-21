/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory

import com.facebook.animated.giflite.decoder.GifMetadataDecoder
import com.facebook.imagepipeline.image.EncodedImage

private const val MAX_GIF_TOTAL_PIXELS = 100_000_000

internal object GifImageValidator : AnimatedImageValidator {

  override fun validateImage(encodedImage: EncodedImage): ValidationResult {
    val inputStream =
        encodedImage.inputStream ?: return ValidationResult.Failure("No input stream available")

    try {
      inputStream.use { stream ->
        val decoder = GifMetadataDecoder.create(stream, null)

        val width = decoder.screenWidth
        val height = decoder.screenHeight
        if (width <= 0 || height <= 0) {
          return ValidationResult.Failure("GIF invalid logical screen size: $width x $height")
        }

        val totalPixels = width * height * decoder.frameCount
        if (totalPixels > MAX_GIF_TOTAL_PIXELS) {
          return ValidationResult.Failure(
              "GIF too large: $width x $height x ${decoder.frameCount} frames = $totalPixels pixels")
        }
      }
      return ValidationResult.Success
    } catch (e: Exception) {
      return ValidationResult.Failure("Error parsing GIF: ${e.message}")
    }
  }
}
