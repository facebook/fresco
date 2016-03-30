/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.cache.disk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultEntryEvictionComparatorSupplierTest}
 */
@RunWith(RobolectricTestRunner.class)
public class DefaultEntryEvictionComparatorSupplierTest {

  private static final long RANDOM_SEED = 42;

  @Test
  public void testSortingOrder() {
    Random random = new Random(RANDOM_SEED);
    List<DiskStorage.Entry> entries = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      entries.add(createEntry(random.nextLong()));
    }
    Collections.sort(entries, new DefaultEntryEvictionComparatorSupplier().get());

    for (int i = 0; i < entries.size() - 1; i++) {
      assertTrue(entries.get(i).getTimestamp() < entries.get(i + 1).getTimestamp());
    }
  }

  private static DiskStorage.Entry createEntry(long time) {
    DiskStorage.Entry entry = mock(DiskStorage.Entry.class);
    when(entry.getTimestamp()).thenReturn(time);
    return entry;
  }

}
