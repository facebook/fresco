/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder;

import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import java.util.Collections;
import java.util.List;

/**
 * Simple {@link ProgressiveJpegConfig} with predefined scans to decode and good-enough scan number.
 *
 * <p/> If no specific scans to decode are provided, every scan is allowed to be decoded.
 */
public class SimpleProgressiveJpegConfig implements ProgressiveJpegConfig {
  public interface DynamicValueConfig {
    List<Integer> getScansToDecode();
    int getGoodEnoughScanNumber();
  }

  private static class DefaultDynamicValueConfig implements DynamicValueConfig {
    public List<Integer> getScansToDecode() {
      return Collections.EMPTY_LIST;
    }

    public int getGoodEnoughScanNumber() {
      return 0;
    }
  }

  private final DynamicValueConfig mDynamicValueConfig;

  public SimpleProgressiveJpegConfig() {
    this (new DefaultDynamicValueConfig());
  }



  public SimpleProgressiveJpegConfig(DynamicValueConfig dynamicValueConfig) {
    mDynamicValueConfig = Preconditions.checkNotNull(dynamicValueConfig);
  }

  @Override
  public int getNextScanNumberToDecode(int scanNumber) {
    final List<Integer> scansToDecode = mDynamicValueConfig.getScansToDecode();
    if (scansToDecode == null || scansToDecode.isEmpty()) {
      return scanNumber + 1;
    }

    for (int i = 0; i < scansToDecode.size(); i++) {
      if (scansToDecode.get(i) > scanNumber) {
        return scansToDecode.get(i);
      }
    }
    return Integer.MAX_VALUE;
  }

  @Override
  public QualityInfo getQualityInfo(int scanNumber) {
    return ImmutableQualityInfo.of(
        scanNumber,
        /* isOfGoodEnoughQuality */ scanNumber >= mDynamicValueConfig.getGoodEnoughScanNumber(),
        /* isOfFullQuality */ false);
  }
}
