/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.backend

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests [AnimationBackendDelegate] */
@RunWith(RobolectricTestRunner::class)
class AnimationBackendDelegateTest {

  private lateinit var animationBackendDelegate: AnimationBackendDelegate<AnimationBackend>
  private lateinit var animationBackend: AnimationBackend
  private lateinit var parent: Drawable
  private lateinit var canvas: Canvas

  @Before
  fun setup() {
    animationBackend = mock<AnimationBackend>()
    parent = mock<Drawable>()
    canvas = mock<Canvas>()

    animationBackendDelegate = AnimationBackendDelegate(animationBackend)
  }

  @Test
  fun testForwardProperties() {
    val colorFilter = mock<ColorFilter>()
    val bounds = mock<Rect>()
    val alphaValue = 123

    verifyNoMoreInteractions(animationBackend)

    // Set values to be persisted
    animationBackendDelegate.setAlpha(alphaValue)
    animationBackendDelegate.setColorFilter(colorFilter)
    animationBackendDelegate.setBounds(bounds)

    // Verify that values have been restored
    verify(animationBackend).setAlpha(alphaValue)
    verify(animationBackend).setColorFilter(colorFilter)
    verify(animationBackend).setBounds(bounds)
  }

  @Test
  fun testGetProperties() {
    val width = 123
    val height = 234
    val sizeInBytes = 2000
    val frameCount = 20
    val loopCount = 1000
    val frameDurationMs = 200

    whenever(animationBackend.intrinsicWidth).thenReturn(width)
    whenever(animationBackend.intrinsicHeight).thenReturn(height)
    whenever(animationBackend.sizeInBytes).thenReturn(sizeInBytes)
    whenever(animationBackend.frameCount).thenReturn(frameCount)
    whenever(animationBackend.loopCount).thenReturn(loopCount)
    whenever(animationBackend.getFrameDurationMs(any())).thenReturn(frameDurationMs)

    assertThat(animationBackendDelegate.intrinsicWidth).isEqualTo(width)
    assertThat(animationBackendDelegate.intrinsicHeight).isEqualTo(height)
    assertThat(animationBackendDelegate.sizeInBytes).isEqualTo(sizeInBytes)
    assertThat(animationBackendDelegate.frameCount).isEqualTo(frameCount)
    assertThat(animationBackendDelegate.loopCount).isEqualTo(loopCount)
    assertThat(animationBackendDelegate.getFrameDurationMs(1)).isEqualTo(frameDurationMs)
  }

  @Test
  fun testGetDefaultProperties() {
    // We don't set an animation backend
    animationBackendDelegate.animationBackend = null

    assertThat(animationBackendDelegate.intrinsicWidth)
        .isEqualTo(AnimationBackend.INTRINSIC_DIMENSION_UNSET)
    assertThat(animationBackendDelegate.intrinsicHeight)
        .isEqualTo(AnimationBackend.INTRINSIC_DIMENSION_UNSET)
    assertThat(animationBackendDelegate.sizeInBytes).isEqualTo(0)
    assertThat(animationBackendDelegate.frameCount).isEqualTo(0)
    assertThat(animationBackendDelegate.loopCount).isEqualTo(0)
    assertThat(animationBackendDelegate.getFrameDurationMs(1)).isEqualTo(0)
  }

  @Test
  fun testSetAnimationBackend() {
    val backend2 = mock<AnimationBackend>()
    val colorFilter = mock<ColorFilter>()
    val bounds = mock<Rect>()
    val alphaValue = 123

    verifyNoMoreInteractions(backend2)

    // Set values to be persisted
    animationBackendDelegate.setAlpha(alphaValue)
    animationBackendDelegate.setColorFilter(colorFilter)
    animationBackendDelegate.setBounds(bounds)

    animationBackendDelegate.animationBackend = backend2

    // Verify that values have been restored
    verify(backend2).setAlpha(alphaValue)
    verify(backend2).setColorFilter(colorFilter)
    verify(backend2).setBounds(bounds)
  }

  @Test
  fun testDrawFrame() {
    animationBackendDelegate.drawFrame(parent, canvas, 1)

    verify(animationBackend).drawFrame(parent, canvas, 1)
  }

  @Test
  fun testClear() {
    animationBackendDelegate.clear()

    verify(animationBackend).clear()
  }
}
