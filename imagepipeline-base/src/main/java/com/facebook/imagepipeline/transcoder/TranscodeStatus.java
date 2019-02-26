/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import static com.facebook.imagepipeline.transcoder.TranscodeStatus.TRANSCODING_ERROR;
import static com.facebook.imagepipeline.transcoder.TranscodeStatus.TRANSCODING_NO_RESIZING;
import static com.facebook.imagepipeline.transcoder.TranscodeStatus.TRANSCODING_SUCCESS;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

/** Status used by {@link ImageTranscodeResult} to supply additional information. */
@Retention(SOURCE)
@IntDef({
  TRANSCODING_SUCCESS,
  TRANSCODING_NO_RESIZING,
  TRANSCODING_ERROR,
})
public @interface TranscodeStatus {

  /** Status flag to show that the image was transcoded successfully. */
  int TRANSCODING_SUCCESS = 0;

  /** Status flag to show that the input image transcoded successfully without resizing. */
  int TRANSCODING_NO_RESIZING = 1;

  /** Status flag to show that an error occured while transcoding the image. */
  int TRANSCODING_ERROR = 2;
}
