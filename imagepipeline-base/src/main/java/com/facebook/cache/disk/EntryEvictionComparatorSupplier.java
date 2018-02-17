/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.cache.disk;

/**
 * Provides an instance of eviction comparator
 */
public interface EntryEvictionComparatorSupplier {
  EntryEvictionComparator get();
}
