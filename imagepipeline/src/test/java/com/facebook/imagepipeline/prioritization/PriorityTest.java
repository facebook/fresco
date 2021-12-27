/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.prioritization;

import static com.facebook.imagepipeline.common.Priority.HIGH;
import static com.facebook.imagepipeline.common.Priority.LOW;
import static com.facebook.imagepipeline.common.Priority.MEDIUM;
import static org.junit.Assert.*;

import com.facebook.imagepipeline.common.Priority;
import org.junit.*;

/** Tests for Priority enum */
public class PriorityTest {

  @Test
  public void testGetHigherPriority() throws Exception {
    assertEquals(HIGH, Priority.getHigherPriority(LOW, HIGH));
    assertEquals(HIGH, Priority.getHigherPriority(MEDIUM, HIGH));
    assertEquals(HIGH, Priority.getHigherPriority(HIGH, HIGH));
    assertEquals(HIGH, Priority.getHigherPriority(HIGH, MEDIUM));
    assertEquals(HIGH, Priority.getHigherPriority(HIGH, LOW));

    assertEquals(MEDIUM, Priority.getHigherPriority(LOW, MEDIUM));
    assertEquals(MEDIUM, Priority.getHigherPriority(MEDIUM, MEDIUM));
    assertEquals(MEDIUM, Priority.getHigherPriority(MEDIUM, LOW));

    assertEquals(LOW, Priority.getHigherPriority(LOW, LOW));
  }
}
