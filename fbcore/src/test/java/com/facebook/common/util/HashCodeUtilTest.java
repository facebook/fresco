/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(HashCodeUtil.hashCode(o1)).isEqualTo(Objects.hashCode(o1));
    assertThat(HashCodeUtil.hashCode(o1, o2)).isEqualTo(Objects.hashCode(o1, o2));
    assertThat(HashCodeUtil.hashCode(o1, o2, o3)).isEqualTo(Objects.hashCode(o1, o2, o3));
    assertThat(HashCodeUtil.hashCode(o1, o2, o3, o4)).isEqualTo(Objects.hashCode(o1, o2, o3, o4));
    assertThat(HashCodeUtil.hashCode(o1, o2, o3, o4, o5))
        .isEqualTo(Objects.hashCode(o1, o2, o3, o4, o5));
    assertThat(HashCodeUtil.hashCode(o1, o2, o3, o4, o5, o6))
        .isEqualTo(Objects.hashCode(o1, o2, o3, o4, o5, o6));
  }
}
