/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.util.Arrays;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunkPool;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for NativePooledByteBufferOutputStream
 */
@RunWith(RobolectricTestRunner.class)
public class NativePooledByteBufferOutputStreamTest extends TestUsingNativeMemoryChunk {
  private NativeMemoryChunkPool mPool;
  private byte[] mData;
  private PoolStats<byte[]> mStats;

  @Before
  public void setup() {
    mPool = new FakeNativeMemoryChunkPool();
    mStats = new PoolStats(mPool);
    mData = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
  }

  // write out the contents of data into the output stream
  private NativePooledByteBuffer doWrite(NativePooledByteBufferOutputStream os, byte[] data)
      throws Exception {
    for (int i = 0; i < data.length; i++) {
      os.write(data, i, 1);
    }
    return os.toByteBuffer();
  }

  // assert that the first 'length' bytes of expected are the same as those in 'actual'
  private void assertArrayEquals(byte[] expected, byte[] actual, int length) {
    Assert.assertTrue(expected.length >= length);
    Assert.assertTrue(actual.length >= length);
    for (int i = 0; i < length; i++) {
      Assert.assertEquals(expected[i], actual[i]);
    }
  }

  private byte[] getBytes(NativePooledByteBuffer bb) {
    byte[] bytes = new byte[bb.size()];
    bb.mBufRef.get().read(0, bytes, 0, bytes.length);
    return bytes;
  }

  @Test
  public void testBasic_1() throws Exception {
    NativePooledByteBufferOutputStream os1 = new NativePooledByteBufferOutputStream(mPool);
    NativePooledByteBuffer sb1 = doWrite(os1, mData);
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
  public void testBasic_2() throws Exception {
    NativePooledByteBufferOutputStream os2 = new NativePooledByteBufferOutputStream(mPool, 8);
    NativePooledByteBuffer sb2 = doWrite(os2, mData);
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
  public void testBasic_3() throws Exception {
    NativePooledByteBufferOutputStream os3 = new NativePooledByteBufferOutputStream(mPool, 16);
    NativePooledByteBuffer sb3 = doWrite(os3, mData);
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
  public void testBasic_4() throws Exception {
    NativePooledByteBufferOutputStream os4 = new NativePooledByteBufferOutputStream(mPool, 32);
    NativePooledByteBuffer sb4 = doWrite(os4, mData);
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
  public void testClose() throws Exception {
    NativePooledByteBufferOutputStream os = new NativePooledByteBufferOutputStream(mPool);
    os.close();
    mStats.refresh();
    Assert.assertEquals(
        ImmutableMap.of(
            32, new IntPair(0, 0),
            16, new IntPair(0, 0),
            8, new IntPair(0, 0),
            4, new IntPair(0, 1)),
        mStats.mBucketStats);
  }

  @Test
  public void testToByteBufException() throws Exception {
    NativePooledByteBufferOutputStream os1 = new NativePooledByteBufferOutputStream(mPool);
    os1.close();
    try {
      os1.toByteBuffer();
      Assert.fail();
    } catch (Exception e) {
      // do nothing
    }
  }

  @Test
  public void testWriteAfterToByteBuf() throws Exception {
    NativePooledByteBufferOutputStream os1 = new NativePooledByteBufferOutputStream(mPool);
    NativePooledByteBuffer buf1 = doWrite(os1, Arrays.copyOf(mData, 9));
    NativePooledByteBuffer buf2 = doWrite(os1, Arrays.copyOf(mData, 3));
    Assert.assertEquals(12, buf2.size());

    final CloseableReference<NativeMemoryChunk> chunk = buf1.mBufRef;
    Assert.assertEquals(3, chunk.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    os1.close();
    buf1.close();
    buf2.close();
    Assert.assertEquals(0, chunk.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }
}
