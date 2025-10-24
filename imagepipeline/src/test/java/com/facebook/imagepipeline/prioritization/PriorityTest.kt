/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.prioritization

import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.common.Priority.Companion.getHigherPriority
import com.facebook.imagepipeline.common.Priority.HIGH
import com.facebook.imagepipeline.common.Priority.LOW
import com.facebook.imagepipeline.common.Priority.MEDIUM
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Tests for Priority enum */
class PriorityTest {
  @Test
  fun testGetHigherPriority() {
    assertThat(getHigherPriority(Priority.LOW, Priority.HIGH)).isEqualTo(Priority.HIGH)
    assertThat(getHigherPriority(Priority.MEDIUM, Priority.HIGH)).isEqualTo(Priority.HIGH)
    assertThat(getHigherPriority(Priority.HIGH, Priority.HIGH)).isEqualTo(Priority.HIGH)
    assertThat(getHigherPriority(Priority.HIGH, Priority.MEDIUM)).isEqualTo(Priority.HIGH)
    assertThat(getHigherPriority(Priority.HIGH, Priority.LOW)).isEqualTo(Priority.HIGH)
    assertThat(getHigherPriority(Priority.LOW, Priority.MEDIUM)).isEqualTo(Priority.MEDIUM)
    assertThat(getHigherPriority(Priority.MEDIUM, Priority.MEDIUM)).isEqualTo(Priority.MEDIUM)
    assertThat(getHigherPriority(Priority.MEDIUM, Priority.LOW)).isEqualTo(Priority.MEDIUM)
    assertThat(getHigherPriority(Priority.LOW, Priority.LOW)).isEqualTo(Priority.LOW)
  }
}
