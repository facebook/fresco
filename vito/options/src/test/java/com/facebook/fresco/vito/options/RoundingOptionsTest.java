/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class RoundingOptionsTest {

  @Test
  public void testHashCode_whenEqualParameters_thenEqualHashCode() {
    RoundingOptions a = RoundingOptions.forCornerRadii(10, 20, 30, 40);
    RoundingOptions b = RoundingOptions.forCornerRadii(10, 20, 30, 40);

    assertThat(b.hashCode()).isEqualTo(a.hashCode());
  }

  @Test
  public void testHashCode_whenDifferentParameters_thenDifferentHashCode() {
    RoundingOptions a = RoundingOptions.forCornerRadii(10, 20, 30, 40);
    RoundingOptions b = RoundingOptions.forCornerRadii(99, 20, 30, 40);

    assertThat(b.hashCode()).isNotEqualTo(a.hashCode());
  }

  @Test
  public void testHashCode_whenDifferentAntiAliasing_thenDifferentHashCode() {
    RoundingOptions a = RoundingOptions.asCircle(true);
    RoundingOptions b = RoundingOptions.asCircle(false);

    assertThat(b.hashCode()).isNotEqualTo(a.hashCode());
  }
}
