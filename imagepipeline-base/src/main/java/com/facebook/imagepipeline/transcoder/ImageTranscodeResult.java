/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder;

import com.facebook.infer.annotation.Nullsafe;
import java.util.Locale;

/** Result returned by an {@link ImageTranscoder} when transcoding an image. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class ImageTranscodeResult {

  private @TranscodeStatus final int mTranscodeStatus;

  public ImageTranscodeResult(@TranscodeStatus int transcodeStatus) {
    mTranscodeStatus = transcodeStatus;
  }

  @TranscodeStatus
  public int getTranscodeStatus() {
    return mTranscodeStatus;
  }

  @Override
  public String toString() {
    return String.format((Locale) null, "Status: %d", mTranscodeStatus);
  }
}
