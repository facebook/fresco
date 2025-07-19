/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

@Suppress("KtDataClass")
data class AnimatedOptions(
    val loopCount: Int,
) {

  fun isInfinite(): Boolean = loopCount == LOOP_COUNT_INFINITE

  fun isStatic(): Boolean = loopCount == LOOP_COUNT_STATIC

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherOptions = other as? AnimatedOptions ?: return false

    return loopCount == otherOptions.loopCount
  }

  override fun hashCode(): Int {
    return loopCount.hashCode()
  }

  @Suppress("BooleanLiteralArgument")
  companion object {
    const val LOOP_COUNT_INFINITE: Int = 0
    const val LOOP_COUNT_STATIC: Int = 1
    val INFINITE: AnimatedOptions = AnimatedOptions(LOOP_COUNT_INFINITE)
    val STATIC_FIRST_FRAME: AnimatedOptions = AnimatedOptions(LOOP_COUNT_STATIC)

    @JvmStatic fun infinite(): AnimatedOptions = INFINITE

    @JvmStatic fun static(): AnimatedOptions = STATIC_FIRST_FRAME

    @JvmStatic fun loop(loopCount: Int): AnimatedOptions = AnimatedOptions(loopCount)
  }
}
