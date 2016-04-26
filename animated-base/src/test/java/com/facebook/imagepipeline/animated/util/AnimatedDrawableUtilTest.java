/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.util;

import org.robolectric.RobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Tests for {@link AnimatedDrawableUtil}.
 */
@RunWith(RobolectricTestRunner.class)
public class AnimatedDrawableUtilTest {

  @Test
  public void testGetFrameTimeStampsFromDurations() {
    int[] frameDurationsMs = new int[] { 30, 30, 60, 30, 30};
    AnimatedDrawableUtil util = new AnimatedDrawableUtil();
    int[] frameTimestampsMs = util.getFrameTimeStampsFromDurations(frameDurationsMs);
    int[] expected = new int[] {0, 30, 60, 120, 150};
    assertArrayEquals(expected, frameTimestampsMs);
  }

  @Test
  public void testGetFrameTimeStampsFromDurationsWithEmptyArray() {
    int[] frameDurationsMs = new int[0];
    AnimatedDrawableUtil util = new AnimatedDrawableUtil();
    int[] frameTimestampsMs = util.getFrameTimeStampsFromDurations(frameDurationsMs);
    assertEquals(0, frameTimestampsMs.length);
  }

  @Test
  public void testGetFrameForTimestampMs() {
    int[] frameTimestampsMs = new int[] { 0, 50, 75, 100, 200};
    AnimatedDrawableUtil util = new AnimatedDrawableUtil();
    assertEquals(0, util.getFrameForTimestampMs(frameTimestampsMs, 0));
    assertEquals(0, util.getFrameForTimestampMs(frameTimestampsMs, 1));
    assertEquals(0, util.getFrameForTimestampMs(frameTimestampsMs, 49));
    assertEquals(1, util.getFrameForTimestampMs(frameTimestampsMs, 50));
    assertEquals(1, util.getFrameForTimestampMs(frameTimestampsMs, 74));
    assertEquals(2, util.getFrameForTimestampMs(frameTimestampsMs, 75));
    assertEquals(2, util.getFrameForTimestampMs(frameTimestampsMs, 76));
    assertEquals(2, util.getFrameForTimestampMs(frameTimestampsMs, 99));
    assertEquals(3, util.getFrameForTimestampMs(frameTimestampsMs, 100));
    assertEquals(3, util.getFrameForTimestampMs(frameTimestampsMs, 101));
    assertEquals(4, util.getFrameForTimestampMs(frameTimestampsMs, 200));
  }

  @Test
  public void testIsOutsideRange() {
    assertTrue(AnimatedDrawableUtil.isOutsideRange(-1, -1, 1)); // Always outside range

    // Test before, within, and after 2 through 5.
    int start = 2;
    int end = 5;
    assertTrue(AnimatedDrawableUtil.isOutsideRange(start, end, 1));
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 2));
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 3));
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 4));
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 5));
    assertTrue(AnimatedDrawableUtil.isOutsideRange(start, end, 6));

    // Test wrapping case when start is greater than end
    // Test before, within, and after 4 through 1
    start = 4;
    end = 1;
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 0));
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 1));
    assertTrue(AnimatedDrawableUtil.isOutsideRange(start, end, 2));
    assertTrue(AnimatedDrawableUtil.isOutsideRange(start, end, 3));
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 4));
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 5));

    // Test cases where start == end
    start = 2;
    end = 2;
    assertTrue(AnimatedDrawableUtil.isOutsideRange(start, end, 1));
    assertFalse(AnimatedDrawableUtil.isOutsideRange(start, end, 2));
    assertTrue(AnimatedDrawableUtil.isOutsideRange(start, end, 3));
  }
}
