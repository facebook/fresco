/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Rect
import java.io.InputStream

/**
 * A clean byte-array-to-Bitmap decode interface for callers that already have raw compressed image
 * bytes. Unlike [PlatformDecoder], this does not require [EncodedImage] wrapping or
 * [CloseableReference] management.
 */
interface RawBitmapDecoder {

  /**
   * Decodes a compressed image from a byte array into a [android.graphics.Bitmap].
   *
   * @param data the compressed image bytes
   * @param offset start offset in the array
   * @param length number of valid bytes starting from [offset]
   * @param bitmapConfig the [android.graphics.Bitmap.Config] for the decoded bitmap (e.g.
   *   ARGB_8888)
   * @param sampleSize downsampling factor. Subsampling behavior is format-dependent (see
   *   BitmapFactory.Options.inSampleSize)
   * @param colorSpace the preferred color space for decoding, or null for sRGB default
   * @return the decoded bitmap, or null if decoding failed
   * @throws java.io.IOException if the image data cannot be decoded
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  fun decode(
      data: ByteArray,
      offset: Int,
      length: Int,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace? = null,
  ): Bitmap?

  fun decode(
      stream: InputStream,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace? = null,
  ): Bitmap? {
    val options = BitmapFactory.Options()
    options.inSampleSize = sampleSize
    options.inPreferredConfig = bitmapConfig
    return decode(stream, options, null, colorSpace)
  }

  fun decode(
      inputStream: InputStream,
      options: BitmapFactory.Options,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    return decode(
        inputStream,
        options.inPreferredConfig ?: Bitmap.Config.ARGB_8888,
        options.inSampleSize,
        colorSpace,
    )
  }

  fun decode(
      decodeStream: InputStream,
      prePassStream: InputStream?,
      sampleSize: Int,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    return decode(decodeStream, bitmapConfig, sampleSize, colorSpace)
  }

  fun decodeJpeg(
      jpegStream: InputStream,
      prePassStream: InputStream?,
      sampleSize: Int,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
      encodedImageSize: Int,
      length: Int,
      isJpegComplete: Boolean,
  ): Bitmap? {
    return decode(jpegStream, bitmapConfig, sampleSize, colorSpace)
  }
}
