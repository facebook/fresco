/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import com.facebook.common.internal.DoNotStrip
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.transcoder.ImageTranscoder
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory

@DoNotStrip
class NativeJpegTranscoderFactory
@DoNotStrip
constructor(
    private val maxBitmapSize: Int,
    private val useDownSamplingRatio: Boolean,
    private val ensureTranscoderLibraryLoaded: Boolean,
) : ImageTranscoderFactory {
  @DoNotStrip
  override fun createImageTranscoder(
      imageFormat: ImageFormat,
      isResizingEnabled: Boolean,
  ): ImageTranscoder? {
    if (imageFormat !== DefaultImageFormats.JPEG) {
      return null
    }
    return NativeJpegTranscoder(
        isResizingEnabled,
        maxBitmapSize,
        useDownSamplingRatio,
        ensureTranscoderLibraryLoaded,
    )
  }
}
