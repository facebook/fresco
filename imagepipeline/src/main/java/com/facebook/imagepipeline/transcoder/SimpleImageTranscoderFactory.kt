/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import com.facebook.imageformat.ImageFormat

/** Factory class to create an [SimpleImageTranscoder] */
class SimpleImageTranscoderFactory(private val maxBitmapSize: Int) : ImageTranscoderFactory {
  override fun createImageTranscoder(
      imageFormat: ImageFormat,
      isResizingEnabled: Boolean,
  ): ImageTranscoder = SimpleImageTranscoder(isResizingEnabled, maxBitmapSize)
}
