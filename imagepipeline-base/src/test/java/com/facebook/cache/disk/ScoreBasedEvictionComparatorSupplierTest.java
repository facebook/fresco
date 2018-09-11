/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for the score-based eviction comparator.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class})
public class ScoreBasedEvictionComparatorSupplierTest {

  private static final long RANDOM_SEED = 42;

  private List<DiskStorage.Entry> entries;

  @Before
  public void setUp() {
    Random random = new Random(RANDOM_SEED);

    PowerMockito.mockStatic(System.class);
    when(System.currentTimeMillis()).thenReturn(0l);

    entries = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      DiskStorage.Entry entry = mock(DiskStorage.Entry.class);
      when(entry.getTimestamp()).thenReturn(random.nextLong());
      when(entry.getSize()).thenReturn(random.nextLong());
      entries.add(entry);
    }
  }

  @Test
  public void testTimestampOnlyOrder() {
    doTest(1f, 0f);
    for (int i = 0; i < entries.size() - 1; i++) {
      assertTrue(entries.get(i).getTimestamp() < entries.get(i + 1).getTimestamp());
    }
  }

  @Test
  public void testSizeOnlyOrder() {
    doTest(0f, 1f);
    for (int i = 0; i < entries.size() - 1; i++) {
      assertTrue(entries.get(i).getSize() > entries.get(i + 1).getSize());
    }
  }

  @Test
  public void testEqualOrder() {
    doTest(1f, 1f);
  }

  @Test
  public void testWeightedOrder() {
    doTest(2f, 3f);
  }

  private void doTest(float ageWeight, float sizeWeight) {
    ScoreBasedEvictionComparatorSupplier supplier =
        new ScoreBasedEvictionComparatorSupplier(ageWeight, sizeWeight);
    Collections.sort(entries, supplier.get());

    for (int i = 0; i < entries.size() - 1; i++) {
      assertTrue(supplier.calculateScore(entries.get(i), 0) >
          supplier.calculateScore(entries.get(i + 1), 0));
    }
  }
}
