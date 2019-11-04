/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common;

public class DimensionsInfo {

  private final int mViewportWidth;
  private final int mViewportHeight;

  private final int mEncodedImageWidth;
  private final int mEncodedImageHeight;

  private final int mDecodedImageWidth;
  private final int mDecodedImageHeight;

  public DimensionsInfo(
      int viewportWidth,
      int viewportHeight,
      int encodedImageWidth,
      int encodedImageHeight,
      int decodedImageWidth,
      int decodedImageHeight) {
    mViewportWidth = viewportWidth;
    mViewportHeight = viewportHeight;
    mEncodedImageWidth = encodedImageWidth;
    mEncodedImageHeight = encodedImageHeight;
    mDecodedImageWidth = decodedImageWidth;
    mDecodedImageHeight = decodedImageHeight;
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
        + '}';
  }
}
