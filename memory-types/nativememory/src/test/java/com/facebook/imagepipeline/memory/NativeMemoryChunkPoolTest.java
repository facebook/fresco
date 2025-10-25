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
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunk;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunkPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link NativeMemoryChunkPool} */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NativeMemoryChunkPoolTest extends TestUsingNativeMemoryChunk {
  private NativeMemoryChunkPool mPool;

  @Before
  public void setup() {
    final SparseIntArray bucketSizes = new SparseIntArray();
    bucketSizes.put(32, 2);
    bucketSizes.put(64, 1);
    bucketSizes.put(128, 1);
    mPool = new FakeNativeMemoryChunkPool(new PoolParams(bucketSizes));
  }

  // Test out the alloc method
  @Test
  public void testAlloc() {
    NativeMemoryChunk c = mPool.alloc(1);
    assertThat(c).isNotNull();
    assertThat(c.getSize()).isEqualTo(1);
    assertThat(mPool.alloc(1).getSize()).isEqualTo(1);
    assertThat(mPool.alloc(33).getSize()).isEqualTo(33);
    assertThat(mPool.alloc(32).getSize()).isEqualTo(32);
  }

  @Test
  public void testFree() {
    NativeMemoryChunk c = mPool.alloc(1);
    assertThat(c.isClosed()).isFalse();
    mPool.free(c);
    assertThat(c.isClosed()).isTrue();
    mPool.free(c);
    assertThat(c.isClosed()).isTrue();
  }

  // tests out the getBucketedSize method
  @Test
  public void testGetBucketedSize() {
    assertThat(mPool.getBucketedSize(1)).isEqualTo(32);
    assertThat(mPool.getBucketedSize(32)).isEqualTo(32);
    assertThat(mPool.getBucketedSize(33)).isEqualTo(64);
    assertThat(mPool.getBucketedSize(64)).isEqualTo(64);
    assertThat(mPool.getBucketedSize(69)).isEqualTo(128);

    // value larger than max bucket
    assertThat(mPool.getBucketedSize(129)).isEqualTo(129);

    int[] invalidSizes = new int[] {-1, 0};
    for (int size : invalidSizes) {
      assertThatThrownBy(() -> mPool.getBucketedSize(size))
          .isInstanceOf(BasePool.InvalidSizeException.class);
    }
  }

  // tests out the getBucketedSizeForValue method
  @Test
  public void testGetBucketedSizeForValue() {
    assertThat(mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(32))).isEqualTo(32);
    assertThat(mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(64))).isEqualTo(64);
    assertThat(mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(128))).isEqualTo(128);

    // test with non-bucket values
    assertThat(mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(1))).isEqualTo(1);
    assertThat(mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(129))).isEqualTo(129);
    assertThat(mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(31))).isEqualTo(31);
  }

  @Test
  public void testGetSizeInBytes() {
    assertThat(mPool.getSizeInBytes(1)).isEqualTo(1);
    assertThat(mPool.getSizeInBytes(32)).isEqualTo(32);
    assertThat(mPool.getSizeInBytes(33)).isEqualTo(33);
    assertThat(mPool.getSizeInBytes(64)).isEqualTo(64);
    assertThat(mPool.getSizeInBytes(69)).isEqualTo(69);
  }

  @Test
  public void testIsReusable() {
    MemoryChunk chunk = mPool.get(1);
    assertThat(mPool.isReusable(chunk)).isTrue();
    chunk.close();
    assertThat(mPool.isReusable(chunk)).isFalse();
  }
}
