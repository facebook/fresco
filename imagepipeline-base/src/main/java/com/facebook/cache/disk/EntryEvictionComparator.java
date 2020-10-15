/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import com.facebook.infer.annotation.Nullsafe;
import java.util.Comparator;

/** Defines an order the items are being evicted from the cache. */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface EntryEvictionComparator extends Comparator<DiskStorage.Entry> {}
