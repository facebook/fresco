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
 * parse (for example a custom container) or parses slowly. Implementations are registered per
 * [ ImageFormat][com.facebook.imageformat.ImageFormat] with [EncodedImageDimensionsDecoderRegistry]
 * and consulted by [EncodedImage.parseMetaData] before its default path (the WebP size reader for
 * WebP, `BitmapFactory` otherwise), so that [EncodedImage.getWidth]/[EncodedImage.getHeight] — and
 * everything derived from them, including image-perf logging — are populated for such formats.
 */
fun interface EncodedImageDimensionsDecoder {
  /**
   * Returns the `(width, height)` read from the encoded header of [inputStream], or `null` to fall
   * back to the default path, for example because the stream does not hold a valid image of this
   * format. The caller owns [inputStream] and closes it.
   *
   * Declared `@Throws(IOException)` so Java callers can handle a read failure on a truncated/broken
   * stream; [EncodedImage.parseMetaData] catches it and falls back to the default path too.
   */
  @Throws(IOException::class) fun decodeDimensions(inputStream: InputStream): Pair<Int, Int>?
}
