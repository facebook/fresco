/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder;

import com.facebook.imagepipeline.image.QualityInfo;

/** Progressive JPEG config. */
public interface ProgressiveJpegConfig {

  /** Shortcut for checking if we should attempt to decode progressively. */
  boolean decodeProgressively();

  /** Gets the next scan-number that should be decoded after the given scan-number. */
  int getNextScanNumberToDecode(int scanNumber);

  /** Gets the quality information for the given scan-number. */
  QualityInfo getQualityInfo(int scanNumber);
}
