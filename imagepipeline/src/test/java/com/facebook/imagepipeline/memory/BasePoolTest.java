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
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for BasePool */
@RunWith(RobolectricTestRunner.class)
public class BasePoolTest {
  private TestPool mPool;
  private PoolStats<byte[]> mStats;

  @Before
  public void setup() {
    mPool = new TestPool();
    mStats = new PoolStats<byte[]>(mPool);
  }

  // Test out the alloc method
  @Test
  public void testAlloc() throws Exception {
    assertThat(mPool.alloc(1).length).isEqualTo(1);
    assertThat(mPool.alloc(3).length).isEqualTo(3);
    assertThat(mPool.alloc(2).length).isEqualTo(2);
  }

  @Test
  public void testFree() throws Exception {}

  // tests out the getBucketedSize method
  @Test
  public void testGetBucketedSize() throws Exception {
    assertThat(mPool.getBucketedSize(1)).isEqualTo(2);
    assertThat(mPool.getBucketedSize(2)).isEqualTo(2);
    assertThat(mPool.getBucketedSize(3)).isEqualTo(4);
    assertThat(mPool.getBucketedSize(6)).isEqualTo(6);
    assertThat(mPool.getBucketedSize(7)).isEqualTo(8);
  }

  // tests out the getBucketedSize method for invalid inputs
  @Test
  public void testGetBucketedSize_Invalid() throws Exception {
    int[] sizes = new int[] {-1, 0};
    for (int s : sizes) {
      final int size = s;
      assertThatThrownBy(() -> mPool.getBucketedSize(size))
          .isInstanceOf(BasePool.InvalidSizeException.class);
    }
  }

  // tests out the getBucketedSizeForValue method
  @Test
  public void testGetBucketedSizeForValue() throws Exception {
    assertThat(mPool.getBucketedSizeForValue(new byte[2])).isEqualTo(2);
    assertThat(mPool.getBucketedSizeForValue(new byte[3])).isEqualTo(3);
    assertThat(mPool.getBucketedSizeForValue(new byte[6])).isEqualTo(6);
  }

  @Test
  public void testGetSizeInBytes() throws Exception {
    assertThat(mPool.getSizeInBytes(1)).isEqualTo(1);
    assertThat(mPool.getSizeInBytes(2)).isEqualTo(2);
    assertThat(mPool.getSizeInBytes(3)).isEqualTo(3);
    assertThat(mPool.getSizeInBytes(5)).isEqualTo(5);
    assertThat(mPool.getSizeInBytes(4)).isEqualTo(4);
  }

  // Get via alloc
  @Test
  public void testGet_Alloc() throws Exception {
    // get a buffer - causes an alloc
    byte[] b1 = mPool.get(1);
    assertThat(b1).isNotNull();
    assertThat(b1.length).isEqualTo(2);
    assertThat(mPool.inUseValues.contains(b1)).isTrue();
    mStats.refresh();
    assertThat(mStats.mBucketStats).isEqualTo(ImmutableMap.of(2, new IntPair(1, 0)));
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mUsedBytes).isEqualTo(2);
    assertThat(mStats.mFreeCount).isEqualTo(0);
    assertThat(mStats.mUsedCount).isEqualTo(1);

    // release this buffer
    mPool.release(b1);
    assertThat(mPool.inUseValues.contains(b1)).isFalse();

