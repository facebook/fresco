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

import com.facebook.imagepipeline.testing.FakeNativeMemoryChunk;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunkPool;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link NativeMemoryChunkPool}
 */
@RunWith(RobolectricTestRunner.class)
public class NativeMemoryChunkPoolTest extends TestUsingNativeMemoryChunk {
  private NativeMemoryChunkPool mPool;

  @Before
  public void setup() {
    final SparseIntArray bucketSizes = new SparseIntArray();
    bucketSizes.put(32, 2);
    bucketSizes.put(64, 1);
    bucketSizes.put(128, 1);
    mPool = new FakeNativeMemoryChunkPool(
        new PoolParams(128, bucketSizes));
  }

  // Test out the alloc method
  @Test
  public void testAlloc() throws Exception {
    NativeMemoryChunk c = mPool.alloc(1);
    Assert.assertNotNull(c);
    Assert.assertEquals(1, c.getSize());
    Assert.assertEquals(1, mPool.alloc(1).getSize());
    Assert.assertEquals(33, mPool.alloc(33).getSize());
    Assert.assertEquals(32, mPool.alloc(32).getSize());
  }

  @Test
  public void testFree() throws Exception {
    NativeMemoryChunk c = mPool.alloc(1);
    Assert.assertFalse(c.isClosed());
    mPool.free(c);
    Assert.assertTrue(c.isClosed());
    mPool.free(c);
    Assert.assertTrue(c.isClosed());
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
    Assert.assertEquals(
        32,
        mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(32)));
    Assert.assertEquals(
        64,
        mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(64)));
    Assert.assertEquals(
        128,
        mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(128)));

    // test with non-bucket values
    Assert.assertEquals(
        1,
        mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(1)));
    Assert.assertEquals(
        129,
        mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(129)));
    Assert.assertEquals(
        31,
        mPool.getBucketedSizeForValue(new FakeNativeMemoryChunk(31)));
  }

  @Test
  public void testGetSizeInBytes() throws Exception {
    Assert.assertEquals(1, mPool.getSizeInBytes(1));
    Assert.assertEquals(32, mPool.getSizeInBytes(32));
    Assert.assertEquals(33, mPool.getSizeInBytes(33));
    Assert.assertEquals(64, mPool.getSizeInBytes(64));
    Assert.assertEquals(69, mPool.getSizeInBytes(69));
  }

  @Test
  public void testIsReusable() throws Exception {
    NativeMemoryChunk chunk = mPool.get(1);
    Assert.assertTrue(mPool.isReusable(chunk));
    chunk.close();
    Assert.assertFalse(mPool.isReusable(chunk));
  }
}
