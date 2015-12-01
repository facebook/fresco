/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

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
 * Tests for {@link SharedByteArray}
 */
@RunWith(RobolectricTestRunner.class)
public class SharedByteArrayTest {

  private SharedByteArray mArray;

  @Before
  public void setup() {
    mArray = new SharedByteArray(
        mock(MemoryTrimmableRegistry.class),
        new PoolParams(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            null,
            4,
            16,
            1));
  }

  @Test
  public void testBasic() throws Exception {
    assertEquals(4, mArray.mMinByteArraySize);
    assertEquals(16, mArray.mMaxByteArraySize);
    assertNull(mArray.mByteArraySoftRef.get());
    assertEquals(1, mArray.mSemaphore.availablePermits());
  }

  @Test
  public void testGet() throws Exception {
    CloseableReference<byte[]> arrayRef = mArray.get(1);
    assertSame(mArray.mByteArraySoftRef.get(), arrayRef.get());
    assertEquals(4, arrayRef.get().length);
    assertEquals(0, mArray.mSemaphore.availablePermits());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetTooBigArray() {
    mArray.get(32);
  }

  @Test
  public void testRelease() throws Exception {
    mArray.get(4).close();
    assertEquals(1, mArray.mSemaphore.availablePermits());
  }

  @Test
  public void testGet_Realloc() {
    CloseableReference<byte[]> arrayRef = mArray.get(1);
    final byte[] smallArray = arrayRef.get();
    arrayRef.close();

    arrayRef = mArray.get(7);
    assertEquals(8, arrayRef.get().length);
    assertSame(mArray.mByteArraySoftRef.get(), arrayRef.get());
    assertNotSame(smallArray, arrayRef.get());
  }

  @Test
  public void testTrim() {
    mArray.get(7).close();
    assertEquals(8, mArray.mByteArraySoftRef.get().length);

    // now trim, and verify again
    mArray.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    assertNull(mArray.mByteArraySoftRef.get());
    assertEquals(1, mArray.mSemaphore.availablePermits());
  }

  @Test
  public void testTrimUnsuccessful() {
    CloseableReference<byte[]> arrayRef = mArray.get(7);
    mArray.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    assertSame(arrayRef.get(), mArray.mByteArraySoftRef.get());
    assertEquals(0, mArray.mSemaphore.availablePermits());
  }

  @Test
  public void testGetBucketedSize() throws Exception {
    assertEquals(4, mArray.getBucketedSize(1));
    assertEquals(4, mArray.getBucketedSize(2));
    assertEquals(4, mArray.getBucketedSize(3));
    assertEquals(4, mArray.getBucketedSize(4));
    assertEquals(8, mArray.getBucketedSize(5));
    assertEquals(8, mArray.getBucketedSize(6));
    assertEquals(8, mArray.getBucketedSize(7));
    assertEquals(8, mArray.getBucketedSize(8));
    assertEquals(16, mArray.getBucketedSize(9));
  }
}
