/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import androidx.annotation.NonNull;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImageInfoImpl implements ImageInfo {
  private final int width;
  private final int height;
  private final int sizeInBytes;
  private final QualityInfo qualityInfo;
  private final Map<String, Object> extras;

  public ImageInfoImpl(
      int width, int height, int sizeInBytes, QualityInfo qualityInfo, Map<String, Object> extras) {
    this.width = width;
    this.height = height;
    this.sizeInBytes = sizeInBytes;
    this.qualityInfo = qualityInfo;
    this.extras = extras;
  }

  @NonNull
  @Override
  public Map<String, Object> getExtras() {
    return extras;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public int getSizeInBytes() {
    return sizeInBytes;
  }

  @Override
  public QualityInfo getQualityInfo() {
    return qualityInfo;
  }
}
