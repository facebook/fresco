/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.decoder;

import com.facebook.imagepipeline.image.QualityInfo;

/**
 * Progressive JPEG config.
 */
public interface ProgressiveJpegConfig {

  /**
   * Gets the next scan-number that should be decoded after the given scan-number.
   */
  public int getNextScanNumberToDecode(int scanNumber);

  /**
   * Gets the quality information for the given scan-number.
   */
  public QualityInfo getQualityInfo(int scanNumber);
}
