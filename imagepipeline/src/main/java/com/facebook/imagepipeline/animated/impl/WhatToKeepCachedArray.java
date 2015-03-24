/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import javax.annotation.concurrent.NotThreadSafe;

import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;

/**
 * A simple data structure for tracking what to keep cached.
 */
@NotThreadSafe
class WhatToKeepCachedArray {

  private final boolean[] mData;

  WhatToKeepCachedArray(int size) {
    mData = new boolean[size];
  }

  boolean get(int index) {
    return mData[index];
  }

  void setAll(boolean value) {
    for (int i = 0; i < mData.length; i++) {
      mData[i] = value;
    }
  }

  void removeOutsideRange(int start, int end) {
    for (int i = 0; i < mData.length; i++) {
      if (AnimatedDrawableUtil.isOutsideRange(start, end, i)) {
        mData[i] = false;
      }
    }
  }

  void set(int index, boolean value) {
    mData[index] = value;
  }
}