    // get another buffer, but of a different size. No reuse possible
    byte[] b2 = mPool.get(3);
    assertThat(b2).isNotNull();
    assertThat(b2.length).isEqualTo(4);
    assertThat(mPool.inUseValues.contains(b2)).isTrue();
    mStats.refresh();
    assertThat(mStats.mBucketStats)
        .isEqualTo(
            ImmutableMap.of(
                2, new IntPair(0, 1),
                4, new IntPair(1, 0)));
    assertThat(mStats.mFreeBytes).isEqualTo(2);
    assertThat(mStats.mUsedBytes).isEqualTo(4);
    assertThat(mStats.mFreeCount).isEqualTo(1);
    assertThat(mStats.mUsedCount).isEqualTo(1);
  }

  // Tests that we can reuse a free buffer in the pool
  @Test
  public void testGet_Reuse() throws Exception {
    // get a buffer, and immediately release it
    byte[] b1 = mPool.get(1);
    mPool.release(b1);
    assertThat(b1).isNotNull();
    assertThat(b1.length).isEqualTo(2);
    mStats.refresh();
    assertThat(mStats.mBucketStats).isEqualTo(ImmutableMap.of(2, new IntPair(0, 1)));
    assertThat(mStats.mFreeBytes).isEqualTo(2);
    assertThat(mStats.mUsedBytes).isEqualTo(0);
    assertThat(mStats.mFreeCount).isEqualTo(1);
    assertThat(mStats.mUsedCount).isEqualTo(0);

    // get another buffer of the same size as above. We should be able to reuse it
    byte[] b2 = mPool.get(1);
    assertThat(b2).isNotNull();
    assertThat(b2.length).isEqualTo(2);
    assertThat(mPool.inUseValues.contains(b2)).isTrue();
    mStats.refresh();
    assertThat(mStats.mBucketStats).isEqualTo(ImmutableMap.of(2, new IntPair(1, 0)));
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mUsedBytes).isEqualTo(2);
    assertThat(mStats.mFreeCount).isEqualTo(0);
    assertThat(mStats.mUsedCount).isEqualTo(1);
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
    assertThat(mStats.mBucketStats).isEqualTo(ImmutableMap.of(2, new IntPair(0, 1)));
    assertThat(mStats.mFreeBytes).isEqualTo(2);
    assertThat(mStats.mUsedBytes).isEqualTo(0);
    assertThat(mStats.mFreeCount).isEqualTo(1);
    assertThat(mStats.mUsedCount).isEqualTo(0);
  }

  // Test release with bucket length constraints
  @Test
  public void testRelease_BucketLengths() throws Exception {
    mPool = new TestPool(makeBucketSizeArray(2, 2));
    mStats.setPool(mPool);

    byte[] b0 = mPool.get(2);
    mPool.get(2);
    mPool.get(2);
    mStats.refresh();
    assertThat(mStats.mBucketStats).isEqualTo(ImmutableMap.of(2, new IntPair(3, 0)));
    assertThat(mStats.mUsedBytes).isEqualTo(6);
    assertThat(mStats.mUsedCount).isEqualTo(3);
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mFreeCount).isEqualTo(0);

    // now release one of the buffers
    mPool.release(b0);

    mStats.refresh();
    assertThat(mStats.mBucketStats).isEqualTo(ImmutableMap.of(2, new IntPair(2, 0)));
    assertThat(mStats.mUsedBytes).isEqualTo(4);
    assertThat(mStats.mUsedCount).isEqualTo(2);
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mFreeCount).isEqualTo(0);
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
    assertThat(mStats.mBucketStats).isEqualTo(ImmutableMap.of(2, new IntPair(1, 0)));
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mUsedBytes).isEqualTo(2);
    assertThat(mStats.mFreeCount).isEqualTo(0);
    assertThat(mStats.mUsedCount).isEqualTo(1);
  }

  // test out release with non reusable values
  @Test
  public void testRelease_NonReusable() throws Exception {
    TestPool pool = new TestPool(makeBucketSizeArray(2, 3));
    mPool.mIsReusable = false;
    mStats.setPool(pool);

    // get a buffer, and then release it
    byte[] b1 = mPool.get(2);
    mPool.release(b1);

    // verify stats
    mStats.refresh();
    assertThat(mStats.mBucketStats).isEqualTo(ImmutableMap.of(2, new IntPair(0, 0)));
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mUsedBytes).isEqualTo(0);
    assertThat(mStats.mFreeCount).isEqualTo(0);
    assertThat(mStats.mUsedCount).isEqualTo(0);
  }

  // test buffers outside the 'normal' bucket sizes
  @Test
  public void testGetRelease_NonBucketSizes() throws Exception {
    mPool = new TestPool(makeBucketSizeArray(2, 1, 4, 1, 6, 1));
    mStats.setPool(mPool);

    mPool.get(2);
    byte[] b1 = mPool.get(7);
    mStats.refresh();
    assertThat(mStats.mUsedBytes).isEqualTo(10);
    assertThat(mStats.mUsedCount).isEqualTo(2);
    mPool.release(b1);
    mStats.refresh();
    assertThat(mStats.mUsedBytes).isEqualTo(2);
    assertThat(mStats.mUsedCount).isEqualTo(1);
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mFreeCount).isEqualTo(0);

    byte[] b2 = new byte[3];
    mPool.release(b2);
    mStats.refresh();
    assertThat(mStats.mUsedBytes).isEqualTo(2);
    assertThat(mStats.mUsedCount).isEqualTo(1);
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mFreeCount).isEqualTo(0);
  }

  // test illegal arguments to get
  @Test
  public void testGetWithErrors() throws Exception {
    int[] sizes = new int[] {-1, 0};
    for (int s : sizes) {
      final int size = s;
      assertThatThrownBy(() -> mPool.get(size)).isInstanceOf(BasePool.InvalidSizeException.class);
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
    assertThat(mStats.mFreeBytes).isEqualTo(2);
    assertThat(mStats.mUsedBytes).isEqualTo(4);

    // trim the pool and check
    mPool.trimToNothing();
    mStats.refresh();
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mUsedBytes).isEqualTo(4);
  }

  // test out trimToSize functionality
  @Test
  public void testTrimToSize() throws Exception {
    mPool = new TestPool(makeBucketSizeArray(2, 2, 4, 2, 6, 2));
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
    assertThat(mStats.mFreeBytes).isEqualTo(12);
    assertThat(mStats.mUsedBytes).isEqualTo(2);

    // perform a dummy trim - nothing should happen
    mPool.trimToSize(100);
    mStats.refresh();
    assertThat(mStats.mFreeBytes).isEqualTo(12);
    assertThat(mStats.mUsedBytes).isEqualTo(2);

    // now perform the real trim
    mPool.trimToSize(8);
    mStats.refresh();
    assertThat(mStats.mFreeBytes).isEqualTo(6);
    assertThat(mStats.mUsedBytes).isEqualTo(2);
    assertThat(mStats.mBucketStats)
        .isEqualTo(
            ImmutableMap.of(
                2, new IntPair(1, 0),
                4, new IntPair(0, 0),
                6, new IntPair(0, 1)));

    // perform another trim
    mPool.trimToSize(1);
    mStats.refresh();
    assertThat(mStats.mFreeBytes).isEqualTo(0);
    assertThat(mStats.mUsedBytes).isEqualTo(2);
    assertThat(mStats.mBucketStats)
        .isEqualTo(
            ImmutableMap.of(
                2, new IntPair(1, 0),
                4, new IntPair(0, 0),
                6, new IntPair(0, 0)));
  }

  /**
   * A simple test pool that allocates byte arrays, and always allocates buffers of double the size
   * requested
   */
  public static class TestPool extends BasePool<byte[]> {
    public boolean mIsReusable;

    public TestPool() {
      this(null);
    }

    public TestPool(SparseIntArray bucketSizes) {
      super(
          mock(MemoryTrimmableRegistry.class),
          new PoolParams(bucketSizes),
          mock(PoolStatsTracker.class));
      mIsReusable = true;
      initialize();
    }

    @Override
    protected byte[] alloc(int bucketedSize) {
      return new byte[bucketedSize];
    }

    @Override
    protected void free(byte[] value) {}

    @Override
    protected boolean isReusable(byte[] value) {
      return mIsReusable;
    }

    /**
     * Allocate the smallest even number than is greater than or equal to the requested size
     *
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
