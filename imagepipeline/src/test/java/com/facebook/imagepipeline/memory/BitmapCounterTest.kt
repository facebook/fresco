/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory

import android.graphics.Bitmap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapCounterTest {
  private var bitmapCounter: BitmapCounter? = null

  @Before
  fun setUp() {
    bitmapCounter = BitmapCounter(MAX_COUNT, MAX_SIZE)
  }

  @Test
  fun testBasic() {
    assertState(0, 0)
    assertThat(bitmapCounter?.increase(bitmapForSize(1))).isTrue()
    assertState(1, 1)
    assertThat(bitmapCounter?.increase(bitmapForSize(2))).isTrue()
    assertState(2, 3)
    bitmapCounter?.decrease(bitmapForSize(1))
    assertState(1, 2)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testDecreaseTooMuch() {
    assertThat(bitmapCounter?.increase(bitmapForSize(1))).isTrue()
    bitmapCounter?.decrease(bitmapForSize(2))
  }

  @Test(expected = IllegalArgumentException::class)
  fun testDecreaseTooMany() {
    assertThat(bitmapCounter?.increase(bitmapForSize(2))).isTrue()
    bitmapCounter?.decrease(bitmapForSize(1))
    bitmapCounter?.decrease(bitmapForSize(1))
  }

  @Test
  fun testMaxSize() {
    assertThat(bitmapCounter?.increase(bitmapForSize(MAX_SIZE))).isTrue()
    assertState(1, MAX_SIZE.toLong())
  }

  @Test
  fun testMaxCount() {
    for (i in 0..<MAX_COUNT) {
      bitmapCounter?.increase(bitmapForSize(1))
    }
    assertState(MAX_COUNT, MAX_COUNT.toLong())
  }

  @Test
  fun increaseTooBigObject() {
    assertThat(bitmapCounter?.increase(bitmapForSize(MAX_SIZE + 1))).isFalse()
    assertState(0, 0)
  }

  @Test
  fun increaseTooManyObjects() {
    for (i in 0..<MAX_COUNT) {
      bitmapCounter?.increase(bitmapForSize(1))
    }
    assertThat(bitmapCounter?.increase(bitmapForSize(1))).isFalse()
    assertState(MAX_COUNT, MAX_COUNT.toLong())
  }

  private fun assertState(count: Int, size: Long) {
    assertThat(bitmapCounter?.getCount()?.toLong()).isEqualTo(count.toLong())
    assertThat(bitmapCounter?.getSize()).isEqualTo(size)
  }

  companion object {
    private const val MAX_COUNT = 4
    private val MAX_SIZE: Int = MAX_COUNT + 1

    private fun bitmapForSize(size: Int): Bitmap {
      val bitmap = Mockito.mock(Bitmap::class.java)
      Mockito.doReturn(1).`when`(bitmap).getHeight()
      Mockito.doReturn(size).`when`(bitmap).getRowBytes()
      Mockito.doReturn(size).`when`(bitmap).getByteCount()
      Mockito.doReturn(size).`when`(bitmap).getAllocationByteCount()
      return bitmap
    }
  }
}
