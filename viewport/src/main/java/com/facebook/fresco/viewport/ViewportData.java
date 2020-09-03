/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.viewport;

import javax.annotation.Nullable;

public class ViewportData {

  private final int mWidth;
  private final int mHeight;
  private final @Nullable HasTransform mTransform;

  public ViewportData(int width, int height, @Nullable HasTransform transform) {
    mWidth = width;
    mHeight = height;
    mTransform = transform;
  }

  public int getWidth() {
    return mWidth;
  }

  public int getHeight() {
    return mHeight;
  }

  @Nullable
  public HasTransform getTransform() {
    return mTransform;
  }
}
