/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.decoder;

import java.util.List;

import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;

/**
 * Simple {@link ProgressiveJpegConfig} with predefined scans to decode and good-enough scan number.
 */
public class SimpleProgressiveJpegConfig implements ProgressiveJpegConfig {
  private final List<Integer> mScansToDecode;
  private final int mGoodEnoughScanNumber;

  public SimpleProgressiveJpegConfig(
      List<Integer> scansToDecode,
      int goodEnoughScanNumber) {
    mScansToDecode = scansToDecode;
    mGoodEnoughScanNumber = goodEnoughScanNumber;
  }

  @Override
  public int getNextScanNumberToDecode(int scanNumber) {
    for (int i = 0; i < mScansToDecode.size(); i++) {
      if (mScansToDecode.get(i) > scanNumber) {
        return mScansToDecode.get(i);
      }
    }
    return Integer.MAX_VALUE;
  }

  @Override
  public QualityInfo getQualityInfo(int scanNumber) {
    return ImmutableQualityInfo.of(
        scanNumber,
        /* isOfGoodEnoughQuality */ scanNumber >= mGoodEnoughScanNumber,
        /* isOfFullQuality */ false);
  }
}
