/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util

import com.facebook.infer.annotation.Functional

/** Generic tri-state enum for boolean values that can also be unset. */
enum class TriState {

  YES,
  NO,
  UNSET,
  ;

  @get:Functional
  val isSet: Boolean
    /** @return whether this value is set; that is, whether it is YES or NO. */
    get() = this != UNSET

  /**
   * Returns the `boolean` value that corresponds to this [TriState], if appropriate.
   *
   * @return `true` if `this` is [TriState#YES] or `false` if `this` is [TriState#NO]
   * @throws IllegalStateException if `this` is [TriState#UNSET].
   */
  @Functional
  fun asBoolean(): Boolean =
      when (this) {
        YES -> true
        NO -> false
        UNSET -> throw IllegalStateException("No boolean equivalent for UNSET")
        else -> throw IllegalStateException("Unrecognized TriState value: $this")
      }

  /**
   * Returns the `boolean` value that corresponds to this [TriState], if appropriate.
   *
   * @param defaultValue default value to use if not set
   * @return `true` if `this` is [TriState#YES] or `false` if `this` is [TriState#NO] or
   *   `defaultValue` if `this` is [TriState#UNSET].
   */
  @Functional
  fun asBoolean(defaultValue: Boolean): Boolean =
      when (this) {
        YES -> true
        NO -> false
        UNSET -> defaultValue
        else -> throw IllegalStateException("Unrecognized TriState value: $this")
      }

  /**
   * Returns the `Boolean` value that corresponds to this [TriState], if appropriate.
   *
   * @return [Boolean#TRUE] if `this` is [TriState#YES] or [Boolean#FALSE] if `this` is
   *   [TriState#NO] or `null` if `this` is [TriState#UNSET].
   */
  @Functional
  fun asBooleanObject(): Boolean? =
      when (this) {
        YES -> true
        NO -> false
        UNSET -> null
        else -> throw IllegalStateException("Unrecognized TriState value: $this")
      }

  @get:Functional
  val dbValue: Int
    get() {
      return when (this) {
        YES -> 1
        NO -> 2
        UNSET -> 3
        else -> 3
      }
    }

  companion object {
    /**
     * Returns the value of the [TriState] enum that corresponds to the specified `boolean`.
     *
     * This method deliberately declares `boolean` as its param type rather than [Boolean] because:
     *
     * 1. Declaring [Boolean] would likely result in a bunch of unnecessary autoboxing.
     * 1. Anyone who finds himself using a [Boolean] instead of a `boolean` for its nullability
     *    should replace the [Boolean] with a [TriState], anyway.
     */
    @JvmStatic
    @Functional
    fun valueOf(bool: Boolean): TriState = if (bool) TriState.YES else TriState.NO

    @JvmStatic
    @Functional
    fun valueOf(bool: Boolean?): TriState = if (bool != null) valueOf(bool) else TriState.UNSET

    @JvmStatic
    @Functional
    fun fromDbValue(value: Int): TriState =
        when (value) {
          1 -> TriState.YES
          2 -> TriState.NO
          3 -> TriState.UNSET
          else -> TriState.UNSET
        }
  }
}
