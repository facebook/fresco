/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.Rect
import com.facebook.common.internal.Supplier
import com.facebook.fresco.vito.core.impl.FrescoDrawable2
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LightweightDebugOverlayFactory2Test {
  private val drawable: FrescoDrawable2 = mock()
  private val overlay = LightweightDebugOverlayDrawable()

  @Before
  fun setup() {
    whenever(drawable.overlayDrawable).thenReturn(overlay)
    whenever(drawable.bounds).thenReturn(Rect(0, 0, 200, 100))
  }

  @Test
  fun testUpdate_withGainmap_showsHdrYes() {
    whenever(drawable.hasBitmapWithGainmap()).thenReturn(true)

    LightweightDebugOverlayFactory2(Supplier { true }).update(drawable, null)

    assertThat(debugText()).startsWith("HDR y | ")
  }

  @Test
  fun testUpdate_withoutGainmap_showsHdrNo() {
    LightweightDebugOverlayFactory2(Supplier { true }).update(drawable, null)

    assertThat(debugText()).startsWith("HDR n | ")
  }

  @Test
  fun testUpdate_withHdrHidden_omitsIndicator() {
    LightweightDebugOverlayFactory2(
        Supplier { true },
        LightweightDebugOverlayConfig(showHdr = false),
    )
        .update(drawable, null)

    assertThat(debugText()).doesNotContain("HDR ")
  }

  private fun debugText(): String {
    val field = LightweightDebugOverlayDrawable::class.java.getDeclaredField("debugText")
    field.isAccessible = true
    return field.get(overlay) as String
  }
}
