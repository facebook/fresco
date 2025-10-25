/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import android.util.SparseIntArray;
import com.facebook.imagepipeline.testing.FakeAshmemMemoryChunk;
import com.facebook.imagepipeline.testing.FakeAshmemMemoryChunkPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link AshmemMemoryChunkPool} */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AshmemMemoryChunkPoolTest {
  private AshmemMemoryChunkPool mPool;

  @Before
  public void setup() {
    final SparseIntArray bucketSizes = new SparseIntArray();
    bucketSizes.put(32, 2);
    bucketSizes.put(64, 1);
    bucketSizes.put(128, 1);
    mPool = new FakeAshmemMemoryChunkPool(new PoolParams(bucketSizes));
  }

  @Test
  public void testAlloc() {
    AshmemMemoryChunk b = mPool.alloc(1);
    assertThat(b).isNotNull();
    assertThat(b.getSize()).isEqualTo(1);
    assertThat(mPool.alloc(1).getSize()).isEqualTo(1);
    assertThat(mPool.alloc(33).getSize()).isEqualTo(33);
    assertThat(mPool.alloc(32).getSize()).isEqualTo(32);
  }

  @Test
  public void testFree() {
    AshmemMemoryChunk b = mPool.alloc(1);
    assertThat(b.isClosed()).isFalse();
    mPool.free(b);
    assertThat(b.isClosed()).isTrue();
    mPool.free(b);
    assertThat(b.isClosed()).isTrue();
  }

  @Test
  public void testGetBucketedSize() {
    assertThat(mPool.getBucketedSize(1)).isEqualTo(32);
    assertThat(mPool.getBucketedSize(32)).isEqualTo(32);
    assertThat(mPool.getBucketedSize(33)).isEqualTo(64);
    assertThat(mPool.getBucketedSize(63)).isEqualTo(64);
    assertThat(mPool.getBucketedSize(64)).isEqualTo(64);
    assertThat(mPool.getBucketedSize(69)).isEqualTo(128);

    assertThat(mPool.getBucketedSize(164)).isEqualTo(164);
    int[] invalidSizes = new int[] {-1, -2, 0};
    for (int size : invalidSizes) {
      assertThatThrownBy(() -> mPool.getBucketedSize(size))
          .isInstanceOf(BasePool.InvalidSizeException.class);
    }
  }

  @Test
  public void testGetBucketedSizeForValue() {
    assertThat(mPool.getBucketedSizeForValue(new FakeAshmemMemoryChunk(32))).isEqualTo(32);
    assertThat(mPool.getBucketedSizeForValue(new FakeAshmemMemoryChunk(64))).isEqualTo(64);
    assertThat(mPool.getBucketedSizeForValue(new FakeAshmemMemoryChunk(128))).isEqualTo(128);

    // Non-bucket values
    assertThat(mPool.getBucketedSizeForValue(new FakeAshmemMemoryChunk(1))).isEqualTo(1);
    assertThat(mPool.getBucketedSizeForValue(new FakeAshmemMemoryChunk(31))).isEqualTo(31);
    assertThat(mPool.getBucketedSizeForValue(new FakeAshmemMemoryChunk(164))).isEqualTo(164);
  }

  @Test
  public void testGetSizeInBytes() {
    assertThat(mPool.getSizeInBytes(1)).isEqualTo(1);
    assertThat(mPool.getSizeInBytes(31)).isEqualTo(31);
    assertThat(mPool.getSizeInBytes(32)).isEqualTo(32);
    assertThat(mPool.getSizeInBytes(64)).isEqualTo(64);
    assertThat(mPool.getSizeInBytes(120)).isEqualTo(120);
  }

  @Test
  public void testisReusable() {
    MemoryChunk chunk = mPool.get(1);
    assertThat(mPool.isReusable(chunk)).isTrue();
    chunk.close();
    assertThat(mPool.isReusable(chunk)).isFalse();
  }
}
