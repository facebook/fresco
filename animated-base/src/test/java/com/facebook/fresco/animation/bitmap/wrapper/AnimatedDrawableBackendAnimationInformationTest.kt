/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.wrapper

import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests [AnimatedDrawableBackendAnimationInformation]. */
class AnimatedDrawableBackendAnimationInformationTest {
  private lateinit var animatedDrawableBackend: AnimatedDrawableBackend
  private lateinit var animatedDrawableBackendAnimationInformation:
      AnimatedDrawableBackendAnimationInformation

  @Before
  fun setup() {
    animatedDrawableBackend = mock()
    animatedDrawableBackendAnimationInformation =
        AnimatedDrawableBackendAnimationInformation(animatedDrawableBackend)
  }

  @Test
  @Throws(Exception::class)
  fun testGetFrameCount() {
    whenever(animatedDrawableBackend.getFrameCount()).thenReturn(123)

    Assertions.assertThat(animatedDrawableBackendAnimationInformation.getFrameCount())
        .isEqualTo(123)
  }

  @Test
  @Throws(Exception::class)
  fun testGetFrameDurationMs() {
    whenever(animatedDrawableBackend.getDurationMsForFrame(1)).thenReturn(123)
    whenever(animatedDrawableBackend.getDurationMsForFrame(2)).thenReturn(200)

    Assertions.assertThat(animatedDrawableBackendAnimationInformation.getFrameDurationMs(1))
        .isEqualTo(123)
    Assertions.assertThat(animatedDrawableBackendAnimationInformation.getFrameDurationMs(2))
        .isEqualTo(200)
  }

  @Test
  @Throws(Exception::class)
  fun testGetLoopCount() {
    whenever(animatedDrawableBackend.getLoopCount()).thenReturn(123)
    Assertions.assertThat(animatedDrawableBackendAnimationInformation.getLoopCount()).isEqualTo(123)
  }

  @Test
  @Throws(Exception::class)
  fun testGetLoopCountInfinite() {
    whenever(animatedDrawableBackend.getLoopCount())
        .thenReturn(AnimationBackend.LOOP_COUNT_INFINITE)

    Assertions.assertThat(animatedDrawableBackendAnimationInformation.getLoopCount())
        .isEqualTo(AnimationBackend.LOOP_COUNT_INFINITE)
  }
}
