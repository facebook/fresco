/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import com.facebook.litho.WorkingRange;

/**
 * A working range targeting components that are above the viewport (i.e., components the user has
 * scrolled past).
 */
public class AboveViewportWorkingRange implements WorkingRange {
  private boolean isInRange(int position, int firstVisibleIndex) {
    return position < firstVisibleIndex;
  }

  @Override
  public boolean shouldEnterRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    return isInRange(position, firstVisibleIndex);
  }

  @Override
  public boolean shouldExitRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    return !isInRange(position, firstVisibleIndex);
  }
}
