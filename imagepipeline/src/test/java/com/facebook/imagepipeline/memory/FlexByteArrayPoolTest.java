/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import android.util.SparseIntArray;

import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.references.CloseableReference;
import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FlexByteArrayPool}
 */
@RunWith(RobolectricTestRunner.class)
public class FlexByteArrayPoolTest {

  private static final int MIN_BUFFER_SIZE = 4;
  private static final int MAX_BUFFER_SIZE = 16;
  private FlexByteArrayPool mPool;
  private FlexByteArrayPool.SoftRefByteArrayPool mDelegatePool;

  @Before
  public void setup() {
    SparseIntArray buckets = new SparseIntArray();
    for (int i = MIN_BUFFER_SIZE; i <= MAX_BUFFER_SIZE; i*=2) {
      buckets.put(i, 3);
    }
    mPool = new FlexByteArrayPool(
        mock(MemoryTrimmableRegistry.class),
        new PoolParams(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            buckets,
            MIN_BUFFER_SIZE,
            MAX_BUFFER_SIZE,
            1));
    mDelegatePool = mPool.mDelegatePool;
  }

  @Test
  public void testBasic() throws Exception {
    assertEquals(MIN_BUFFER_SIZE, mPool.getMinBufferSize());
    assertEquals(MAX_BUFFER_SIZE, mDelegatePool.mPoolParams.maxBucketSize);
    assertEquals(0, mDelegatePool.mFree.mNumBytes);
  }

  @Test
  public void testGet() throws Exception {
    CloseableReference<byte[]> arrayRef = mPool.get(1);
    assertEquals(0, mDelegatePool.mFree.mNumBytes);
    assertEquals(MIN_BUFFER_SIZE, arrayRef.get().length);
  }

  @Test
  public void testGetTooBigArray() {
    assertEquals(2 * MAX_BUFFER_SIZE, mPool.get(2 * MAX_BUFFER_SIZE).get().length);
  }

  @Test
  public void testRelease() throws Exception {
    mPool.get(MIN_BUFFER_SIZE).close();
    assertEquals(MIN_BUFFER_SIZE, mDelegatePool.mFree.mNumBytes);
  }

  @Test
  public void testGet_Realloc() {
    CloseableReference<byte[]> arrayRef = mPool.get(1);
    final byte[] smallArray = arrayRef.get();
    arrayRef.close();

    arrayRef = mPool.get(7);
    assertEquals(8, arrayRef.get().length);
    assertNotSame(smallArray, arrayRef.get());
  }

  @Test
  public void testTrim() {
    mPool.get(7).close();
    assertEquals(1, mDelegatePool.getBucket(8).getFreeListSize());

    // now trim, and verify again
    mDelegatePool.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    assertEquals(0, mDelegatePool.getBucket(8).getFreeListSize());
  }

  @Test
  public void testTrimUnsuccessful() {
    CloseableReference<byte[]> arrayRef = mPool.get(7);
    mDelegatePool.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    assertNotNull(arrayRef.get());
  }

  @Test
  public void testGetBucketedSize() throws Exception {
    assertEquals(MIN_BUFFER_SIZE, mDelegatePool.getBucketedSize(1));
    assertEquals(MIN_BUFFER_SIZE, mDelegatePool.getBucketedSize(2));
    assertEquals(MIN_BUFFER_SIZE, mDelegatePool.getBucketedSize(3));
    assertEquals(MIN_BUFFER_SIZE, mDelegatePool.getBucketedSize(4));
    assertEquals(8, mDelegatePool.getBucketedSize(5));
    assertEquals(8, mDelegatePool.getBucketedSize(6));
    assertEquals(8, mDelegatePool.getBucketedSize(7));
    assertEquals(8, mDelegatePool.getBucketedSize(8));
    assertEquals(16, mDelegatePool.getBucketedSize(9));
  }
}
