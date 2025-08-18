/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util

/**
 * Provides implementation of hashCode for compound objects. Implementation provided by this class
 * gives the same results as Objects.hashCode, but does not create array consisting of all
 * components
 */
object HashCodeUtil {

  /**
   * Hash code is computed as value of polynomial whose coefficients are determined by hash codes of
   * objects passed as parameter to one of hashCode functions. More precisely: hashCode(o1, o2, ...,
   * on) = P[o1, o2, ..., on](X) = X^n + o1.hashCode() * X ^ (n - 1) + o2.hashCode() * X ^ (n - 2) +
   * ... + on.hashCode() * X ^ 0
   *
   * Constant X determines point at which polynomial is evaluated.
   */
  private const val X = 31

  @JvmStatic fun hashCode(o1: Any?): Int = hashCode(o1?.hashCode() ?: 0)

  @JvmStatic
  fun hashCode(o1: Any?, o2: Any?): Int = hashCode(o1?.hashCode() ?: 0, o2?.hashCode() ?: 0)

  @JvmStatic
  fun hashCode(o1: Any?, o2: Any?, o3: Any?): Int =
      hashCode(o1?.hashCode() ?: 0, o2?.hashCode() ?: 0, o3?.hashCode() ?: 0)

  @JvmStatic
  fun hashCode(o1: Any?, o2: Any?, o3: Any?, o4: Any?): Int =
      hashCode(o1?.hashCode() ?: 0, o2?.hashCode() ?: 0, o3?.hashCode() ?: 0, o4?.hashCode() ?: 0)

  @JvmStatic
  fun hashCode(o1: Any?, o2: Any?, o3: Any?, o4: Any?, o5: Any?): Int =
      hashCode(
          o1?.hashCode() ?: 0,
          o2?.hashCode() ?: 0,
          o3?.hashCode() ?: 0,
          o4?.hashCode() ?: 0,
          o5?.hashCode() ?: 0,
      )

  @JvmStatic
  fun hashCode(o1: Any?, o2: Any?, o3: Any?, o4: Any?, o5: Any?, o6: Any?): Int =
      hashCode(
          o1?.hashCode() ?: 0,
          o2?.hashCode() ?: 0,
          o3?.hashCode() ?: 0,
          o4?.hashCode() ?: 0,
          o5?.hashCode() ?: 0,
          o6?.hashCode() ?: 0,
      )

  @JvmStatic
  fun hashCode(i1: Int): Int {
    val acc = X + i1
    return acc
  }

  @JvmStatic
  fun hashCode(i1: Int, i2: Int): Int {
    var acc = X + i1
    acc = X * acc + i2
    return acc
  }

  @JvmStatic
  fun hashCode(i1: Int, i2: Int, i3: Int): Int {
    var acc = X + i1
    acc = X * acc + i2
    acc = X * acc + i3
    return acc
  }

  @JvmStatic
  fun hashCode(i1: Int, i2: Int, i3: Int, i4: Int): Int {
    var acc = X + i1
    acc = X * acc + i2
    acc = X * acc + i3
    acc = X * acc + i4
    return acc
  }

  @JvmStatic
  fun hashCode(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int): Int {
    var acc = X + i1
    acc = X * acc + i2
    acc = X * acc + i3
    acc = X * acc + i4
    acc = X * acc + i5
    return acc
  }

  @JvmStatic
  fun hashCode(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int): Int {
    var acc = X + i1
    acc = X * acc + i2
    acc = X * acc + i3
    acc = X * acc + i4
    acc = X * acc + i5
    acc = X * acc + i6
    return acc
  }
}
