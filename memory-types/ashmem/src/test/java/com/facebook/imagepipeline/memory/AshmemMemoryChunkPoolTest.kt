/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.util.SparseIntArray
import com.facebook.imagepipeline.testing.FakeAshmemMemoryChunk
import com.facebook.imagepipeline.testing.FakeAshmemMemoryChunkPool
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [AshmemMemoryChunkPool] */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AshmemMemoryChunkPoolTest {
  private lateinit var pool: AshmemMemoryChunkPool

  @Before
  fun setup() {
    val bucketSizes = SparseIntArray()
    bucketSizes.put(32, 2)
    bucketSizes.put(64, 1)
    bucketSizes.put(128, 1)
    pool = FakeAshmemMemoryChunkPool(PoolParams(bucketSizes))
  }

  @Test
  fun testAlloc() {
    val b = pool.alloc(1)
    assertThat(b).isNotNull()
    assertThat(b.size).isEqualTo(1)
    assertThat(pool.alloc(1).size).isEqualTo(1)
    assertThat(pool.alloc(33).size).isEqualTo(33)
    assertThat(pool.alloc(32).size).isEqualTo(32)
  }

  @Test
  fun testFree() {
    val b = pool.alloc(1)
    assertThat(b.isClosed).isFalse()
    pool.free(b)
    assertThat(b.isClosed).isTrue()
    pool.free(b)
    assertThat(b.isClosed).isTrue()
  }

  @Test
  fun testGetBucketedSize() {
    assertThat(pool.getBucketedSize(1)).isEqualTo(32)
    assertThat(pool.getBucketedSize(32)).isEqualTo(32)
    assertThat(pool.getBucketedSize(33)).isEqualTo(64)
    assertThat(pool.getBucketedSize(63)).isEqualTo(64)
    assertThat(pool.getBucketedSize(64)).isEqualTo(64)
    assertThat(pool.getBucketedSize(69)).isEqualTo(128)

    assertThat(pool.getBucketedSize(164)).isEqualTo(164)
    val invalidSizes = intArrayOf(-1, -2, 0)
    for (size in invalidSizes) {
      assertThatThrownBy { pool.getBucketedSize(size) }
          .isInstanceOf(BasePool.InvalidSizeException::class.java)
    }
  }

  @Test
  fun testGetBucketedSizeForValue() {
    assertThat(pool.getBucketedSizeForValue(FakeAshmemMemoryChunk(32))).isEqualTo(32)
    assertThat(pool.getBucketedSizeForValue(FakeAshmemMemoryChunk(64))).isEqualTo(64)
    assertThat(pool.getBucketedSizeForValue(FakeAshmemMemoryChunk(128))).isEqualTo(128)

    // Non-bucket values
    assertThat(pool.getBucketedSizeForValue(FakeAshmemMemoryChunk(1))).isEqualTo(1)
    assertThat(pool.getBucketedSizeForValue(FakeAshmemMemoryChunk(31))).isEqualTo(31)
    assertThat(pool.getBucketedSizeForValue(FakeAshmemMemoryChunk(164))).isEqualTo(164)
  }

  @Test
  fun testGetSizeInBytes() {
    assertThat(pool.getSizeInBytes(1)).isEqualTo(1)
    assertThat(pool.getSizeInBytes(31)).isEqualTo(31)
    assertThat(pool.getSizeInBytes(32)).isEqualTo(32)
    assertThat(pool.getSizeInBytes(64)).isEqualTo(64)
    assertThat(pool.getSizeInBytes(120)).isEqualTo(120)
  }

  @Test
  fun testIsReusable() {
    val chunk = pool.get(1)
    assertThat(pool.isReusable(chunk)).isTrue()
    chunk.close()
    assertThat(pool.isReusable(chunk)).isFalse()
  }
}
