/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import com.facebook.infer.annotation.Nullsafe;

/** Provides an instance of eviction comparator */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface EntryEvictionComparatorSupplier {
  EntryEvictionComparator get();
}
