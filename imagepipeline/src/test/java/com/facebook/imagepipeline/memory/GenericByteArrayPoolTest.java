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

import com.facebook.common.memory.MemoryTrimmableRegistry;
import org.robolectric.RobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

/**
 * Basic tests for GenericByteArrayPool
 */
@RunWith(RobolectricTestRunner.class)
public class GenericByteArrayPoolTest {
  private GenericByteArrayPool mPool;

  @Before
  public void setup() {
    final SparseIntArray bucketSizes = new SparseIntArray();
    bucketSizes.put(32, 2);
    bucketSizes.put(64, 1);
    bucketSizes.put(128, 1);
    mPool = new GenericByteArrayPool(
        mock(MemoryTrimmableRegistry.class),
        new PoolParams(128, bucketSizes),
        mock(PoolStatsTracker.class));
  }

  // Test out the alloc method
  @Test
  public void testAlloc() throws Exception {
    Assert.assertEquals(1, mPool.alloc(1).length);
    Assert.assertEquals(33, mPool.alloc(33).length);
    Assert.assertEquals(32, mPool.alloc(32).length);
  }

  @Test
  public void testFree() throws Exception {
  }

  // tests out the getBucketedSize method
  @Test
  public void testGetBucketedSize() throws Exception {
    Assert.assertEquals(32, mPool.getBucketedSize(1));
    Assert.assertEquals(32, mPool.getBucketedSize(32));
    Assert.assertEquals(64, mPool.getBucketedSize(33));
    Assert.assertEquals(64, mPool.getBucketedSize(64));
    Assert.assertEquals(128, mPool.getBucketedSize(69));

    // value larger than max bucket
    Assert.assertEquals(129, mPool.getBucketedSize(129));

    int[] invalidSizes = new int[] {-1, 0};
    for (int size: invalidSizes) {
      try {
        mPool.getBucketedSize(size);
        Assert.fail();
      } catch (BasePool.InvalidSizeException e) {
        // do nothing
      }
    }
  }

  // tests out the getBucketedSizeForValue method
  @Test
  public void testGetBucketedSizeForValue() throws Exception {
    Assert.assertEquals(32, mPool.getBucketedSizeForValue(new byte[32]));
    Assert.assertEquals(64, mPool.getBucketedSizeForValue(new byte[64]));
    Assert.assertEquals(128, mPool.getBucketedSizeForValue(new byte[128]));

    // test with non-bucket values
    Assert.assertEquals(1, mPool.getBucketedSizeForValue(new byte[1]));
    Assert.assertEquals(129, mPool.getBucketedSizeForValue(new byte[129]));
    Assert.assertEquals(31, mPool.getBucketedSizeForValue(new byte[31]));
  }

  @Test
  public void testGetSizeInBytes() throws Exception {
    Assert.assertEquals(1, mPool.getSizeInBytes(1));
    Assert.assertEquals(32, mPool.getSizeInBytes(32));
    Assert.assertEquals(33, mPool.getSizeInBytes(33));
    Assert.assertEquals(64, mPool.getSizeInBytes(64));
    Assert.assertEquals(69, mPool.getSizeInBytes(69));
  }
}
