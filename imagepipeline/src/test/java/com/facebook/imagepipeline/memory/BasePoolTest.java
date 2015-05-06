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
import com.facebook.imagepipeline.memory.BasePool.PoolSizeViolationException;
import org.robolectric.RobolectricTestRunner;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

/**
 * Tests for BasePool
 */
@RunWith(RobolectricTestRunner.class)
public class BasePoolTest {
  private TestPool mPool;
  private PoolStats<byte[]> mStats;

  @Before
  public void setup() {
    mPool = new TestPool(10, 14);
    mStats = new PoolStats<byte[]>(mPool);
  }

  // Test out the alloc method
  @Test
  public void testAlloc() throws Exception {
    Assert.assertEquals(1, mPool.alloc(1).length);
    Assert.assertEquals(3, mPool.alloc(3).length);
    Assert.assertEquals(2, mPool.alloc(2).length);
  }

  @Test
  public void testFree() throws Exception {
  }

  // tests out the getBucketedSize method
  @Test
  public void testGetBucketedSize() throws Exception {
    Assert.assertEquals(2, mPool.getBucketedSize(1));
    Assert.assertEquals(2, mPool.getBucketedSize(2));
    Assert.assertEquals(4, mPool.getBucketedSize(3));
    Assert.assertEquals(6, mPool.getBucketedSize(6));
    Assert.assertEquals(8, mPool.getBucketedSize(7));
  }

  // tests out the getBucketedSize method for invalid inputs
  @Test
  public void testGetBucketedSize_Invalid() throws Exception {
    int[] sizes = new int[] {-1, 0};
    for (int s: sizes) {
      try {
        mPool.getBucketedSize(s);
        Assert.fail("Failed size: " + s);
      } catch (BasePool.InvalidSizeException e) {
        // do nothing
      }
    }
  }

  // tests out the getBucketedSizeForValue method
  @Test
  public void testGetBucketedSizeForValue() throws Exception {
    Assert.assertEquals(2, mPool.getBucketedSizeForValue(new byte[2]));
    Assert.assertEquals(3, mPool.getBucketedSizeForValue(new byte[3]));
    Assert.assertEquals(6, mPool.getBucketedSizeForValue(new byte[6]));
  }

  @Test
  public void testGetSizeInBytes() throws Exception {
    Assert.assertEquals(1, mPool.getSizeInBytes(1));
    Assert.assertEquals(2, mPool.getSizeInBytes(2));
    Assert.assertEquals(3, mPool.getSizeInBytes(3));
    Assert.assertEquals(5, mPool.getSizeInBytes(5));
    Assert.assertEquals(4, mPool.getSizeInBytes(4));
  }

