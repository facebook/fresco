/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BucketMapTest {
  @Test
  fun testLRU() {
    val map = BucketMap<Any>()
    val one = Any()
    val two = Any()
    val three = Any()
    val four = Any()

    map.release(1, one)
    map.release(2, two)
    map.release(3, three)

    assertLinkedList(map.mHead, map.mTail, 3, 2, 1)

    map.removeFromEnd()
    map.removeFromEnd()
    map.removeFromEnd()

    map.release(1, one)
    map.release(3, three)
    map.release(2, two)

    assertLinkedList(map.mHead, map.mTail, 2, 3, 1)

    map.acquire(3)

    assertLinkedList(map.mHead, map.mTail, 3, 2, 1)

    map.release(4, four)

    assertLinkedList(map.mHead, map.mTail, 4, 3, 2, 1)
  }

  @Test
  fun testLRU2() {
    val map = BucketMap<Any>()
    val one = Any()
    val two = Any()

    map.release(1, one)
    map.release(1, two)

    assertLinkedList(map.mHead, map.mTail, 1)
    assertThat(map.valueCount()).isEqualTo(2)

    map.removeFromEnd()
    assertThat(map.valueCount()).isEqualTo(1)
  }

  companion object {
    private fun assertLinkedList(
        head: BucketMap.LinkedEntry<*>?,
        tail: BucketMap.LinkedEntry<*>?,
        vararg expected: Int,
    ) {
      // Ensure head and tail are not null
      assertThat(head).isNotNull
      assertThat(tail).isNotNull

      val len = expected.size
      val actual = IntArray(len)
      val expectedReverse = IntArray(len)
      val actualReverse = IntArray(len)

      // check head to tail
      var current: BucketMap.LinkedEntry<*>? = head
      for (i in expected.indices) {
        expectedReverse[len - 1 - i] = expected[i]

        assertThat(current).isNotNull
        actual[i] = current?.key ?: 0
        current = current?.next
      }
      assertThat(actual).isEqualTo(expected)
      assertThat(head?.prev).isNull()

      // check tail to head
      current = tail
      for (i in expected.indices) {
        assertThat(current).isNotNull
        actualReverse[i] = current?.key ?: 0
        current = current?.prev
      }
      assertThat(actualReverse).isEqualTo(expectedReverse)
      assertThat(tail?.next).isNull()
    }
  }
}
