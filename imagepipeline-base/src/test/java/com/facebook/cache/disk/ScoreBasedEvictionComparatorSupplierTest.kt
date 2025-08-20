/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import android.os.SystemClock
import java.util.Collections
import java.util.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

/** Test for the score-based eviction comparator. */
@RunWith(RobolectricTestRunner::class)
class ScoreBasedEvictionComparatorSupplierTest {

  companion object {
    private const val RANDOM_SEED = 42L
  }

  private lateinit var entries: MutableList<DiskStorage.Entry>

  @Before
  fun setUp() {
    val random = Random(RANDOM_SEED)

    SystemClock.setCurrentTimeMillis(0L)

    entries = mutableListOf()
    for (i in 0 until 100) {
      val entry = Mockito.mock(DiskStorage.Entry::class.java)
      Mockito.`when`(entry.timestamp).thenReturn(random.nextLong())
      Mockito.`when`(entry.size).thenReturn(random.nextLong())
      entries.add(entry)
    }
  }

  @Test
  fun testTimestampOnlyOrder() {
    doTest(1f, 0f)
    for (i in 0 until entries.size - 1) {
      assertThat(entries[i].timestamp < entries[i + 1].timestamp).isTrue()
    }
  }

  @Test
  fun testSizeOnlyOrder() {
    doTest(0f, 1f)
    for (i in 0 until entries.size - 1) {
      assertThat(entries[i].size > entries[i + 1].size).isTrue()
    }
  }

  @Test
  fun testEqualOrder() {
    doTest(1f, 1f)
  }

  @Test
  fun testWeightedOrder() {
    doTest(2f, 3f)
  }

  private fun doTest(ageWeight: Float, sizeWeight: Float) {
    val supplier = ScoreBasedEvictionComparatorSupplier(ageWeight, sizeWeight)
    Collections.sort(entries, supplier.get())

    for (i in 0 until entries.size - 1) {
      assertThat(
              supplier.calculateScore(entries[i], 0) > supplier.calculateScore(entries[i + 1], 0)
          )
          .isTrue()
    }
  }
}
