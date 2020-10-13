/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public class DimensionsInfo {

  private final int mViewportWidth;
  private final int mViewportHeight;

  private final int mEncodedImageWidth;
  private final int mEncodedImageHeight;

  private final int mDecodedImageWidth;
  private final int mDecodedImageHeight;
  private final String mScaleType;

  public DimensionsInfo(
      int viewportWidth,
      int viewportHeight,
      int encodedImageWidth,
      int encodedImageHeight,
      int decodedImageWidth,
      int decodedImageHeight,
      String scaleType) {
    mViewportWidth = viewportWidth;
    mViewportHeight = viewportHeight;
    mEncodedImageWidth = encodedImageWidth;
    mEncodedImageHeight = encodedImageHeight;
    mDecodedImageWidth = decodedImageWidth;
    mDecodedImageHeight = decodedImageHeight;
    mScaleType = scaleType;
  }

  public int getViewportWidth() {
    return mViewportWidth;
  }

  public int getViewportHeight() {
    return mViewportHeight;
  }

  public int getEncodedImageWidth() {
    return mEncodedImageWidth;
  }

  public int getEncodedImageHeight() {
    return mEncodedImageHeight;
  }

  public int getDecodedImageWidth() {
    return mDecodedImageWidth;
  }

  public int getDecodedImageHeight() {
    return mDecodedImageHeight;
  }

  public String getScaleType() {
    return mScaleType;
  }

  @Override
  public String toString() {
    return "DimensionsInfo{"
        + "mViewportWidth="
        + mViewportWidth
        + ", mViewportHeight="
        + mViewportHeight
        + ", mEncodedImageWidth="
        + mEncodedImageWidth
        + ", mEncodedImageHeight="
        + mEncodedImageHeight
        + ", mDecodedImageWidth="
        + mDecodedImageWidth
        + ", mDecodedImageHeight="
        + mDecodedImageHeight
        + ", mScaleType='"
        + mScaleType
        + '\''
        + '}';
  }
}
