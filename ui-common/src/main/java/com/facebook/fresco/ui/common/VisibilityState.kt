/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

enum class VisibilityState(val value: Int) {
  UNKNOWN(-1),
  VISIBLE(1),
  INVISIBLE(2);

  companion object {
    private val VALUES = values()

    fun fromInt(value: Int): VisibilityState? = VALUES.firstOrNull { it.value == value }
  }
}
