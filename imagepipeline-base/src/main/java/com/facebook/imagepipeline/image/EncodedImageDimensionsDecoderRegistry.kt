/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

import com.facebook.imageformat.ImageFormat
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide registry of per-format [EncodedImageDimensionsDecoder]s consulted by
 * [EncodedImage.parseMetaData]. This mirrors how custom
 * [ ImageFormat.FormatChecker][com.facebook.imageformat.ImageFormat.FormatChecker]s are registered
 * on [com.facebook.imageformat.ImageFormatChecker]: application code populates it once at
 * pipeline-configuration time and [EncodedImage] reads it. It is empty by default, so it is inert
 * for every format that does not register a decoder (the existing `BitmapFactory` path is
 * unchanged).
 */
object EncodedImageDimensionsDecoderRegistry {
  private val decoders = ConcurrentHashMap<ImageFormat, EncodedImageDimensionsDecoder>()

  @JvmStatic
  fun register(imageFormat: ImageFormat, decoder: EncodedImageDimensionsDecoder) {
    decoders[imageFormat] = decoder
  }

  @JvmStatic
  fun get(imageFormat: ImageFormat): EncodedImageDimensionsDecoder? = decoders[imageFormat]
}
