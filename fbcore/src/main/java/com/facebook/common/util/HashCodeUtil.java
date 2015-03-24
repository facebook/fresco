/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.util;

import javax.annotation.Nullable;

/**
 * Provides implementation of hashCode for compound objects. Implementation provided by
 * this class gives the same results as Objects.hashCode, but does not create array consisting of
 * all components
 */
public class HashCodeUtil {

  /**
   * Hash code is computed as value of polynomial whose coefficients are determined by hash codes
   * of objects passed as parameter to one of hashCode functions. More precisely:
   * hashCode(o1, o2, ..., on) = P[o1, o2, ..., on](X) =
   * X^n + o1.hashCode() * X ^ (n - 1) + o2.hashCode() * X ^ (n - 2) + ... + on.hashCode() * X ^ 0
   *
   * <p> Constant X determines point at which polynomial is evaluated.
   */
  private static final int X = 31;

  public static int hashCode(
      @Nullable Object o1) {
    return hashCode(
        o1 == null ? 0 : o1.hashCode());
  }

  public static int hashCode(
      @Nullable Object o1,
      @Nullable Object o2) {
    return hashCode(
        o1 == null ? 0 : o1.hashCode(),
        o2 == null ? 0 : o2.hashCode());
  }

  public static int hashCode(
      @Nullable Object o1,
      @Nullable Object o2,
      @Nullable Object o3) {
    return hashCode(
        o1 == null ? 0 : o1.hashCode(),
        o2 == null ? 0 : o2.hashCode(),
        o3 == null ? 0 : o3.hashCode());
  }

  public static int hashCode(
      @Nullable Object o1,
      @Nullable Object o2,
      @Nullable Object o3,
      @Nullable Object o4) {
    return hashCode(
        o1 == null ? 0 : o1.hashCode(),
        o2 == null ? 0 : o2.hashCode(),
        o3 == null ? 0 : o3.hashCode(),
        o4 == null ? 0 : o4.hashCode());
  }

  public static int hashCode(
      @Nullable Object o1,
      @Nullable Object o2,
      @Nullable Object o3,
      @Nullable Object o4,
      @Nullable Object o5) {
    return hashCode(
        o1 == null ? 0 : o1.hashCode(),
        o2 == null ? 0 : o2.hashCode(),
        o3 == null ? 0 : o3.hashCode(),
        o4 == null ? 0 : o4.hashCode(),
        o5 == null ? 0 : o5.hashCode());
  }

  public static int hashCode(
      @Nullable Object o1,
      @Nullable Object o2,
      @Nullable Object o3,
      @Nullable Object o4,
      @Nullable Object o5,
      @Nullable Object o6) {
    return hashCode(
        o1 == null ? 0 : o1.hashCode(),
        o2 == null ? 0 : o2.hashCode(),
        o3 == null ? 0 : o3.hashCode(),
        o4 == null ? 0 : o4.hashCode(),
        o5 == null ? 0 : o5.hashCode(),
        o6 == null ? 0 : o6.hashCode());
  }

  public static int hashCode(
      int i1) {
    int acc = X + i1;
    return acc;
  }

  public static int hashCode(
      int i1,
      int i2) {
    int acc = X + i1;
    acc = X * acc + i2;
    return acc;
  }

  public static int hashCode(
      int i1,
      int i2,
      int i3) {
    int acc = X + i1;
    acc = X * acc + i2;
    acc = X * acc + i3;
    return acc;
  }

  public static int hashCode(
      int i1,
      int i2,
      int i3,
      int i4) {
    int acc = X + i1;
    acc = X * acc + i2;
    acc = X * acc + i3;
    acc = X * acc + i4;
    return acc;
  }

  public static int hashCode(
      int i1,
      int i2,
      int i3,
      int i4,
      int i5) {
    int acc = X + i1;
    acc = X * acc + i2;
    acc = X * acc + i3;
    acc = X * acc + i4;
    acc = X * acc + i5;
    return acc;
  }

  public static int hashCode(
      int i1,
      int i2,
      int i3,
      int i4,
      int i5,
      int i6) {
    int acc = X + i1;
    acc = X * acc + i2;
    acc = X * acc + i3;
    acc = X * acc + i4;
    acc = X * acc + i5;
    acc = X * acc + i6;
    return acc;
  }
}
