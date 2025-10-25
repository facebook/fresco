/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.util.SparseIntArray;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.references.CloseableReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link FlexByteArrayPool} */
@RunWith(RobolectricTestRunner.class)
public class FlexByteArrayPoolTest {

  private static final int MIN_BUFFER_SIZE = 4;
  private static final int MAX_BUFFER_SIZE = 16;
  private FlexByteArrayPool mPool;
  private FlexByteArrayPool.SoftRefByteArrayPool mDelegatePool;

  @Before
  public void setup() {
    SparseIntArray buckets = new SparseIntArray();
    for (int i = MIN_BUFFER_SIZE; i <= MAX_BUFFER_SIZE; i *= 2) {
      buckets.put(i, 3);
    }
    mPool =
        new FlexByteArrayPool(
            mock(MemoryTrimmableRegistry.class),
            new PoolParams(buckets, MIN_BUFFER_SIZE, MAX_BUFFER_SIZE, 1));
    mDelegatePool = mPool.getDelegatePool();
  }

  @Test
  public void testBasic() throws Exception {
    assertThat(mPool.getMinBufferSize()).isEqualTo(MIN_BUFFER_SIZE);
    assertThat(mDelegatePool.poolParams.maxBucketSize).isEqualTo(MAX_BUFFER_SIZE);
    assertThat(mDelegatePool.free.numBytes).isEqualTo(0);
  }

  @Test
  public void testGet() throws Exception {
    CloseableReference<byte[]> arrayRef = mPool.get(1);
    assertThat(mDelegatePool.free.numBytes).isEqualTo(0);
    assertThat(arrayRef.get().length).isEqualTo(MIN_BUFFER_SIZE);
  }

  @Test
  public void testGetTooBigArray() {
    assertThat(mPool.get(2 * MAX_BUFFER_SIZE).get().length).isEqualTo(2 * MAX_BUFFER_SIZE);
  }

  @Test
  public void testRelease() throws Exception {
    mPool.get(MIN_BUFFER_SIZE).close();
    assertThat(mDelegatePool.free.numBytes).isEqualTo(MIN_BUFFER_SIZE);
  }

  @Test
  public void testGet_Realloc() {
    CloseableReference<byte[]> arrayRef = mPool.get(1);
    final byte[] smallArray = arrayRef.get();
    arrayRef.close();

    arrayRef = mPool.get(7);
    assertThat(arrayRef.get().length).isEqualTo(8);
    assertThat(arrayRef.get()).isNotSameAs(smallArray);
  }

  @Test
  public void testTrim() {
    mPool.get(7).close();
    assertThat(mDelegatePool.getBucket(8).getFreeListSize()).isEqualTo(1);

    // now trim, and verify again
    mDelegatePool.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    assertThat(mDelegatePool.getBucket(8).getFreeListSize()).isEqualTo(0);
  }

  @Test
  public void testTrimUnsuccessful() {
    CloseableReference<byte[]> arrayRef = mPool.get(7);
    mDelegatePool.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    assertThat(arrayRef.get()).isNotNull();
  }

  @Test
  public void testGetBucketedSize() throws Exception {
    assertThat(mDelegatePool.getBucketedSize(1)).isEqualTo(MIN_BUFFER_SIZE);
    assertThat(mDelegatePool.getBucketedSize(2)).isEqualTo(MIN_BUFFER_SIZE);
    assertThat(mDelegatePool.getBucketedSize(3)).isEqualTo(MIN_BUFFER_SIZE);
    assertThat(mDelegatePool.getBucketedSize(4)).isEqualTo(MIN_BUFFER_SIZE);
    assertThat(mDelegatePool.getBucketedSize(5)).isEqualTo(8);
    assertThat(mDelegatePool.getBucketedSize(6)).isEqualTo(8);
    assertThat(mDelegatePool.getBucketedSize(7)).isEqualTo(8);
    assertThat(mDelegatePool.getBucketedSize(8)).isEqualTo(8);
    assertThat(mDelegatePool.getBucketedSize(9)).isEqualTo(16);
  }
}
