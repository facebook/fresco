/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import java.util.Arrays

data class RoundingOptions(
    val isCircular: Boolean,
    val cornerRadius: Float,
    val cornerRadii: FloatArray?,
    val isAntiAliased: Boolean,
    val isForceRoundAtDecode: Boolean
) {
  fun hasRoundedCorners(): Boolean = cornerRadius != CORNER_RADIUS_UNSET || cornerRadii != null

  override fun equals(other: Any?): Boolean {
    if (!fixEqualityChecks) {
      return super.equals(other)
    }

    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) return false
    other as RoundingOptions
    return isCircular == other.isCircular &&
        cornerRadius == other.cornerRadius &&
        cornerRadii.contentEquals(other.cornerRadii) &&
        isAntiAliased == other.isAntiAliased &&
        isForceRoundAtDecode == other.isForceRoundAtDecode
  }

  override fun hashCode(): Int {
    if (!fixEqualityChecks) {
      return super.hashCode()
    }
    var result = if (isCircular) 1 else 0
    result = 31 * result + cornerRadius.hashCode()
    result = 31 * result + Arrays.hashCode(cornerRadii)
    result = 31 * result + if (isAntiAliased) 1 else 0
    result = 31 * result + if (isForceRoundAtDecode) 1 else 0
    return result
  }

  companion object {
    const val CORNER_RADIUS_UNSET = 0f
    val AS_CIRCLE = RoundingOptions(true, CORNER_RADIUS_UNSET, null, false, false)
    val AS_CIRCLE_ANTI_ALIASING = RoundingOptions(true, CORNER_RADIUS_UNSET, null, true, false)

    var fixEqualityChecks: Boolean = true

    @JvmStatic fun asCircle(): RoundingOptions = AS_CIRCLE

    @JvmStatic
    fun asCircle(antiAliasing: Boolean): RoundingOptions =
        if (antiAliasing) AS_CIRCLE_ANTI_ALIASING else AS_CIRCLE

    @JvmStatic
    fun asCircle(antiAliasing: Boolean, forceRoundAtDecode: Boolean): RoundingOptions {
      return RoundingOptions(true, CORNER_RADIUS_UNSET, null, antiAliasing, forceRoundAtDecode)
    }

    @JvmStatic
    fun forCornerRadiusPx(cornerRadiusPx: Float): RoundingOptions {
      return RoundingOptions(false, cornerRadiusPx, null, false, false)
    }

    @JvmStatic
    fun forCornerRadii(
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float
    ): RoundingOptions {
      val radii = FloatArray(8)
      radii[0] = topLeft
      radii[1] = topLeft
      radii[2] = topRight
      radii[3] = topRight
      radii[4] = bottomRight
      radii[5] = bottomRight
      radii[6] = bottomLeft
      radii[7] = bottomLeft
      return RoundingOptions(false, CORNER_RADIUS_UNSET, radii, false, false)
    }

    @JvmStatic
    @JvmOverloads
    fun forCornerRadii(cornerRadii: FloatArray, antiAliasing: Boolean = false): RoundingOptions {
      return RoundingOptions(false, CORNER_RADIUS_UNSET, cornerRadii, antiAliasing, false)
    }
  }
}
