/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.loadframe

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FpsCompressorInfoTest {

  @Test
  fun calculateReducedIndexes_pusheenAsset_preservesAllFrames() {
    val compressor = FpsCompressorInfo(maxFpsLimit = 30, shouldRoundUpFractionalFrameBudget = true)

    // 7 FPS over 560 ms allows 3.92 frames, which rounds up to all 4 source frames. With a skip
    // ratio of 1, every source frame therefore maps to itself.
    val result =
        compressor.calculateReducedIndexes(
            durationMs = 560,
            frameCount = 4,
            targetFps = 7,
        )

    assertThat(result).isEqualTo(mapOf(0 to 0, 1 to 1, 2 to 2, 3 to 3))
  }

  @Test
  fun calculateReducedIndexes_roundUpDisabled_preservesExistingMapping() {
    val compressor = FpsCompressorInfo(maxFpsLimit = 30)

    // With rounding disabled, the 3.92-frame budget retains the existing behavior: source frame 1
    // reuses frame 0.
    val result =
        compressor.calculateReducedIndexes(
            durationMs = 560,
            frameCount = 4,
            targetFps = 7,
        )

    assertThat(result).isEqualTo(mapOf(0 to 0, 1 to 0, 2 to 2, 3 to 3))
  }

  @Test
  fun calculateReducedIndexes_fractionBelowThreshold_preservesExistingMapping() {
    val compressor = FpsCompressorInfo(maxFpsLimit = 30, shouldRoundUpFractionalFrameBudget = true)

    // 8 FPS over 400 ms allows 3.2 frames. The 0.2 fraction is below the rounding threshold.
    val result =
        compressor.calculateReducedIndexes(
            durationMs = 400,
            frameCount = 4,
            targetFps = 8,
        )

    assertThat(result).isEqualTo(mapOf(0 to 0, 1 to 0, 2 to 2, 3 to 3))
  }

  @Test
  fun calculateReducedIndexes_halfFrameFraction_preservesExistingMapping() {
    val compressor = FpsCompressorInfo(maxFpsLimit = 30, shouldRoundUpFractionalFrameBudget = true)

    // The threshold is strict, so a 3.5-frame budget is not rounded up.
    val result =
        compressor.calculateReducedIndexes(
            durationMs = 500,
            frameCount = 4,
            targetFps = 7,
        )

    assertThat(result).isEqualTo(mapOf(0 to 0, 1 to 0, 2 to 2, 3 to 3))
  }

  @Test
  fun calculateReducedIndexes_effectivelyWholeBudget_doesNotAddFrame() {
    val compressor = FpsCompressorInfo(maxFpsLimit = 30, shouldRoundUpFractionalFrameBudget = true)

    // Float precision makes this nominally 15-frame budget slightly larger than 15.
    val result =
        compressor.calculateReducedIndexes(
            durationMs = 600,
            frameCount = 16,
            targetFps = 25,
        )

    assertThat(result.values.toSet()).hasSize(15)
  }

  @Test
  fun calculateReducedIndexes_genuineCompression_reducesFrameCount() {
    val compressor = FpsCompressorInfo(maxFpsLimit = 30, shouldRoundUpFractionalFrameBudget = true)

    // Compressing 60 source frames over one second to 30 FPS allows 30 frames. The resulting
    // skip ratio of 2 retains every even-indexed source frame.
    val result =
        compressor.calculateReducedIndexes(
            durationMs = 1000,
            frameCount = 60,
            targetFps = 30,
        )

    assertThat(result.values.toSet()).isEqualTo((0 until 60 step 2).toSet())
  }

  @Test
  fun calculateReducedIndexes_targetAboveLimit_honorsMaximumFps() {
    val compressor = FpsCompressorInfo(maxFpsLimit = 30, shouldRoundUpFractionalFrameBudget = true)

    // The requested 60 FPS is capped at 30 FPS, so only 30 of the 60 one-second source frames
    // are allowed and the even-indexed frames are retained.
    val result =
        compressor.calculateReducedIndexes(
            durationMs = 1000,
            frameCount = 60,
            targetFps = 60,
        )

    assertThat(result.values.toSet()).isEqualTo((0 until 60 step 2).toSet())
  }
}
