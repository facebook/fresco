/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/** Test for TimeWaster */
public class TimeWasterTest {

  @Test
  public void testFib_0_0() {
    final long result = TimeWaster.Fib(0);
    assertThat(result).isEqualTo(0);
  }

  @Test
  public void testFib_1_1() {
    final long result = TimeWaster.Fib(1);
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void testFib_2_1() {
    final long result = TimeWaster.Fib(2);
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void testFib_5_5() {
    final long result = TimeWaster.Fib(5);
    assertThat(result).isEqualTo(5);
  }

  @Test
  public void testFib_10_55() {
    final long result = TimeWaster.Fib(10);
    assertThat(result).isEqualTo(55);
  }

  @Test
  public void testFib_20_6765() {
    final long result = TimeWaster.Fib(20);
    assertThat(result).isEqualTo(6765);
  }
}
