/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.Objects;

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
