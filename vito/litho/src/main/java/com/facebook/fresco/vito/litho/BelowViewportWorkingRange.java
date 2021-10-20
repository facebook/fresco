/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import com.facebook.litho.WorkingRange;

/**
 * A working range targeting components that are below the viewport (i.e., components the user will
 * eventually reach if they scroll long enough).
 */
public class BelowViewportWorkingRange implements WorkingRange {
  private final int mMinDistance, mMaxDistance;

  public BelowViewportWorkingRange(int minDistance, int maxDistance) {
    this.mMinDistance = minDistance;
    this.mMaxDistance = maxDistance;
  }

  private boolean isInRange(int position, int lastVisibleIndex) {
    int delta = position - lastVisibleIndex;
    return delta >= mMinDistance && delta <= mMaxDistance;
  }

  @Override
  public boolean shouldEnterRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    return isInRange(position, lastVisibleIndex);
  }

  @Override
  public boolean shouldExitRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    return !isInRange(position, lastVisibleIndex);
  }
}
