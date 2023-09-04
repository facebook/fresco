/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.viewport

@Suppress("KtDataClass")
data class ViewportData(val width: Int, val height: Int, val transform: HasTransform?) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherData: ViewportData = other as ViewportData

    return width == otherData.width &&
        height == otherData.height &&
        transform == otherData.transform
  }

  override fun hashCode(): Int {
    var result = width
    result = 31 * result + height
    result = 31 * result + (transform?.hashCode() ?: 0)
    return result
  }
}
