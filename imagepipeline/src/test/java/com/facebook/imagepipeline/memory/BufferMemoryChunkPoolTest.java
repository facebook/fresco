/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import android.util.SparseIntArray;
import com.facebook.imagepipeline.testing.FakeBufferMemoryChunkPool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link BufferMemoryChunkPool} */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BufferMemoryChunkPoolTest {
  private BufferMemoryChunkPool mPool;

  @Before
  public void setup() {
    final SparseIntArray bucketSizes = new SparseIntArray();
    bucketSizes.put(32, 2);
    bucketSizes.put(64, 1);
    bucketSizes.put(128, 1);
    mPool = new FakeBufferMemoryChunkPool(new PoolParams(128, bucketSizes));
  }

  @Test
  public void testAlloc() {
    BufferMemoryChunk b = mPool.alloc(1);
    Assert.assertNotNull(b);
    Assert.assertEquals(1, b.getSize());
    Assert.assertEquals(1, mPool.alloc(1).getSize());
    Assert.assertEquals(33, mPool.alloc(33).getSize());
    Assert.assertEquals(32, mPool.alloc(32).getSize());
  }

  @Test
  public void testFree() {
    BufferMemoryChunk b = new BufferMemoryChunk(1);
    Assert.assertFalse(b.isClosed());
    mPool.free(b);
    Assert.assertTrue(b.isClosed());
    mPool.free(b);
    Assert.assertTrue(b.isClosed());
  }

  @Test
  public void testGetBucketedSize() {
    Assert.assertEquals(32, mPool.getBucketedSize(1));
    Assert.assertEquals(32, mPool.getBucketedSize(32));
    Assert.assertEquals(64, mPool.getBucketedSize(33));
    Assert.assertEquals(64, mPool.getBucketedSize(63));
    Assert.assertEquals(64, mPool.getBucketedSize(64));
    Assert.assertEquals(128, mPool.getBucketedSize(69));

    Assert.assertEquals(164, mPool.getBucketedSize(164));
    int[] invalidSizes = new int[] {-1, -2, 0};
    for (int size : invalidSizes) {
      try {
        mPool.getBucketedSize(size);
        Assert.fail();
      } catch (BasePool.InvalidSizeException e) {
        // all good
      }
    }
  }

  @Test
  public void testGetBucketedSizeForValue() {
    Assert.assertEquals(32, mPool.getBucketedSizeForValue(new BufferMemoryChunk(32)));
    Assert.assertEquals(64, mPool.getBucketedSizeForValue(new BufferMemoryChunk(64)));
    Assert.assertEquals(128, mPool.getBucketedSizeForValue(new BufferMemoryChunk(128)));

    // Non-bucket values
    Assert.assertEquals(1, mPool.getBucketedSizeForValue(new BufferMemoryChunk(1)));
    Assert.assertEquals(31, mPool.getBucketedSizeForValue(new BufferMemoryChunk(31)));
    Assert.assertEquals(164, mPool.getBucketedSizeForValue(new BufferMemoryChunk(164)));
  }

  @Test
  public void testGetSizeInBytes() {
    Assert.assertEquals(1, mPool.getSizeInBytes(1));
    Assert.assertEquals(31, mPool.getSizeInBytes(31));
    Assert.assertEquals(32, mPool.getSizeInBytes(32));
    Assert.assertEquals(64, mPool.getSizeInBytes(64));
    Assert.assertEquals(120, mPool.getSizeInBytes(120));
  }

  @Test
  public void testisReusable() {
    MemoryChunk chunk = mPool.get(1);
    Assert.assertTrue(mPool.isReusable(chunk));
    chunk.close();
    Assert.assertFalse(mPool.isReusable(chunk));
  }
}
