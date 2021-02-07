/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BoundedLinkedHashSetTest {

  @Test
  public void testPut() {
    BoundedLinkedHashSet<String> boundedLinkedHashSet = new BoundedLinkedHashSet<>(2);
    String element = "element";
    boundedLinkedHashSet.add(element);
    assertThat(boundedLinkedHashSet.contains(element)).isTrue();
  }

  @Test
  public void testMaxSize() {
    int maxSize = 5;
    BoundedLinkedHashSet<Integer> boundedLinkedHashSet = new BoundedLinkedHashSet<>(maxSize);

    // insert 6 values: 0 to 5
    for (int i = 0; i <= maxSize; i++) {
      boundedLinkedHashSet.add(i);
    }

    // the eldest element (value 0) should be removed, values 1-5 should be in the linked hash list
    assertThat(boundedLinkedHashSet.contains(0)).isFalse();
    for (int i = 1; i <= maxSize; i++) {
      assertThat(boundedLinkedHashSet.contains(i)).isTrue();
    }

    // the eldest element (1) should be removed, values 2-5 and 9 should be in the linked hash list
    boundedLinkedHashSet.add(9);
    assertThat(boundedLinkedHashSet.contains(1)).isFalse();
    assertThat(boundedLinkedHashSet.contains(9)).isTrue();
  }

  @Test
  public void testUpdateElementPosition() {
    int maxSize = 5;
    BoundedLinkedHashSet<Integer> boundedLinkedHashSet = new BoundedLinkedHashSet<>(maxSize);

    // insert 5 values: 0 to 4
    for (int i = 0; i < maxSize; i++) {
      boundedLinkedHashSet.add(i);
    }

    // insert an existing element 0, this should update this element's position
    boundedLinkedHashSet.add(0);

    // add new value 9, this should remove the eldest element which is now 1, after adding 0 again.
    boundedLinkedHashSet.add(9);
    assertThat(boundedLinkedHashSet.contains(1)).isFalse();
    assertThat(boundedLinkedHashSet.contains(9)).isTrue();
    assertThat(boundedLinkedHashSet.contains(0)).isTrue();
  }
}
