/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.urimod

@Suppress("KtDataClass")
data class Dimensions(val w: Int, val h: Int) {

  override fun toString(): String = "${w}x${h}"

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherDimensions: Dimensions = other as Dimensions

    return w == otherDimensions.w && h == otherDimensions.h
  }

  override fun hashCode(): Int = 31 * w + h
}
