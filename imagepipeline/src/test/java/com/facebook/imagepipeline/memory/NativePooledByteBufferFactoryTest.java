/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.io.ByteArrayInputStream;

import com.facebook.imagepipeline.testing.FakeNativeMemoryChunkPool;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic tests for {@link NativePooledByteBufferFactory}
 */
@RunWith(RobolectricTestRunner.class)
public class NativePooledByteBufferFactoryTest extends TestUsingNativeMemoryChunk {
  private NativeMemoryChunkPool mPool;
  private NativePooledByteBufferFactory mFactory;
  private PoolStats mStats;
  PooledByteStreams mPooledByteStreams;
  private byte[] mData;

  @Before
  public void setup() {
    mData = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
    mPool = new FakeNativeMemoryChunkPool();
    mStats = new PoolStats(mPool);

    ByteArrayPool byteArrayPool = mock(ByteArrayPool.class);
    byte[] pooledByteArray = new byte[8];
    when(byteArrayPool.get(8)).thenReturn(pooledByteArray);
    mPooledByteStreams = new PooledByteStreams(byteArrayPool, 8);

    mFactory = new NativePooledByteBufferFactory(mPool, mPooledByteStreams);
  }

  // assert that the first 'length' bytes of expected are the same as those in 'actual'
  private void assertArrayEquals(byte[] expected, byte[] actual, int length) {
    Assert.assertTrue(expected.length >= length);
    Assert.assertTrue(actual.length >= length);
    for (int i = 0; i < length; i++) {
      Assert.assertEquals(expected[i], actual[i], i);
    }
  }

  private byte[] getBytes(NativePooledByteBuffer bb) {
    byte[] bytes = new byte[bb.size()];
    bb.mBufRef.get().read(0, bytes, 0, bytes.length);
    return bytes;
  }

  @Test
  public void testNewByteBuf_1() throws Exception {
    NativePooledByteBuffer sb1 = mFactory.newByteBuffer(new ByteArrayInputStream(mData));
    Assert.assertEquals(16, sb1.mBufRef.get().getSize());
    assertArrayEquals(mData, getBytes(sb1), mData.length);
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            32, new IntPair(0, 0),
            16, new IntPair(1, 0),
            8, new IntPair(0, 1),
            4, new IntPair(0, 1)),
        mStats.mBucketStats);
  }

  @Test
  public void testNewByteBuf_2() throws Exception {
    NativePooledByteBuffer sb2 = mFactory.newByteBuffer(new ByteArrayInputStream(mData), 8);
    Assert.assertEquals(16, sb2.mBufRef.get().getSize());
    assertArrayEquals(mData, getBytes(sb2), mData.length);
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            32, new IntPair(0, 0),
            16, new IntPair(1, 0),
            8, new IntPair(0, 1),
            4, new IntPair(0, 0)),
        mStats.mBucketStats);
  }

  @Test
  public void testNewByteBuf_3() throws Exception {
    NativePooledByteBuffer sb3 = mFactory.newByteBuffer(new ByteArrayInputStream(mData), 16);
    Assert.assertEquals(16, sb3.mBufRef.get().getSize());
    assertArrayEquals(mData, getBytes(sb3), mData.length);
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            32, new IntPair(0, 0),
            16, new IntPair(1, 0),
            8, new IntPair(0, 0),
            4, new IntPair(0, 0)),
        mStats.mBucketStats);
  }

  @Test
  public void testNewByteBuf_4() throws Exception {
    NativePooledByteBuffer sb4 = mFactory.newByteBuffer(new ByteArrayInputStream(mData), 32);
    Assert.assertEquals(32, sb4.mBufRef.get().getSize());
    assertArrayEquals(mData, getBytes(sb4), mData.length);
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            32, new IntPair(1, 0),
            16, new IntPair(0, 0),
            8, new IntPair(0, 0),
            4, new IntPair(0, 0)),
        mStats.mBucketStats);
  }

  @Test
  public void testNewByteBuf_5() {
    NativePooledByteBuffer sb5 = mFactory.newByteBuffer(5);
    Assert.assertEquals(8, sb5.mBufRef.get().getSize());
    Assert.assertEquals(1, sb5.mBufRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            32, new IntPair(0, 0),
            16, new IntPair(0, 0),
            8, new IntPair(1, 0),
            4, new IntPair(0, 0)),
        mStats.mBucketStats);
    sb5.close();
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            32, new IntPair(0, 0),
            16, new IntPair(0, 0),
            8, new IntPair(0, 1),
            4, new IntPair(0, 0)),
        mStats.mBucketStats);
  }
}
