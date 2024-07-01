/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import androidx.annotation.IntDef

/** Status used by [ImageTranscodeResult] to supply additional information. */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    TranscodeStatus.TRANSCODING_SUCCESS,
    TranscodeStatus.TRANSCODING_NO_RESIZING,
    TranscodeStatus.TRANSCODING_ERROR,
)
annotation class TranscodeStatus {
  companion object {
    /** Status flag to show that the image was transcoded successfully. */
    /** Status flag to show that the image was transcoded successfully. */
    const val TRANSCODING_SUCCESS: Int = 0

    /** Status flag to show that the input image transcoded successfully without resizing. */
    /** Status flag to show that the input image transcoded successfully without resizing. */
    const val TRANSCODING_NO_RESIZING: Int = 1

    /** Status flag to show that an error occurred while transcoding the image. */
    /** Status flag to show that an error occurred while transcoding the image. */
    const val TRANSCODING_ERROR: Int = 2
  }
}
