/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BoundedLinkedHashSetTest {
  @Test
  fun testPut() {
    val boundedLinkedHashSet = BoundedLinkedHashSet<String?>(2)
    val element = "element"
    boundedLinkedHashSet.add(element)
    Assertions.assertThat(boundedLinkedHashSet.contains(element)).isTrue()
  }

  @Test
  fun testMaxSize() {
    val maxSize = 5
    val boundedLinkedHashSet = BoundedLinkedHashSet<Int?>(maxSize)

    // insert 6 values: 0 to 5
    for (i in 0..maxSize) {
      boundedLinkedHashSet.add(i)
    }

    // the eldest element (value 0) should be removed, values 1-5 should be in the linked hash list
    Assertions.assertThat(boundedLinkedHashSet.contains(0)).isFalse()
    for (i in 1..maxSize) {
      Assertions.assertThat(boundedLinkedHashSet.contains(i)).isTrue()
    }

    // the eldest element (1) should be removed, values 2-5 and 9 should be in the linked hash list
    boundedLinkedHashSet.add(9)
    Assertions.assertThat(boundedLinkedHashSet.contains(1)).isFalse()
    Assertions.assertThat(boundedLinkedHashSet.contains(9)).isTrue()
  }

  @Test
  fun testUpdateElementPosition() {
    val maxSize = 5
    val boundedLinkedHashSet = BoundedLinkedHashSet<Int?>(maxSize)

    // insert 5 values: 0 to 4
    for (i in 0..<maxSize) {
      boundedLinkedHashSet.add(i)
    }

    // insert an existing element 0, this should update this element's position
    boundedLinkedHashSet.add(0)

    // add new value 9, this should remove the eldest element which is now 1, after adding 0 again.
    boundedLinkedHashSet.add(9)
    Assertions.assertThat(boundedLinkedHashSet.contains(1)).isFalse()
    Assertions.assertThat(boundedLinkedHashSet.contains(9)).isTrue()
    Assertions.assertThat(boundedLinkedHashSet.contains(0)).isTrue()
  }
}
