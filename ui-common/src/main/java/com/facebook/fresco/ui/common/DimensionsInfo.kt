/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

@Suppress("KtDataClass")
data class DimensionsInfo(
    val viewportWidth: Int,
    val viewportHeight: Int,
    val encodedImageWidth: Int,
    val encodedImageHeight: Int,
    val decodedImageWidth: Int,
    val decodedImageHeight: Int,
    val scaleType: String
) {
  override fun equals(other: Any?): Boolean {

    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherDimensions: DimensionsInfo = other as DimensionsInfo

    return viewportWidth == otherDimensions.viewportWidth &&
        viewportHeight == otherDimensions.viewportHeight &&
        encodedImageWidth == otherDimensions.encodedImageWidth &&
        encodedImageHeight == otherDimensions.encodedImageHeight &&
        decodedImageWidth == otherDimensions.decodedImageWidth &&
        decodedImageHeight == otherDimensions.decodedImageHeight &&
        scaleType == otherDimensions.scaleType
  }

  override fun hashCode(): Int {
    var result = viewportWidth
    result = 31 * result + viewportHeight
    result = 31 * result + encodedImageWidth
    result = 31 * result + encodedImageHeight
    result = 31 * result + decodedImageWidth
    result = 31 * result + decodedImageHeight
    result = 31 * result + scaleType.hashCode()
    return result
  }
}
