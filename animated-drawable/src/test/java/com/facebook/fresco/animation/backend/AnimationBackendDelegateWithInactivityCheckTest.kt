/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.backend

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestScheduledExecutorService
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

/** Tests [AnimationBackendDelegateWithInactivityCheck] */
class AnimationBackendDelegateWithInactivityCheckTest {

  private lateinit var animationBackendDelegateWithInactivityCheck:
      AnimationBackendDelegate<AnimationBackend>
  private lateinit var animationBackend: AnimationBackend
  private lateinit var inactivityListener:
      AnimationBackendDelegateWithInactivityCheck.InactivityListener
  private lateinit var parent: Drawable
  private lateinit var canvas: Canvas
  private lateinit var fakeClock: FakeClock
  private lateinit var testScheduledExecutorService: TestScheduledExecutorService

  @Before
  fun setup() {
    animationBackend = mock()
    inactivityListener = mock()
    parent = mock()
    canvas = mock()

    fakeClock = FakeClock()
    testScheduledExecutorService = TestScheduledExecutorService(fakeClock)

    animationBackendDelegateWithInactivityCheck =
        AnimationBackendDelegateWithInactivityCheck.createForBackend(
            animationBackend, inactivityListener, fakeClock, testScheduledExecutorService)
  }

  @Test
  fun testNotifyInactive() {
    verifyNoMoreInteractions(inactivityListener)
    animationBackendDelegateWithInactivityCheck.drawFrame(parent, canvas, 0)
    verifyNoMoreInteractions(inactivityListener)
    fakeClock.incrementBy(100)
    verifyNoMoreInteractions(inactivityListener)
    fakeClock.incrementBy(AnimationBackendDelegateWithInactivityCheck.INACTIVITY_THRESHOLD_MS)
    verify(inactivityListener).onInactive()
  }
}
