/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import com.facebook.imageformat.ImageFormat

interface ImageTranscoderFactory {

  /**
   * Creates an [ImageTranscoder] that enables or disables resizing depending on
   * `isResizingEnabled`. It can return null if the [ImageFormat] is not supported by this
   * [ImageTranscoder].
   *
   * Note that if JPEG images are not supported, we will fallback to our native [ImageTranscoder]
   * implementation.
   *
   * @param imageFormat the [ImageFormat] of the input images.
   * @param isResizingEnabled true if resizing is allowed.
   * @return The [ImageTranscoder] or null if the image format is not supported.
   */
  fun createImageTranscoder(imageFormat: ImageFormat, isResizingEnabled: Boolean): ImageTranscoder?
}
