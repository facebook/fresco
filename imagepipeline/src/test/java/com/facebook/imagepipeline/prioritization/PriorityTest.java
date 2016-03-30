/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.prioritization;

import com.facebook.imagepipeline.common.Priority;

import org.junit.*;

import static com.facebook.imagepipeline.common.Priority.HIGH;
import static com.facebook.imagepipeline.common.Priority.LOW;
import static com.facebook.imagepipeline.common.Priority.MEDIUM;
import static org.junit.Assert.*;

/**
 * Tests for Priority enum
 */
public class PriorityTest {

  @Test
  public void testGetHigherPriority() throws Exception {
    assertEquals(HIGH, Priority.getHigherPriority(null, HIGH));
    assertEquals(HIGH, Priority.getHigherPriority(LOW, HIGH));
    assertEquals(HIGH, Priority.getHigherPriority(MEDIUM, HIGH));
    assertEquals(HIGH, Priority.getHigherPriority(HIGH, HIGH));
    assertEquals(HIGH, Priority.getHigherPriority(HIGH, MEDIUM));
    assertEquals(HIGH, Priority.getHigherPriority(HIGH, LOW));
    assertEquals(HIGH, Priority.getHigherPriority(HIGH, null));

    assertEquals(MEDIUM, Priority.getHigherPriority(null, MEDIUM));
    assertEquals(MEDIUM, Priority.getHigherPriority(LOW, MEDIUM));
    assertEquals(MEDIUM, Priority.getHigherPriority(MEDIUM, MEDIUM));
    assertEquals(MEDIUM, Priority.getHigherPriority(MEDIUM, LOW));
    assertEquals(MEDIUM, Priority.getHigherPriority(MEDIUM, null));

    assertEquals(LOW, Priority.getHigherPriority(null, LOW));
    assertEquals(LOW, Priority.getHigherPriority(LOW, LOW));
    assertEquals(LOW, Priority.getHigherPriority(LOW, null));

    assertEquals(null, Priority.getHigherPriority(null, null));
  }
}
