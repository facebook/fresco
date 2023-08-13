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
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for Priority enum */
class PriorityTest {
  @Test
  fun testGetHigherPriority() {
    assertEquals(Priority.HIGH, getHigherPriority(Priority.LOW, Priority.HIGH))
    assertEquals(Priority.HIGH, getHigherPriority(Priority.MEDIUM, Priority.HIGH))
    assertEquals(Priority.HIGH, getHigherPriority(Priority.HIGH, Priority.HIGH))
    assertEquals(Priority.HIGH, getHigherPriority(Priority.HIGH, Priority.MEDIUM))
    assertEquals(Priority.HIGH, getHigherPriority(Priority.HIGH, Priority.LOW))
    assertEquals(Priority.MEDIUM, getHigherPriority(Priority.LOW, Priority.MEDIUM))
    assertEquals(Priority.MEDIUM, getHigherPriority(Priority.MEDIUM, Priority.MEDIUM))
    assertEquals(Priority.MEDIUM, getHigherPriority(Priority.MEDIUM, Priority.LOW))
    assertEquals(Priority.LOW, getHigherPriority(Priority.LOW, Priority.LOW))
  }
}
