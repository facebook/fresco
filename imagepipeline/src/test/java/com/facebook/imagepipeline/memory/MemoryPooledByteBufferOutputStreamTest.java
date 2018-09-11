/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.testing.FakeBufferMemoryChunkPool;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunkPool;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link MemoryPooledByteBufferOutputStream} */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MemoryPooledByteBufferOutputStreamTest extends TestUsingNativeMemoryChunk {
  private NativeMemoryChunkPool mNativePool;
  private BufferMemoryChunkPool mBufferPool;

  private byte[] mData;
  private PoolStats<byte[]> mNativeStats;
  private PoolStats<byte[]> mBufferStats;

  @Before
  public void setup() {
    mNativePool = new FakeNativeMemoryChunkPool();
    mNativeStats = new PoolStats(mNativePool);

    mBufferPool = new FakeBufferMemoryChunkPool();
    mBufferStats = new PoolStats(mBufferPool);

    mData = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
  }

  @Test
  public void testBasic_1() throws Exception {
    testBasic_1(mNativePool, mNativeStats);
    testBasic_1(mBufferPool, mBufferStats);
  }

  @Test
  public void testBasic_2() throws Exception {
    testBasic_2(mNativePool, mNativeStats);
    testBasic_2(mBufferPool, mBufferStats);
  }

  @Test
  public void testBasic_3() throws Exception {
    testBasic_3(mNativePool, mNativeStats);
    testBasic_3(mBufferPool, mBufferStats);
  }

  @Test
  public void testBasic_4() throws Exception {
    testBasic_4(mNativePool, mNativeStats);
    testBasic_4(mBufferPool, mBufferStats);
  }

  @Test
  public void testClose() {
    testClose(mNativePool, mNativeStats);
    testClose(mBufferPool, mBufferStats);
  }

  @Test(expected = MemoryPooledByteBufferOutputStream.InvalidStreamException.class)
  public void testToByteBufExceptionUsingNativePool() {
    testToByteBufException(mNativePool);
  }

  @Test(expected = MemoryPooledByteBufferOutputStream.InvalidStreamException.class)
  public void testToByteBufExceptionUsingBufferPool() {
    testToByteBufException(mBufferPool);
  }

  @Test
  public void testWriteAfterToByteBuf() throws Exception {
    testWriteAfterToByteBuf(mNativePool);
    testWriteAfterToByteBuf(mBufferPool);
  }

  private void testBasic_1(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats)
      throws Exception {
    MemoryPooledByteBufferOutputStream os1 = new MemoryPooledByteBufferOutputStream(mPool);
    MemoryPooledByteBuffer sb1 = doWrite(os1, mData);
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

  private void testBasic_2(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats)
      throws Exception {
    MemoryPooledByteBufferOutputStream os2 = new MemoryPooledByteBufferOutputStream(mPool, 8);
    MemoryPooledByteBuffer sb2 = doWrite(os2, mData);
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

  private void testBasic_3(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats)
      throws Exception {
    MemoryPooledByteBufferOutputStream os3 = new MemoryPooledByteBufferOutputStream(mPool, 16);
    MemoryPooledByteBuffer sb3 = doWrite(os3, mData);
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

  private void testBasic_4(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats)
      throws Exception {
    MemoryPooledByteBufferOutputStream os4 = new MemoryPooledByteBufferOutputStream(mPool, 32);
    MemoryPooledByteBuffer sb4 = doWrite(os4, mData);
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

  private static void testClose(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats) {
    MemoryPooledByteBufferOutputStream os = new MemoryPooledByteBufferOutputStream(mPool);
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

  private static void testToByteBufException(final MemoryChunkPool mPool) {
    MemoryPooledByteBufferOutputStream os1 = new MemoryPooledByteBufferOutputStream(mPool);
    os1.close();
    os1.toByteBuffer();
    Assert.fail();
  }

  private void testWriteAfterToByteBuf(final MemoryChunkPool mPool) throws Exception {
    MemoryPooledByteBufferOutputStream os1 = new MemoryPooledByteBufferOutputStream(mPool);
    MemoryPooledByteBuffer buf1 = doWrite(os1, Arrays.copyOf(mData, 9));
    MemoryPooledByteBuffer buf2 = doWrite(os1, Arrays.copyOf(mData, 3));
    Assert.assertEquals(12, buf2.size());

    final CloseableReference<MemoryChunk> chunk = buf1.mBufRef;
    Assert.assertEquals(3, chunk.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    os1.close();
    buf1.close();
    buf2.close();
    Assert.assertEquals(0, chunk.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  // write out the contents of data into the output stream
  private static MemoryPooledByteBuffer doWrite(MemoryPooledByteBufferOutputStream os, byte[] data)
      throws Exception {
    for (int i = 0; i < data.length; i++) {
      os.write(data, i, 1);
    }
    return os.toByteBuffer();
  }

  // assert that the first 'length' bytes of expected are the same as those in 'actual'
  private static void assertArrayEquals(byte[] expected, byte[] actual, int length) {
    Assert.assertTrue(expected.length >= length);
    Assert.assertTrue(actual.length >= length);
    for (int i = 0; i < length; i++) {
      Assert.assertEquals(expected[i], actual[i]);
    }
  }

  private static byte[] getBytes(MemoryPooledByteBuffer bb) {
    byte[] bytes = new byte[bb.size()];
    bb.mBufRef.get().read(0, bytes, 0, bytes.length);
    return bytes;
  }
}
