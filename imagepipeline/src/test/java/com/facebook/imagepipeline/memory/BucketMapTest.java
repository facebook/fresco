/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.memory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
@RunWith(RobolectricTestRunner.class)
public class BucketMapTest {

  @Test
  public void testLRU() {
    BucketMap<Object> map = new BucketMap<>();
    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();

    map.release(1, one);
    map.release(2, two);
    map.release(3, three);

    assertLinkedList(map.mHead, map.mTail, 3, 2, 1);

    map.removeFromEnd();
    map.removeFromEnd();
    map.removeFromEnd();

    map.release(1, one);
    map.release(3, three);
    map.release(2, two);

    assertLinkedList(map.mHead, map.mTail, 2, 3, 1);

    map.acquire(3);

    assertLinkedList(map.mHead, map.mTail, 3, 2, 1);

    map.release(4, four);

    assertLinkedList(map.mHead, map.mTail, 4, 3, 2, 1);
  }

  @Test
  public void testLRU2() {
    BucketMap<Object> map = new BucketMap<>();
    Object one = new Object();
    Object two = new Object();

    map.release(1, one);
    map.release(1, two);

    assertLinkedList(map.mHead, map.mTail, 1);
    assertEquals(2, map.valueCount());

    map.removeFromEnd();
    assertEquals(1, map.valueCount());
  }

  private static void assertLinkedList(
      BucketMap.LinkedEntry head, BucketMap.LinkedEntry tail, int... expected) {
    int len = expected.length;
    int[] actual = new int[len];
    int[] expectedReverse = new int[len];
    int[] actualReverse = new int[len];

    // check head to tail
    BucketMap.LinkedEntry current = head;
    for (int i = 0; i < expected.length; i++) {
      expectedReverse[len - 1 - i] = expected[i];

      //noinspection ConstantConditions
      actual[i] = current.key;
      current = current.next;
    }
    assertArrayEquals(expected, actual);
    assertNull(head.prev);

    // check tail to head
    current = tail;
    for (int i = 0; i < expected.length; i++) {
      //noinspection ConstantConditions
      actualReverse[i] = current.key;
      current = current.prev;
    }
    assertArrayEquals(expectedReverse, actualReverse);
    assertNull(tail.next);
  }
}
