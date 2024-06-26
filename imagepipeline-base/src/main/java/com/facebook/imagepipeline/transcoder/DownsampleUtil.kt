/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import androidx.annotation.VisibleForTesting
import com.facebook.common.logging.FLog
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.EncodedImage
import kotlin.math.pow

object DownsampleUtil {

  const val DEFAULT_SAMPLE_SIZE = 1
  private const val INTERVAL_ROUNDING = 1.0f / 3

  /**
   * Get the factor between the dimensions of the encodedImage (actual image) and the ones of the
   * imageRequest (requested size).
   *
   * @param rotationOptions the rotations options of the request
   * @param resizeOptions the resize options of the request
   * @param encodedImage the encoded image with the actual dimensions
   * @param maxBitmapDimension the maximum supported bitmap dimension (in pixels) when not specified
   *   in the encoded image resizeOptions.
   * @return
   */
  @JvmStatic
  fun determineSampleSize(
      rotationOptions: RotationOptions,
      resizeOptions: ResizeOptions?,
      encodedImage: EncodedImage,
      maxBitmapDimension: Int
  ): Int {
    if (!EncodedImage.isMetaDataAvailable(encodedImage)) {
      return DEFAULT_SAMPLE_SIZE
    }
    val ratio = determineDownsampleRatio(rotationOptions, resizeOptions, encodedImage)
    var sampleSize: Int =
        if (encodedImage.imageFormat === DefaultImageFormats.JPEG) {
          ratioToSampleSizeJPEG(ratio)
        } else {
          ratioToSampleSize(ratio)
        }

    // Check the case when the dimension of the downsampled image is still larger than the max
    // possible dimension for an image.
    val maxDimension = Math.max(encodedImage.height, encodedImage.width)
    val computedMaxBitmapSize = resizeOptions?.maxBitmapDimension ?: maxBitmapDimension.toFloat()
    while (maxDimension / sampleSize > computedMaxBitmapSize) {
      if (encodedImage.imageFormat === DefaultImageFormats.JPEG) {
        sampleSize *= 2
      } else {
        sampleSize++
      }
    }
    return sampleSize
  }

  @JvmStatic
  fun determineSampleSizeJPEG(
      encodedImage: EncodedImage,
      pixelSize: Int,
      maxBitmapSizeInBytes: Int
  ): Int {
    var sampleSize = encodedImage.sampleSize
    val base = encodedImage.width * encodedImage.height * pixelSize
    while (base / sampleSize / sampleSize > maxBitmapSizeInBytes) {
      sampleSize *= 2
    }
    return sampleSize
  }

  @JvmStatic
  @VisibleForTesting
  fun determineDownsampleRatio(
      rotationOptions: RotationOptions,
      resizeOptions: ResizeOptions?,
      encodedImage: EncodedImage
  ): Float {
    check(EncodedImage.isMetaDataAvailable(encodedImage))
    if (resizeOptions == null ||
        resizeOptions.height <= 0 ||
        resizeOptions.width <= 0 ||
        encodedImage.width == 0 ||
        encodedImage.height == 0) {
      return 1.0f
    }
    val rotationAngle = getRotationAngle(rotationOptions, encodedImage)
    val swapDimensions = rotationAngle == 90 || rotationAngle == 270
    val widthAfterRotation = if (swapDimensions) encodedImage.height else encodedImage.width
    val heightAfterRotation = if (swapDimensions) encodedImage.width else encodedImage.height
    val widthRatio = resizeOptions.width.toFloat() / widthAfterRotation
    val heightRatio = resizeOptions.height.toFloat() / heightAfterRotation
    val ratio = widthRatio.coerceAtLeast(heightRatio)
    FLog.v(
        "DownsampleUtil",
        "Downsample - Specified size: %dx%d, image size: %dx%d " +
            "ratio: %.1f x %.1f, ratio: %.3f",
        resizeOptions.width,
        resizeOptions.height,
        widthAfterRotation,
        heightAfterRotation,
        widthRatio,
        heightRatio,
        ratio)
    return ratio
  }

  @JvmStatic
  @VisibleForTesting
  fun ratioToSampleSize(ratio: Float): Int {
    if (ratio > 0.5f + 0.5f * INTERVAL_ROUNDING) {
      return 1 // should have resized
    }
    var sampleSize = 2
    while (true) {
      val intervalLength = 1.0 / (sampleSize.toDouble().pow(2.0) - sampleSize)
      val compare = 1.0 / sampleSize + intervalLength * INTERVAL_ROUNDING
      if (compare <= ratio) {
        return sampleSize - 1
      }
      sampleSize++
    }
  }

  @JvmStatic
  @VisibleForTesting
  fun ratioToSampleSizeJPEG(ratio: Float): Int {
    if (ratio > 0.5f + 0.5f * INTERVAL_ROUNDING) {
      return 1 // should have resized
    }
    var sampleSize = 2
    while (true) {
      val intervalLength = 1.0 / (2 * sampleSize)
      val compare = 1.0 / (2 * sampleSize) + intervalLength * INTERVAL_ROUNDING
      if (compare <= ratio) {
        return sampleSize
      }
      sampleSize *= 2
    }
  }

  private fun getRotationAngle(rotationOptions: RotationOptions, encodedImage: EncodedImage): Int {
    if (!rotationOptions.useImageMetadata()) {
      return 0
    }
    val rotationAngle = encodedImage.rotationAngle
    check(rotationAngle == 0 || rotationAngle == 90 || rotationAngle == 180 || rotationAngle == 270)
    return rotationAngle
  }

  @JvmStatic
  @VisibleForTesting
  fun roundToPowerOfTwo(sampleSize: Int): Int {
    var compare = 1
    while (true) {
      if (compare >= sampleSize) {
        return compare
      }
      compare *= 2
    }
  }
}
