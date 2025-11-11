/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder;

import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Collections;
import java.util.List;

/**
 * Simple {@link ProgressiveJpegConfig} with predefined scans to decode and good-enough scan number.
 *
 * <p>If no specific scans to decode are provided, every scan is allowed to be decoded.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class SimpleProgressiveJpegConfig implements ProgressiveJpegConfig {
  public interface DynamicValueConfig {

    List<Integer> getScansToDecode(ImageRequest imageRequest);

    int getGoodEnoughScanNumber(ImageRequest imageRequest);
  }

  private static class DefaultDynamicValueConfig implements DynamicValueConfig {
    @Override
    public List<Integer> getScansToDecode(ImageRequest imageRequest) {
      return Collections.EMPTY_LIST;
    }

    @Override
    public int getGoodEnoughScanNumber(ImageRequest imageRequest) {
      return 0;
    }
  }

  private final DynamicValueConfig mDynamicValueConfig;

  public SimpleProgressiveJpegConfig() {
    this(new DefaultDynamicValueConfig());
  }

  public SimpleProgressiveJpegConfig(DynamicValueConfig dynamicValueConfig) {
    mDynamicValueConfig = Preconditions.checkNotNull(dynamicValueConfig);
  }

  @Override
  public boolean decodeProgressively(ImageRequest imageRequest) {
    return true;
  }

  @Override
  public int getNextScanNumberToDecode(ImageRequest imageRequest, int scanNumber) {
    final List<Integer> scansToDecode = mDynamicValueConfig.getScansToDecode(imageRequest);
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
  public QualityInfo getQualityInfo(ImageRequest imageRequest, int scanNumber) {
    return ImmutableQualityInfo.of(
        scanNumber,
        /* isOfGoodEnoughQuality */ scanNumber
            >= mDynamicValueConfig.getGoodEnoughScanNumber(imageRequest),
        /* isOfFullQuality */ false);
  }
}
