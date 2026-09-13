/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PartialResultThrottleTest {

  @Test
  fun noneAllowsEveryPartialResult() {
    assertThat(PartialResultThrottle.NONE.allowsPropagationAt(0, 0)).isTrue()
    assertThat(PartialResultThrottle.NONE.allowsPropagationAt(2, 1)).isTrue()
    // A producer that reads a second response body without resetting the count still propagates
    // everything, which is what a fetcher setting no throttle is entitled to.
    assertThat(PartialResultThrottle.NONE.allowsPropagationAt(16_384, 300_000)).isTrue()
  }

  @Test
  fun aSizeBelowTheLastPropagatedOneRestartsAtTheInitialThreshold() {
    val throttle = PartialResultThrottle(100_000, 20_000, 0)

    assertThat(throttle.allowsPropagationAt(16_384, 300_000)).isFalse()
    assertThat(throttle.allowsPropagationAt(99_999, 300_000)).isFalse()
    assertThat(throttle.allowsPropagationAt(100_000, 300_000)).isTrue()
  }

  @Test
  fun theFirstPartialResultWaitsForTheInitialThreshold() {
    val throttle = PartialResultThrottle(100_000, 20_000, 0)

    assertThat(throttle.allowsPropagationAt(99_999, 0)).isFalse()
    assertThat(throttle.allowsPropagationAt(100_000, 0)).isTrue()
  }

  @Test
  fun laterPartialResultsStepFromTheSizeTheLastOneWentOutAt() {
    val throttle = PartialResultThrottle(100_000, 20_000, 0)

    assertThat(throttle.allowsPropagationAt(119_999, 100_000)).isFalse()
    assertThat(throttle.allowsPropagationAt(120_000, 100_000)).isTrue()
    // A read that overshoots to 150_000 puts the next threshold at 170_000, not at 120_000.
    assertThat(throttle.allowsPropagationAt(169_999, 150_000)).isFalse()
    assertThat(throttle.allowsPropagationAt(170_000, 150_000)).isTrue()
  }

  @Test
  fun aZeroIncrementOnlyDelaysTheFirstPartialResult() {
    val throttle = PartialResultThrottle(100_000, 0, 0)

    assertThat(throttle.allowsPropagationAt(99_999, 0)).isFalse()
    assertThat(throttle.allowsPropagationAt(100_000, 0)).isTrue()
    assertThat(throttle.allowsPropagationAt(100_001, 100_000)).isTrue()
  }

  @Test
  fun aZeroInitialSpacesEveryPartialResultButTheFirst() {
    val throttle = PartialResultThrottle(0, 20_000, 0)

    assertThat(throttle.allowsPropagationAt(1, 0)).isTrue()
    assertThat(throttle.allowsPropagationAt(20_000, 1)).isFalse()
    assertThat(throttle.allowsPropagationAt(20_001, 1)).isTrue()
  }

  @Test
  fun anIncrementNearTheIntegerLimitDoesNotOverflowIntoPropagatingEverything() {
    val throttle = PartialResultThrottle(0, Int.MAX_VALUE, 0)

    assertThat(throttle.allowsPropagationAt(1, 0)).isTrue()
    assertThat(throttle.allowsPropagationAt(Int.MAX_VALUE, 1)).isFalse()
  }

  @Test
  fun aZeroMinIntervalKeepsTheIntervalTheResponseWouldHaveHad() {
    assertThat(PartialResultThrottle(100_000, 20_000, 0).minIntervalMsOr(100L)).isEqualTo(100L)
    assertThat(PartialResultThrottle(0, 0, 500L).minIntervalMsOr(100L)).isEqualTo(500L)
    // Shorter than the fallback as well as longer: the throttle replaces it, it does not raise it.
    assertThat(PartialResultThrottle(0, 0, 20L).minIntervalMsOr(100L)).isEqualTo(20L)
  }
}
