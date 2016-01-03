/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.cache.disk;

import java.util.Comparator;

/**
 * Defines an order the items are being evicted from the cache.
 */
public interface EntryEvictionComparator extends Comparator<DiskStorage.Entry> {
}
