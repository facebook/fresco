/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Test for TimeWaster */
public class TimeWasterTest {

  @Test
  public void testFib_0_0() {
    final long result = TimeWaster.Fib(0);
    assertEquals(0, result);
  }

  @Test
  public void testFib_1_1() {
    final long result = TimeWaster.Fib(1);
    assertEquals(1, result);
  }

  @Test
  public void testFib_2_1() {
    final long result = TimeWaster.Fib(2);
    assertEquals(1, result);
  }

  @Test
  public void testFib_5_5() {
    final long result = TimeWaster.Fib(5);
    assertEquals(5, result);
  }

  @Test
  public void testFib_10_55() {
    final long result = TimeWaster.Fib(10);
    assertEquals(55, result);
  }

  @Test
  public void testFib_20_6765() {
    final long result = TimeWaster.Fib(20);
    assertEquals(6765, result);
  }
}
