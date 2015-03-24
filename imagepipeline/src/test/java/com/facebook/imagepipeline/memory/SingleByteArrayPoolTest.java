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
import com.facebook.testing.robolectric.v2.WithTestDefaultsRunner;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SingleByteArrayPool}
 */
@RunWith(WithTestDefaultsRunner.class)
public class SingleByteArrayPoolTest {

  private SingleByteArrayPoolStatsTracker mStatsTracker;
  private SingleByteArrayPool mPool;

  @Before
  public void setup() {
    mStatsTracker = mock(SingleByteArrayPoolStatsTracker.class);
    mPool = new SingleByteArrayPool(mock(MemoryTrimmableRegistry.class), mStatsTracker, 4, 16);
  }

  @Test
  public void testBasic() throws Exception {
    Assert.assertEquals(4, mPool.mMinByteArraySize);
    Assert.assertEquals(16, mPool.mMaxByteArraySize);
    Assert.assertNull(mPool.mByteArraySoftRef.get());
  }

  @Test
  public void testGet() throws Exception {
    // test size-too-large
    try {
      mPool.get(32);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      // expected
      verify(mStatsTracker).onBucketedSizeRequested(32);
    }

    // test basic get
    byte[] buf = mPool.get(1);
    Assert.assertSame(mPool.mByteArraySoftRef.get(), buf);
    Assert.assertEquals(4, buf.length);
    verify(mStatsTracker).onBucketedSizeRequested(1);
    verify(mStatsTracker).onMemoryAlloc(4);

    // test in-use
    try {
      mPool.get(4);
      Assert.fail();
    } catch (Throwable t) {
      // expected
      verify(mStatsTracker, never()).onBucketedSizeRequested(4);
    }
  }

  @Test
  public void testGet_Realloc() throws Exception {
    byte[] buf = mPool.get(4);
    verify(mStatsTracker).onBucketedSizeRequested(4);
    verify(mStatsTracker).onMemoryAlloc(4);
    mPool.release(buf);

    byte[] buf2 = mPool.get(7);
    Assert.assertEquals(8, buf2.length);
    Assert.assertSame(mPool.mByteArraySoftRef.get(), buf2);
    Assert.assertNotSame(buf, buf2);
    verify(mStatsTracker).onBucketedSizeRequested(8);
    verify(mStatsTracker).onMemoryAlloc(8);
  }

  @Test
  public void testRelease() throws Exception {
    byte[] buf = mPool.get(4);
    verify(mStatsTracker).onBucketedSizeRequested(4);
    verify(mStatsTracker).onMemoryAlloc(4);
    mPool.release(buf);
    Assert.assertFalse(mPool.mInUse);
  }

  @Test
  public void testRelease_UnknownValue() throws Exception {
    mPool.get(4);
    verify(mStatsTracker).onBucketedSizeRequested(4);
    verify(mStatsTracker).onMemoryAlloc(4);
    mPool.release(new byte[4]);
    Assert.assertTrue(mPool.mInUse);
  }

  @Test
  public void testTrim() throws Exception {
    byte[] buf = mPool.get(7);
    verify(mStatsTracker).onBucketedSizeRequested(8);
    verify(mStatsTracker).onMemoryAlloc(8);
    mPool.release(buf);
    Assert.assertEquals(8, mPool.mByteArraySoftRef.get().length);

    // now trim, and verify again
    mPool.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    verify(mStatsTracker).onMemoryTrimmed(8);
    Assert.assertNull(mPool.mByteArraySoftRef.get());
  }

  @Test
  public void testGetBucketedSize() throws Exception {
    Assert.assertEquals(1, mPool.getBucketedSize(1));
    Assert.assertEquals(2, mPool.getBucketedSize(2));
    Assert.assertEquals(4, mPool.getBucketedSize(3));
    Assert.assertEquals(4, mPool.getBucketedSize(4));
    Assert.assertEquals(8, mPool.getBucketedSize(5));
    Assert.assertEquals(8, mPool.getBucketedSize(6));
    Assert.assertEquals(8, mPool.getBucketedSize(7));
    Assert.assertEquals(8, mPool.getBucketedSize(8));
  }
}
