/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.image;

/**
 * Implementation of {@link QualityInfo}
 */
public class ImmutableQualityInfo implements QualityInfo {

  public static final QualityInfo FULL_QUALITY = of(Integer.MAX_VALUE, true, true);

  int mQuality;
  boolean mIsOfGoodEnoughQuality;
  boolean mIsOfFullQuality;

  private ImmutableQualityInfo(
      int quality,
      boolean isOfGoodEnoughQuality,
      boolean isOfFullQuality) {
    mQuality = quality;
    mIsOfGoodEnoughQuality = isOfGoodEnoughQuality;
    mIsOfFullQuality = isOfFullQuality;
  }

  @Override
  public int getQuality() {
    return mQuality;
  }

  @Override
  public boolean isOfGoodEnoughQuality() {
    return mIsOfGoodEnoughQuality;
  }

  @Override
  public boolean isOfFullQuality() {
    return mIsOfFullQuality;
  }

  @Override
  public int hashCode() {
    return mQuality ^ (mIsOfGoodEnoughQuality ? 0x400000 : 0) ^ (mIsOfFullQuality ? 0x800000 : 0);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof ImmutableQualityInfo)) {
      return false;
    }
    ImmutableQualityInfo that = (ImmutableQualityInfo) other;
    return mQuality == that.mQuality &&
        mIsOfGoodEnoughQuality == that.mIsOfGoodEnoughQuality &&
        mIsOfFullQuality == that.mIsOfFullQuality;
  }

  public static QualityInfo of(
      int quality,
      boolean isOfGoodEnoughQuality,
      boolean isOfFullQuality) {
    return new ImmutableQualityInfo(quality, isOfGoodEnoughQuality, isOfFullQuality);
  }
}
