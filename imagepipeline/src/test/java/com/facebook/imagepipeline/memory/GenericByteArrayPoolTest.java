/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import android.util.SparseIntArray;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Basic tests for GenericByteArrayPool */
@RunWith(RobolectricTestRunner.class)
public class GenericByteArrayPoolTest {
  private GenericByteArrayPool mPool;

  @Before
  public void setup() {
    final SparseIntArray bucketSizes = new SparseIntArray();
    bucketSizes.put(32, 2);
    bucketSizes.put(64, 1);
    bucketSizes.put(128, 1);
    mPool =
        new GenericByteArrayPool(
            mock(MemoryTrimmableRegistry.class),
            new PoolParams(bucketSizes),
            mock(PoolStatsTracker.class));
  }

  // Test out the alloc method
  @Test
  public void testAlloc() throws Exception {
    assertThat(mPool.alloc(1).length).isEqualTo(1);
    assertThat(mPool.alloc(33).length).isEqualTo(33);
    assertThat(mPool.alloc(32).length).isEqualTo(32);
  }

  @Test
  public void testFree() throws Exception {}

  // tests out the getBucketedSize method
  @Test
  public void testGetBucketedSize() throws Exception {
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
  public void testGetBucketedSizeForValue() throws Exception {
    assertThat(mPool.getBucketedSizeForValue(new byte[32])).isEqualTo(32);
    assertThat(mPool.getBucketedSizeForValue(new byte[64])).isEqualTo(64);
    assertThat(mPool.getBucketedSizeForValue(new byte[128])).isEqualTo(128);

    // test with non-bucket values
    assertThat(mPool.getBucketedSizeForValue(new byte[1])).isEqualTo(1);
    assertThat(mPool.getBucketedSizeForValue(new byte[129])).isEqualTo(129);
    assertThat(mPool.getBucketedSizeForValue(new byte[31])).isEqualTo(31);
  }

  @Test
  public void testGetSizeInBytes() throws Exception {
    assertThat(mPool.getSizeInBytes(1)).isEqualTo(1);
    assertThat(mPool.getSizeInBytes(32)).isEqualTo(32);
    assertThat(mPool.getSizeInBytes(33)).isEqualTo(33);
    assertThat(mPool.getSizeInBytes(64)).isEqualTo(64);
    assertThat(mPool.getSizeInBytes(69)).isEqualTo(69);
  }
}
