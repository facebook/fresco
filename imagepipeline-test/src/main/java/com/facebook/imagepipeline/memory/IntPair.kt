/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import com.facebook.common.internal.Objects

/** Surprise! A pair of integers */
class IntPair(@JvmField val a: Int, @JvmField val b: Int) {

  override fun hashCode(): Int = Objects.hashCode(a, b)

  override fun equals(other: Any?): Boolean {
    if (other is IntPair) {
      val that = other
      return this.a == that.a && this.b == that.b
    }
    return false
  }

  override fun toString(): String = "[$a, $b]"
}
