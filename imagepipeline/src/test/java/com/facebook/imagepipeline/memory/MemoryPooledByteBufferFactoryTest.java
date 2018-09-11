/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteStreams;
import com.facebook.imagepipeline.testing.FakeBufferMemoryChunkPool;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunkPool;
import java.io.ByteArrayInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Basic tests for {@link MemoryPooledByteBufferFactory} */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MemoryPooledByteBufferFactoryTest extends TestUsingNativeMemoryChunk {
  private MemoryPooledByteBufferFactory mNativeFactory;
  private MemoryPooledByteBufferFactory mBufferFactory;
  private PoolStats mNativeStats;
  private PoolStats mBufferStats;
  private byte[] mData;

  @Before
  public void setup() {
    mData = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};

    NativeMemoryChunkPool mNativePool = new FakeNativeMemoryChunkPool();
    mNativeStats = new PoolStats(mNativePool);

    BufferMemoryChunkPool mBufferPool = new FakeBufferMemoryChunkPool();
    mBufferStats = new PoolStats(mBufferPool);

    ByteArrayPool byteArrayPool = mock(ByteArrayPool.class);
    byte[] pooledByteArray = new byte[8];
    when(byteArrayPool.get(8)).thenReturn(pooledByteArray);
    PooledByteStreams pooledByteStreams = new PooledByteStreams(byteArrayPool, 8);

    mNativeFactory = new MemoryPooledByteBufferFactory(mNativePool, pooledByteStreams);
    mBufferFactory = new MemoryPooledByteBufferFactory(mBufferPool, pooledByteStreams);
  }

  @Test
  public void testNewByteBuf_1() throws Exception {
    testNewByteBuf_1(mNativeFactory, mNativeStats);
    testNewByteBuf_1(mBufferFactory, mBufferStats);
  }

  @Test
  public void testNewByteBuf_2() throws Exception {
    testNewByteBuf_2(mNativeFactory, mNativeStats);
    testNewByteBuf_2(mBufferFactory, mBufferStats);
  }

  @Test
  public void testNewByteBuf_3() throws Exception {
    testNewByteBuf_3(mNativeFactory, mNativeStats);
    testNewByteBuf_3(mBufferFactory, mBufferStats);
  }

  @Test
  public void testNewByteBuf_4() throws Exception {
    testNewByteBuf_4(mNativeFactory, mNativeStats);
    testNewByteBuf_4(mBufferFactory, mBufferStats);
  }

  @Test
  public void testNewByteBuf_5() {
    testNewByteBuf_5(mNativeFactory, mNativeStats);
    testNewByteBuf_5(mBufferFactory, mBufferStats);
  }

  private void testNewByteBuf_1(
      final MemoryPooledByteBufferFactory mFactory, final PoolStats mStats) throws Exception {
    MemoryPooledByteBuffer sb1 = mFactory.newByteBuffer(new ByteArrayInputStream(mData));
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

  private void testNewByteBuf_2(
      final MemoryPooledByteBufferFactory mFactory, final PoolStats mStats) throws Exception {
    MemoryPooledByteBuffer sb2 = mFactory.newByteBuffer(new ByteArrayInputStream(mData), 8);
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

  private void testNewByteBuf_3(
      final MemoryPooledByteBufferFactory mFactory, final PoolStats mStats) throws Exception {
    MemoryPooledByteBuffer sb3 = mFactory.newByteBuffer(new ByteArrayInputStream(mData), 16);
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

  private void testNewByteBuf_4(
      final MemoryPooledByteBufferFactory mFactory, final PoolStats mStats) throws Exception {
    MemoryPooledByteBuffer sb4 = mFactory.newByteBuffer(new ByteArrayInputStream(mData), 32);
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

  private static void testNewByteBuf_5(
      final MemoryPooledByteBufferFactory mFactory, final PoolStats mStats) {
    MemoryPooledByteBuffer sb5 = mFactory.newByteBuffer(5);
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

  // Assert that the first 'length' bytes of expected are the same as those in 'actual'
  private static void assertArrayEquals(byte[] expected, byte[] actual, int length) {
    Assert.assertTrue(expected.length >= length);
    Assert.assertTrue(actual.length >= length);
    for (int i = 0; i < length; i++) {
      Assert.assertEquals(expected[i], actual[i], i);
    }
  }

  private static byte[] getBytes(MemoryPooledByteBuffer bb) {
    byte[] bytes = new byte[bb.size()];
    bb.mBufRef.get().read(0, bytes, 0, bytes.length);
    return bytes;
  }
}
