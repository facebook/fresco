/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import com.google.common.base.Objects;

/**
 * Surprise! A pair of integers
 */
public class IntPair {
  public final int a;
  public final int b;

  public IntPair(int a, int b) {
    this.a = a;
    this.b = b;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(a, b);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof IntPair) {
      IntPair that = (IntPair)other;
      return this.a == that.a && this.b == that.b;
    }
    return false;
  }

  @Override
  public String toString() {
    return "[" + a + ", " + b + "]";
  }
}
