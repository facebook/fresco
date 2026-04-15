/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.graphics.Bitmap
import android.graphics.ColorSpace

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
}
