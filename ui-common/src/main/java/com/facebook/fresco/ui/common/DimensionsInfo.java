/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common;

public class DimensionsInfo {

  private int mViewportWidth;
  private int mViewportHeight;

  public DimensionsInfo(int viewportWidth, int viewportHeight) {
    mViewportWidth = viewportWidth;
    mViewportHeight = viewportHeight;
  }

  public int getViewportWidth() {
    return mViewportWidth;
  }

  public int getViewportHeight() {
    return mViewportHeight;
  }

  @Override
  public String toString() {
    return "DimensionsInfo{"
        + "mViewportWidth="
        + mViewportWidth
        + ", mViewportHeight="
        + mViewportHeight
        + '}';
  }
}
