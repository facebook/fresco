/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho

import com.facebook.litho.WorkingRange

/**
 * Working range targeting components that are in the viewport (i.e., components that are visible).
 */
class InViewportWorkingRange : WorkingRange {

  @Suppress("ConvertTwoComparisonsToRangeCheck")
  private fun isInRange(position: Int, firstVisibleIndex: Int, lastVisibleIndex: Int): Boolean =
      position >= firstVisibleIndex && position <= lastVisibleIndex

  override fun shouldEnterRange(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ): Boolean = isInRange(position, firstVisibleIndex, lastVisibleIndex)

  override fun shouldExitRange(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ): Boolean = !isInRange(position, firstVisibleIndex, lastVisibleIndex)
}
