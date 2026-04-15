/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import com.facebook.common.logging.FLog
import com.facebook.imagepipeline.image.EncodedImage

/** Validation utilities for bitmap decoding. Checks input dimensions before decoding. */
object DecodeValidationUtils {

  private const val TAG = "DecodeValidationUtils"

  // Maximum image dimension (width or height) to decode. Images exceeding this
  // are rejected to guard against buggy vendor JPEG decoders that crash on large inputs.
  // Copied from DefaultDecoder.
  const val MAX_DECODE_DIMENSION = 32768

  /**
   * Validates encoded image dimensions against [MAX_DECODE_DIMENSION]. Returns true if dimensions
   * are valid, false if unknown or they should be rejected. Uses synchronized(encodedImage) to
   * prevent concurrent parseMetadataIfNeeded() writes.
   */
  fun validateDimensions(
      encodedImage: EncodedImage,
      platformDecoderOptions: PlatformDecoderOptions,
  ): Boolean {
    if (!platformDecoderOptions.enableDecodeDimensionValidation) return true
    val width: Int
    val height: Int
    synchronized(encodedImage) {
      width = encodedImage.width
      height = encodedImage.height
    }
    if (width == EncodedImage.UNKNOWN_WIDTH || height == EncodedImage.UNKNOWN_HEIGHT) {
      return false
    }
    if (
        width <= 0 || height <= 0 || width > MAX_DECODE_DIMENSION || height > MAX_DECODE_DIMENSION
    ) {
      val message = "Rejecting decode with invalid dimensions: ${width}x${height}"
      FLog.e(TAG, message)
      platformDecoderOptions.errorReporter?.reportError(
          "DECODE_DIMENSION_VALIDATION_ERROR",
          message,
          null,
      )
      return false
    }
    return true
  }

  /**
   * Validates that input data is non-null and has valid bounds. Returns true if valid, false if the
   * data should be rejected.
   */
  fun validateInput(data: ByteArray?, offset: Int, length: Int): Boolean {
    return data != null && length > 0 && data.size >= offset + length
  }

  /**
   * Validates that the given byte array contains a valid JPEG header (SOI marker 0xFF 0xD8) when
   * the data appears to be JPEG. Returns true if the data is valid (either valid JPEG, too short to
   * check, or a known non-JPEG format like PNG/WebP/GIF).
   */
  fun isValidImageHeader(data: ByteArray, offset: Int, length: Int): Boolean {
    if (length < 2) return true
    val firstByte = data[offset].toInt() and 0xFF
    val secondByte = data[offset + 1].toInt() and 0xFF
    // Valid JPEG SOI marker
    if (firstByte == 0xFF && secondByte == 0xD8) return true
    // Known non-JPEG formats (PNG, WebP/RIFF, GIF) — valid data, just not JPEG
    if (firstByte == 0x89 && secondByte == 0x50) return true // PNG
    if (firstByte == 0x52 && secondByte == 0x49) return true // WebP (RIFF)
    if (firstByte == 0x47 && secondByte == 0x49) return true // GIF
    return false
  }
}
