/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho

import com.facebook.litho.WorkingRange

/**
 * A working range targeting components that are below the viewport (i.e., components the user will
 * eventually reach if they scroll long enough).
 */
class BelowViewportWorkingRange(minDistance: Int, maxDistance: Int) : WorkingRange {

  private val distanceRange: IntRange = minDistance..maxDistance

  private fun isInRange(position: Int, lastVisibleIndex: Int): Boolean {
    val delta = position - lastVisibleIndex
    return delta in distanceRange
  }

  override fun shouldEnterRange(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ): Boolean = isInRange(position, lastVisibleIndex)

  override fun shouldExitRange(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ): Boolean = !isInRange(position, lastVisibleIndex)
}
