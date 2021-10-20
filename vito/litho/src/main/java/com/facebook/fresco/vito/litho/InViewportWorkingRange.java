/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import com.facebook.litho.WorkingRange;

/**
 * Working range targeting components that are in the viewport (i.e., components that are visible).
 */
public class InViewportWorkingRange implements WorkingRange {
  private boolean isInRange(int position, int firstVisibleIndex, int lastVisibleIndex) {
    return position >= firstVisibleIndex && position <= lastVisibleIndex;
  }

  @Override
  public boolean shouldEnterRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    return isInRange(position, firstVisibleIndex, lastVisibleIndex);
  }

  @Override
  public boolean shouldExitRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    return !isInRange(position, firstVisibleIndex, lastVisibleIndex);
  }
}
