/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import com.facebook.imageformat.ImageFormat
import java.util.Locale

/** Result returned by an [ImageTranscoder] when transcoding an image. */
class ImageTranscodeResult(
    @field:TranscodeStatus @get:TranscodeStatus @param:TranscodeStatus val transcodeStatus: Int,
    val outputFormat: ImageFormat,
) {
  override fun toString(): String = String.format(null as Locale?, "Status: %d", transcodeStatus)
}
