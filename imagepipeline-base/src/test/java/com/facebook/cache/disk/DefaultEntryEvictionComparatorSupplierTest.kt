/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import java.util.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

/** Test for [DefaultEntryEvictionComparatorSupplierTest] */
@RunWith(RobolectricTestRunner::class)
class DefaultEntryEvictionComparatorSupplierTest {
  @Test
  fun testSortingOrder() {
    val random = Random(RANDOM_SEED)
    val entries: MutableList<DiskStorage.Entry> = ArrayList()
    for (i in 0..99) {
      entries.add(createEntry(random.nextLong()))
    }
    entries.sortWith(DefaultEntryEvictionComparatorSupplier().get())

    for (i in 0 until entries.size - 1) {
      val currentEntry = entries[i]
      val nextEntry = entries[i + 1]
      assertThat(currentEntry.timestamp).isLessThan(nextEntry.timestamp)
    }
  }

  companion object {
    private const val RANDOM_SEED: Long = 42

    private fun createEntry(time: Long): DiskStorage.Entry {
      val entry = Mockito.mock(DiskStorage.Entry::class.java)
      Mockito.`when`(entry.timestamp).thenReturn(time)
      return entry
    }
  }
}
