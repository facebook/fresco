/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

class PlatformDecoderOptions(
    val avoidPoolGet: Boolean = false,
    val avoidPoolRelease: Boolean = false,
    val enableDecodeDimensionValidation: Boolean = false,
    val catchNativeDecoderErrors: Boolean = false,
    val errorReporter: DecoderErrorReporter? = null,
    val useEfficientDecoder: Boolean = false,
    val useBitmapFactoryDecoder: Boolean = false,
    val decodeImmutableBitmaps: Boolean = false,
    // BitmapFactory.Options.inDither was deprecated in API 24 and is ignored by the platform on
    // API 24+, so toggling this flag has no effect on devices running Android N or newer. Kept
    // for completeness and for the rare pre-N callers; do not attribute experiment wins to it.
    val enableDither: Boolean = true,
    // When false, the decoder does not force inPreferredColorSpace to sRGB (an explicit colorSpace
    // argument is still honored). Forcing sRGB adds a color-space transform for non-sRGB sources.
    // Defaults to true to preserve existing behavior.
    val forceSrgbColorSpace: Boolean = true,
    // When true, DefaultRawBitmapDecoder decodes directly from the byte array via
    // BitmapFactory.decodeByteArray (avoiding ByteArrayInputStream + decodeStream overhead) on the
    // pool-free path. Only applies when no BitmapPool is configured. Defaults to false.
    val preferByteArrayDecode: Boolean = false,
) {
  fun interface DecoderErrorReporter {
    fun reportError(category: String, message: String, cause: Throwable?)
  }
}
