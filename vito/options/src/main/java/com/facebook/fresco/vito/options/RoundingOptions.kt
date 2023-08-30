/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import java.util.Arrays

@Suppress("KtDataClass")
data class RoundingOptions(
    val isCircular: Boolean,
    val cornerRadius: Float,
    val cornerRadii: FloatArray?,
    val isAntiAliased: Boolean,
    val isForceRoundAtDecode: Boolean,
) {

  fun hasRoundedCorners(): Boolean = cornerRadius != CORNER_RADIUS_UNSET || cornerRadii != null

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherOptions: RoundingOptions = other as RoundingOptions

    return isCircular == otherOptions.isCircular &&
        cornerRadius == otherOptions.cornerRadius &&
        cornerRadii.contentEquals(otherOptions.cornerRadii) &&
        isAntiAliased == otherOptions.isAntiAliased &&
        isForceRoundAtDecode == otherOptions.isForceRoundAtDecode
  }

  override fun hashCode(): Int {
    var result = isCircular.hashCode()
    result = 31 * result + cornerRadius.hashCode()
    result = 31 * result + Arrays.hashCode(cornerRadii)
    result = 31 * result + isAntiAliased.hashCode()
    result = 31 * result + isForceRoundAtDecode.hashCode()
    return result
  }

  @Suppress("BooleanLiteralArgument")
  companion object {
    const val CORNER_RADIUS_UNSET: Float = 0f
    val AS_CIRCLE: RoundingOptions = RoundingOptions(true, CORNER_RADIUS_UNSET, null, false, false)
    val AS_CIRCLE_ANTI_ALIASING: RoundingOptions =
        RoundingOptions(true, CORNER_RADIUS_UNSET, null, true, false)

    @JvmStatic fun asCircle(): RoundingOptions = AS_CIRCLE

    @JvmStatic
    fun asCircle(antiAliasing: Boolean): RoundingOptions =
        if (antiAliasing) AS_CIRCLE_ANTI_ALIASING else AS_CIRCLE

    @JvmStatic
    fun asCircle(antiAliasing: Boolean, forceRoundAtDecode: Boolean): RoundingOptions =
        RoundingOptions(true, CORNER_RADIUS_UNSET, null, antiAliasing, forceRoundAtDecode)

    @JvmStatic
    fun forCornerRadiusPx(cornerRadiusPx: Float): RoundingOptions =
        RoundingOptions(false, cornerRadiusPx, null, false, false)

    @JvmStatic
    fun forCornerRadii(
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float
    ): RoundingOptions =
        RoundingOptions(
            false,
            CORNER_RADIUS_UNSET,
            floatArrayOf(
                topLeft,
                topLeft,
                topRight,
                topRight,
                bottomRight,
                bottomRight,
                bottomLeft,
                bottomLeft),
            false,
            false)

    @JvmStatic
    @JvmOverloads
    fun forCornerRadii(cornerRadii: FloatArray, antiAliasing: Boolean = false): RoundingOptions =
        RoundingOptions(false, CORNER_RADIUS_UNSET, cornerRadii, antiAliasing, false)
  }
}
