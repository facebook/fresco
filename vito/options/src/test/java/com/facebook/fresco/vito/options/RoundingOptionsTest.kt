/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RoundingOptionsTest {

  @Test
  fun testHashCode_whenEqualParameters_thenEqualHashCode() {
    val a = RoundingOptions.forCornerRadii(10f, 20f, 30f, 40f)
    val b = RoundingOptions.forCornerRadii(10f, 20f, 30f, 40f)
    assertThat(b.hashCode()).isEqualTo(a.hashCode())
  }

  @Test
  fun testHashCode_whenDifferentParameters_thenDifferentHashCode() {
    val a = RoundingOptions.forCornerRadii(10f, 20f, 30f, 40f)
    val b = RoundingOptions.forCornerRadii(99f, 20f, 30f, 40f)
    assertThat(b.hashCode()).isNotEqualTo(a.hashCode())
  }

  @Test
  fun testHashCode_whenDifferentAntiAliasing_thenDifferentHashCode() {
    val a = RoundingOptions.asCircle(true)
    val b = RoundingOptions.asCircle(false)
    assertThat(b.hashCode()).isNotEqualTo(a.hashCode())
  }
}
