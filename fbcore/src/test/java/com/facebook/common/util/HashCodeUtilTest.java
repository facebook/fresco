/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import static org.junit.Assert.assertEquals;

import com.facebook.common.internal.Objects;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HashCodeUtilTest {

  @Test
  public void testSimple() {
    testCase(1, 2, 3, 4, 5, 6);
  }

  @Test
  public void testRandom() {
    Random generator = new Random(123);
    testCase(
        generator.nextInt(),
        generator.nextInt(),
        generator.nextInt(),
        generator.nextInt(),
        generator.nextInt(),
        generator.nextInt());
  }

  @Test
  public void testNull() {
    testCase(1, null, 3, 4, 5, 6);
  }

  private void testCase(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
    assertEquals(
        Objects.hashCode(o1),
        HashCodeUtil.hashCode(o1));
    assertEquals(
        Objects.hashCode(o1, o2),
        HashCodeUtil.hashCode(o1, o2));
    assertEquals(
        Objects.hashCode(o1, o2, o3),
        HashCodeUtil.hashCode(o1, o2, o3));
    assertEquals(
        Objects.hashCode(o1, o2, o3, o4),
        HashCodeUtil.hashCode(o1, o2, o3, o4));
    assertEquals(
        Objects.hashCode(o1, o2, o3, o4, o5),
        HashCodeUtil.hashCode(o1, o2, o3, o4, o5));
    assertEquals(
        Objects.hashCode(o1, o2, o3, o4, o5, o6),
        HashCodeUtil.hashCode(o1, o2, o3, o4, o5, o6));
  }
}
