/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImagePerfStateTest {

  private lateinit var state: ImagePerfState

  @Before
  fun setup() {
    state = ImagePerfState(ImageRenderingInfra.VITO_V2)
  }

  @Test
  fun testSnapshot_includesPrefetchData() {
    val prefetch = PrefetchData(
        cacheKeyString = "test-key",
        startTimeMs = 100,
        endTimeMs = 200,
        origin = "network",
    )
    state.prefetchData = prefetch

    val snapshot = state.snapshot()

    assertEquals(prefetch, snapshot.prefetchData)
    assertEquals("test-key", snapshot.prefetchData?.cacheKeyString)
    assertEquals(100L, snapshot.prefetchData?.startTimeMs)
    assertEquals(200L, snapshot.prefetchData?.endTimeMs)
    assertEquals("network", snapshot.prefetchData?.origin)
  }

  @Test
  fun testSnapshot_prefetchDataNullByDefault() {
    val snapshot = state.snapshot()

    assertNull(snapshot.prefetchData)
  }

  @Test
  fun testReset_clearsPrefetchData() {
    state.prefetchData = PrefetchData(startTimeMs = 100, endTimeMs = 200, origin = "network")
    assertNotNull(state.prefetchData)

    state.reset()

    assertNull(state.prefetchData)
    assertNull(state.snapshot().prefetchData)
  }

  @Test
  fun testResetPointsTimestamps_doesNotClearPrefetchData() {
    val prefetch = PrefetchData(startTimeMs = 100, endTimeMs = 200, origin = "disk")
    state.prefetchData = prefetch

    state.resetPointsTimestamps()

    assertEquals(prefetch, state.prefetchData)
    assertEquals(prefetch, state.snapshot().prefetchData)
  }
}
