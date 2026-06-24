/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

import java.io.IOException
import java.io.InputStream

/**
 * Reads the encoded pixel dimensions of an image whose format Android's `BitmapFactory` cannot
 * parse (for example a custom container). Implementations are registered per
 * [ ImageFormat][com.facebook.imageformat.ImageFormat] with [EncodedImageDimensionsDecoderRegistry]
 * and consulted by [EncodedImage.parseMetaData] before the generic `BitmapFactory` fallback, so
 * that [EncodedImage.getWidth]/[EncodedImage.getHeight] — and everything derived from them,
 * including image-perf logging — are populated for such formats.
 */
fun interface EncodedImageDimensionsDecoder {
  /**
   * Returns the `(width, height)` read from the encoded header of [inputStream], or `null` if the
   * stream does not hold a valid image of this format. The caller owns [inputStream] and closes it.
   *
   * Declared `@Throws(IOException)` so Java callers can handle a read failure on a truncated/broken
   * stream; [EncodedImage.parseMetaData] catches it and falls back to unknown dimensions.
   */
  @Throws(IOException::class) fun decodeDimensions(inputStream: InputStream): Pair<Int, Int>?
}
