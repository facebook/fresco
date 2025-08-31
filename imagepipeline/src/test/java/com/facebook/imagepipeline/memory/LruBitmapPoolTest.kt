/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.graphics.Bitmap
import com.facebook.common.memory.MemoryTrimType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LruBitmapPoolTest {
  private lateinit var pool: LruBitmapPool

  @Before
  fun setup() {
    pool =
        Mockito.spy(
            LruBitmapPool(10 * 1024 * 1024, 1024 * 1024, NoOpPoolStatsTracker.getInstance(), null)
        )
  }

  @Test
  fun testBitmapIsReused() {
    val expected = Bitmap.createBitmap(128, 128, Bitmap.Config.RGB_565)
    pool.release(expected)

    val actual = pool.get(128 * 128 * 2)

    assertThat(actual).isSameAs(expected)
  }

  @Test
  fun testBitmapWasTrimmed() {
    val expected = Bitmap.createBitmap(128, 128, Bitmap.Config.RGB_565)
    pool.release(expected)

    val strategy = pool.mStrategy as LruBucketsPoolBackend<*>
    assertThat(strategy.valueCount()).isEqualTo(1)

    pool.trim(MemoryTrimType.OnAppBackgrounded)

    val actual = pool.get(128 * 128 * 2)

    assertThat(actual).isNotSameAs(expected)
    assertThat(strategy.valueCount()).isEqualTo(0)
  }

  @Test
  fun testUniqueObjects() {
    val one = Bitmap.createBitmap(4, 4, Bitmap.Config.RGB_565)
    pool.release(one)
    pool.release(one)
    pool.release(one)

    val strategy = pool.mStrategy as LruBucketsPoolBackend<*>
    assertThat(strategy.valueCount()).isEqualTo(1)
  }
}
