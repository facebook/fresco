/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

public class BitmapCounterConfig {

  public static final int DEFAULT_MAX_BITMAP_COUNT = 384;

  private int mMaxBitmapCount = DEFAULT_MAX_BITMAP_COUNT;

  public BitmapCounterConfig(Builder builder) {
    mMaxBitmapCount = builder.getMaxBitmapCount();
  }

  public int getMaxBitmapCount() {
    return mMaxBitmapCount;
  }

  public void setMaxBitmapCount(int maxBitmapCount) {
    mMaxBitmapCount = maxBitmapCount;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Builder() {}

    private int mMaxBitmapCount = DEFAULT_MAX_BITMAP_COUNT;

    public Builder setMaxBitmapCount(int maxBitmapCount) {
      mMaxBitmapCount = maxBitmapCount;
      return this;
    }

    public int getMaxBitmapCount() {
      return mMaxBitmapCount;
    }

    public BitmapCounterConfig build() {
      return new BitmapCounterConfig(this);
    }
  }
}
