/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import com.facebook.infer.annotation.Nullsafe;

/** Sorts entries by date of the last access, evicting old ones first */
@Nullsafe(Nullsafe.Mode.STRICT)
public class DefaultEntryEvictionComparatorSupplier implements EntryEvictionComparatorSupplier {

  @Override
  public EntryEvictionComparator get() {
    return new EntryEvictionComparator() {
      @Override
      public int compare(DiskStorage.Entry e1, DiskStorage.Entry e2) {
        long time1 = e1.getTimestamp();
        long time2 = e2.getTimestamp();
        return time1 < time2 ? -1 : ((time2 == time1) ? 0 : 1);
      }
    };
  }
}
