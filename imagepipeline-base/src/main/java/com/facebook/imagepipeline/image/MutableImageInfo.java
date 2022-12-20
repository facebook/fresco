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
public class MutableImageInfo implements ImageInfo {

  public MutableImageInfo(
      int width, int height, QualityInfo qualityInfo, Map<String, Object> extras) {
    this.width = width;
    this.height = height;
    this.qualityInfo = qualityInfo;
    this.extras = extras;
  }

  int width;
  int height;
  QualityInfo qualityInfo;
  Map<String, Object> extras;

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
  public QualityInfo getQualityInfo() {
    return qualityInfo;
  }
}