  // Get via alloc
  @Test
  public void testGet_Alloc() throws Exception {
    // get a buffer - causes an alloc
    byte[] b1 = mPool.get(1);
    Assert.assertNotNull(b1);
    Assert.assertEquals(2, b1.length);
    Assert.assertTrue(mPool.mInUseValues.contains(b1));
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(2, new IntPair(1, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(2, mStats.mUsedBytes);
    Assert.assertEquals(0, mStats.mFreeCount);
    Assert.assertEquals(1, mStats.mUsedCount);

    // release this buffer
    mPool.release(b1);
    Assert.assertFalse(mPool.mInUseValues.contains(b1));

    // get another buffer, but of a different size. No reuse possible
    byte[] b2 = mPool.get(3);
    Assert.assertNotNull(b2);
    Assert.assertEquals(4, b2.length);
    Assert.assertTrue(mPool.mInUseValues.contains(b2));
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            2, new IntPair(0, 1),
            4, new IntPair(1, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(2, mStats.mFreeBytes);
    Assert.assertEquals(4, mStats.mUsedBytes);
    Assert.assertEquals(1, mStats.mFreeCount);
    Assert.assertEquals(1, mStats.mUsedCount);
  }

  // Get via alloc+trim
  @Test
  public void testGet_AllocAndTrim() throws Exception {
    mPool = new TestPool(10, 10, makeBucketSizeArray(2, 2, 4, 2, 6, 2));
    mStats.setPool(mPool);

    // allocate and release multiple buffers
    byte[] b1;
    b1 = mPool.get(2);
    mPool.release(b1);
    b1 = mPool.get(6);
    mPool.release(b1);

    // get current stats
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            2, new IntPair(0, 1),
            4, new IntPair(0, 0),
            6, new IntPair(0, 1)),
        mStats.mBucketStats);

    // get a new buffer; this should cause an alloc and a trim
    mPool.get(3);
    mStats.refresh();
    // validate stats
    Assert.assertEquals(
        ImmutableMap.of(
            2, new IntPair(0, 0),
            4, new IntPair(1, 0),
            6, new IntPair(0, 1)),
        mStats.mBucketStats);
    Assert.assertEquals(6, mStats.mFreeBytes);
    Assert.assertEquals(4, mStats.mUsedBytes);
    Assert.assertEquals(1, mStats.mFreeCount);
    Assert.assertEquals(1, mStats.mUsedCount);
  }

  // Tests that we can reuse a free buffer in the pool
  @Test
  public void testGet_Reuse() throws Exception {
    // get a buffer, and immediately release it
    byte[] b1 = mPool.get(1);
    mPool.release(b1);
    Assert.assertNotNull(b1);
    Assert.assertEquals(2, b1.length);
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(2, new IntPair(0, 1)),
        mStats.mBucketStats);
    Assert.assertEquals(2, mStats.mFreeBytes);
    Assert.assertEquals(0, mStats.mUsedBytes);
    Assert.assertEquals(1, mStats.mFreeCount);
    Assert.assertEquals(0, mStats.mUsedCount);

    // get another buffer of the same size as above. We should be able to reuse it
    byte[] b2 = mPool.get(1);
    Assert.assertNotNull(b2);
    Assert.assertEquals(2, b2.length);
    Assert.assertTrue(mPool.mInUseValues.contains(b2));
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(2, new IntPair(1, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(2, mStats.mUsedBytes);
    Assert.assertEquals(0, mStats.mFreeCount);
    Assert.assertEquals(1, mStats.mUsedCount);
  }

  // Get via alloc - exception on max size hard cap
  @Test
  public void testGet_AllocFailure() throws Exception {
    TestPool pool = new TestPool(4, 5);
    pool.get(4);
    try {
      pool.get(4);
      Assert.fail();
    } catch (PoolSizeViolationException e) {
      // expected exception
    }
  }

  // test a simple release
  @Test
  public void testRelease() throws Exception {
    // get a buffer - causes an alloc
    byte[] b1 = mPool.get(1);
    // release this buffer
    mPool.release(b1);

    // verify stats
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(2, new IntPair(0, 1)),
        mStats.mBucketStats);
    Assert.assertEquals(2, mStats.mFreeBytes);
    Assert.assertEquals(0, mStats.mUsedBytes);
    Assert.assertEquals(1, mStats.mFreeCount);
    Assert.assertEquals(0, mStats.mUsedCount);
  }

  // test out release(), when it should free the value, instead of adding to the pool
  @Test
  public void testRelease_Free() throws Exception {
    // get a set of buffers that bump up above the max size
    mPool.get(6);
    // get and release another buffer. this should cause a free
    byte[] b3 = mPool.get(6);
    mPool.release(b3);

    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(6, new IntPair(1, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(6, mStats.mUsedBytes);
    Assert.assertEquals(0, mStats.mFreeCount);
    Assert.assertEquals(1, mStats.mUsedCount);
  }

  // test release on  zero-sized pool
  @Test
  public void testRelease_Free2() throws Exception {
    // create a new pool with a max size cap of zero.
    mPool = new TestPool(0, 10);
    mStats.setPool(mPool);

    // get a buffer and release it - this should trigger the soft cap
    byte[] b1 = mPool.get(4);
    mPool.release(b1);

    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            4, new IntPair(0, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(0, mStats.mUsedBytes);
    Assert.assertEquals(0, mStats.mFreeCount);
    Assert.assertEquals(0, mStats.mUsedCount);
  }

  // Test release with bucket length constraints
  @Test
  public void testRelease_BucketLengths() throws Exception {
    mPool = new TestPool(Integer.MAX_VALUE, Integer.MAX_VALUE, makeBucketSizeArray(2, 2));
    mStats.setPool(mPool);

    byte[] b0 = mPool.get(2);
    mPool.get(2);
    mPool.get(2);
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            2, new IntPair(3, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(6, mStats.mUsedBytes);
    Assert.assertEquals(3, mStats.mUsedCount);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(0, mStats.mFreeCount);

    // now release one of the buffers
    mPool.release(b0);

    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            2, new IntPair(2, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(4, mStats.mUsedBytes);
    Assert.assertEquals(2, mStats.mUsedCount);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(0, mStats.mFreeCount);
  }

  // Test releasing an 'unknown' value
  @Test
  public void testRelease_UnknownValue() throws Exception {
    // get a buffer from the pool
    mPool.get(1);

    // allocate a buffer outside the pool
    byte[] b2 = new byte[2];
    // try to release this buffer to the pool
    mPool.release(b2);

    // verify stats
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(2, new IntPair(1, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(2, mStats.mUsedBytes);
    Assert.assertEquals(0, mStats.mFreeCount);
    Assert.assertEquals(1, mStats.mUsedCount);
  }

  // test out release with non reusable values
  @Test
  public void testRelease_NonReusable() throws Exception {
    TestPool pool = new TestPool(100, 100, makeBucketSizeArray(2, 3));
    mPool.mIsReusable = false;
    mStats.setPool(pool);

    // get a buffer, and then release it
    byte[] b1 = mPool.get(2);
    mPool.release(b1);

    // verify stats
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(2, new IntPair(0, 0)),
        mStats.mBucketStats);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(0, mStats.mUsedBytes);
    Assert.assertEquals(0, mStats.mFreeCount);
    Assert.assertEquals(0, mStats.mUsedCount);

  }

  // test buffers outside the 'normal' bucket sizes
  @Test
  public void testGetRelease_NonBucketSizes() throws Exception {
    mPool = new TestPool(10, 10, makeBucketSizeArray(2, 1, 4, 1, 6, 1));
    mStats.setPool(mPool);

    mPool.get(2);
    byte[] b1 = mPool.get(7);
    mStats.refresh();
    Assert.assertEquals(10, mStats.mUsedBytes);
    Assert.assertEquals(2, mStats.mUsedCount);
    mPool.release(b1);
    mStats.refresh();
    Assert.assertEquals(2, mStats.mUsedBytes);
    Assert.assertEquals(1, mStats.mUsedCount);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(0, mStats.mFreeCount);

    byte[] b2 = new byte[3];
    mPool.release(b2);
    mStats.refresh();
    Assert.assertEquals(2, mStats.mUsedBytes);
    Assert.assertEquals(1, mStats.mUsedCount);
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(0, mStats.mFreeCount);
  }

  // test illegal arguments to get
  @Test
  public void testGetWithErrors() throws Exception {
    int[] sizes = new int[] {-1, 0};
    for (int s: sizes) {
      try {
        mPool.get(s);
        Assert.fail("Failed size: " + s);
      } catch (BasePool.InvalidSizeException e) {
        // do nothing
      }
    }
  }

  // test out trimToNothing functionality
  @Test
  public void testTrimToNothing() throws Exception {
    // alloc a buffer and then release it
    byte[] b1 = mPool.get(1);
    mPool.release(b1);
    mPool.get(3);
    mStats.refresh();
    Assert.assertEquals(2, mStats.mFreeBytes);
    Assert.assertEquals(4, mStats.mUsedBytes);

    // trim the pool and check
    mPool.trimToNothing();
    mStats.refresh();
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(4, mStats.mUsedBytes);
  }

  // test out trimToSize functionality
  @Test
  public void testTrimToSize() throws Exception {
    mPool = new TestPool(100, 100, makeBucketSizeArray(2, 2, 4, 2, 6, 2));
    mStats.setPool(mPool);

    // allocate and release multiple buffers
    byte[] b1;
    mPool.get(2);
    b1 = mPool.get(2);
    mPool.release(b1);
    b1 = mPool.get(6);
    mPool.release(b1);
    b1 = mPool.get(4);
    mPool.release(b1);

    mStats.refresh();
    Assert.assertEquals(12, mStats.mFreeBytes);
    Assert.assertEquals(2, mStats.mUsedBytes);

    // perform a dummy trim - nothing should happen
    mPool.trimToSize(100);
    mStats.refresh();
    Assert.assertEquals(12, mStats.mFreeBytes);
    Assert.assertEquals(2, mStats.mUsedBytes);

    // now perform the real trim
    mPool.trimToSize(8);
    mStats.refresh();
    Assert.assertEquals(6, mStats.mFreeBytes);
    Assert.assertEquals(2, mStats.mUsedBytes);
    Assert.assertEquals(
        ImmutableMap.of(
            2, new IntPair(1, 0),
            4, new IntPair(0, 0),
            6, new IntPair(0, 1)),
        mStats.mBucketStats);

    // perform another trim
    mPool.trimToSize(1);
    mStats.refresh();
    Assert.assertEquals(0, mStats.mFreeBytes);
    Assert.assertEquals(2, mStats.mUsedBytes);
    Assert.assertEquals(
        ImmutableMap.of(
            2, new IntPair(1, 0),
            4, new IntPair(0, 0),
            6, new IntPair(0, 0)),
        mStats.mBucketStats);
  }

  @Test
  public void test_canAllocate() throws Exception {
    TestPool pool = new TestPool(4, 8);

    pool.get(4);
    Assert.assertFalse(pool.isMaxSizeSoftCapExceeded());
    Assert.assertTrue(pool.canAllocate(2));
    pool.get(2);
    Assert.assertTrue(pool.isMaxSizeSoftCapExceeded());
    Assert.assertTrue(pool.canAllocate(2));
    Assert.assertFalse(pool.canAllocate(4));
  }

  /**
   * A simple test pool that allocates byte arrays, and always allocates buffers of double
   * the size requested
   */
  public static class TestPool extends BasePool<byte[]> {
    public boolean mIsReusable;

    public TestPool(int maxPoolSizeSoftCap, int maxPoolSizeHardCap) {
      this(maxPoolSizeSoftCap, maxPoolSizeHardCap, null);
    }

    public TestPool(
        int maxPoolSizeSoftCap,
        int maxPoolSizeHardCap,
        SparseIntArray bucketSizes) {
      super(
          mock(MemoryTrimmableRegistry.class),
          new PoolParams(maxPoolSizeSoftCap, maxPoolSizeHardCap, bucketSizes),
          mock(PoolStatsTracker.class));
      mIsReusable = true;
      initialize();
    }

    @Override
    protected byte[] alloc(int bucketedSize) {
      return new byte[bucketedSize];
    }

    @Override
    protected void free(byte[] value) {
    }

    @Override
    protected boolean isReusable(byte[] value) {
      return mIsReusable;
    }

    /**
     * Allocate the smallest even number than is greater than or equal to the requested size
     * @param requestSize the logical request size
     * @return the slightly higher size
     */
    @Override
    protected int getBucketedSize(int requestSize) {
      if (requestSize <= 0) {
        throw new InvalidSizeException(requestSize);
      }

      return (requestSize % 2 == 0) ? requestSize : requestSize + 1;
    }

    @Override
    protected int getBucketedSizeForValue(byte[] value) {
      return value.length;
    }

    @Override
    protected int getSizeInBytes(int bucketedSize) {
      return bucketedSize;
    }
  }

  private static SparseIntArray makeBucketSizeArray(int... params) {
    Preconditions.checkArgument(params.length % 2 == 0);
    final SparseIntArray bucketSizes = new SparseIntArray();
    for (int i = 0; i < params.length; i += 2) {
      bucketSizes.append(params[i], params[i + 1]);
    }
    return bucketSizes;
  }
}

