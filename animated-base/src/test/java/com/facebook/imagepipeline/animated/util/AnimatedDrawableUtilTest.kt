/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [AnimatedDrawableUtil]. */
@RunWith(RobolectricTestRunner::class)
class AnimatedDrawableUtilTest {
  @Test
  fun testGetFrameTimeStampsFromDurations() {
    val frameDurationsMs = intArrayOf(30, 30, 60, 30, 30)
    val util = AnimatedDrawableUtil()
    val frameTimestampsMs = util.getFrameTimeStampsFromDurations(frameDurationsMs)
    val expected = intArrayOf(0, 30, 60, 120, 150)
    assertThat(frameTimestampsMs).isEqualTo(expected)
  }

  @Test
  fun testGetFrameTimeStampsFromDurationsWithEmptyArray() {
    val frameDurationsMs = IntArray(0)
    val util = AnimatedDrawableUtil()
    val frameTimestampsMs = util.getFrameTimeStampsFromDurations(frameDurationsMs)
    assertThat(frameTimestampsMs.size.toLong()).isEqualTo(0)
  }

  @Test
  fun testGetFrameForTimestampMs() {
    val frameTimestampsMs = intArrayOf(0, 50, 75, 100, 200)
    val util = AnimatedDrawableUtil()
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 0).toLong()).isEqualTo(0)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 1).toLong()).isEqualTo(0)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 49).toLong()).isEqualTo(0)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 50).toLong()).isEqualTo(1)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 74).toLong()).isEqualTo(1)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 75).toLong()).isEqualTo(2)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 76).toLong()).isEqualTo(2)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 99).toLong()).isEqualTo(2)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 100).toLong()).isEqualTo(3)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 101).toLong()).isEqualTo(3)
    assertThat(util.getFrameForTimestampMs(frameTimestampsMs, 200).toLong()).isEqualTo(4)
  }

  @Test
  fun testIsOutsideRange() {
    assertThat(AnimatedDrawableUtil.isOutsideRange(-1, -1, 1)).isTrue() // Always outside range

    // Test before, within, and after 2 through 5.
    var start = 2
    var end = 5
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 1)).isTrue()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 2)).isFalse()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 3)).isFalse()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 4)).isFalse()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 5)).isFalse()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 6)).isTrue()

    // Test wrapping case when start is greater than end
    // Test before, within, and after 4 through 1
    start = 4
    end = 1
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 0)).isFalse()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 1)).isFalse()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 2)).isTrue()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 3)).isTrue()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 4)).isFalse()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 5)).isFalse()

    // Test cases where start == end
    start = 2
    end = 2
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 1)).isTrue()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 2)).isFalse()
    assertThat(AnimatedDrawableUtil.isOutsideRange(start, end, 3)).isTrue()
  }
}
