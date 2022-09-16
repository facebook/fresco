/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho

import com.facebook.litho.WorkingRange

/**
 * A working range targeting components that are above the viewport (i.e., components the user has
 * scrolled past).
 */
class AboveViewportWorkingRange : WorkingRange {

  private fun isInRange(position: Int, firstVisibleIndex: Int): Boolean =
      position < firstVisibleIndex

  override fun shouldEnterRange(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ): Boolean = isInRange(position, firstVisibleIndex)

  override fun shouldExitRange(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ): Boolean = !isInRange(position, firstVisibleIndex)
}
