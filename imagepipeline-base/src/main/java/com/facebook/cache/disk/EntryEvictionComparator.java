/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.cache.disk;

import java.util.Comparator;

/**
 * Defines an order the items are being evicted from the cache.
 */
public interface EntryEvictionComparator extends Comparator<DiskStorage.Entry> {
}
