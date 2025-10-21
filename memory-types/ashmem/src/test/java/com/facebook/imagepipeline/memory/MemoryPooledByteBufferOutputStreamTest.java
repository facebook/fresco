/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.testing.FakeAshmemMemoryChunkPool;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link MemoryPooledByteBufferOutputStream} */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MemoryPooledByteBufferOutputStreamTest {
  private AshmemMemoryChunkPool mAshmemPool;

  private byte[] mData;
  private PoolStats<byte[]> mAshmemStats;

  @Before
  public void setup() {
    mAshmemPool = new FakeAshmemMemoryChunkPool();
    mAshmemStats = new PoolStats(mAshmemPool);

    mData = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
  }

  @Test
  public void testBasic_1() throws Exception {
    testBasic_1(mAshmemPool, mAshmemStats);
  }

  @Test
  public void testBasic_2() throws Exception {
    testBasic_2(mAshmemPool, mAshmemStats);
  }

  @Test
  public void testBasic_3() throws Exception {
    testBasic_3(mAshmemPool, mAshmemStats);
  }

  @Test
  public void testBasic_4() throws Exception {
    testBasic_4(mAshmemPool, mAshmemStats);
  }

  @Test
  public void testClose() {
    testClose(mAshmemPool, mAshmemStats);
  }

  @Test(expected = MemoryPooledByteBufferOutputStream.InvalidStreamException.class)
  public void testToByteBufExceptionUsingAshmemPool() {
    testToByteBufException(mAshmemPool);
  }

  @Test
  public void testWriteAfterToByteBuf() throws Exception {
    testWriteAfterToByteBuf(mAshmemPool);
  }

  private void testBasic_1(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats)
      throws Exception {
    MemoryPooledByteBufferOutputStream os1 = new MemoryPooledByteBufferOutputStream(mPool);
    MemoryPooledByteBuffer sb1 = doWrite(os1, mData);
    assertThat(sb1.getCloseableReference().get().getSize()).isEqualTo(16);
    assertArrayEquals(mData, getBytes(sb1), mData.length);
    mStats.refresh();
    assertThat(mStats.getBucketStats())
        .isEqualTo(
            ImmutableMap.of(
                32, new IntPair(0, 0),
                16, new IntPair(1, 0),
                8, new IntPair(0, 1),
                4, new IntPair(0, 1)));
  }

  private void testBasic_2(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats)
      throws Exception {
    MemoryPooledByteBufferOutputStream os2 = new MemoryPooledByteBufferOutputStream(mPool, 8);
    MemoryPooledByteBuffer sb2 = doWrite(os2, mData);
    assertThat(sb2.getCloseableReference().get().getSize()).isEqualTo(16);
    assertArrayEquals(mData, getBytes(sb2), mData.length);
    mStats.refresh();
    assertThat(mStats.getBucketStats())
        .isEqualTo(
            ImmutableMap.of(
                32, new IntPair(0, 0),
                16, new IntPair(1, 0),
                8, new IntPair(0, 1),
                4, new IntPair(0, 0)));
  }

  private void testBasic_3(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats)
      throws Exception {
    MemoryPooledByteBufferOutputStream os3 = new MemoryPooledByteBufferOutputStream(mPool, 16);
    MemoryPooledByteBuffer sb3 = doWrite(os3, mData);
    assertThat(sb3.getCloseableReference().get().getSize()).isEqualTo(16);
    assertArrayEquals(mData, getBytes(sb3), mData.length);
    mStats.refresh();
    assertThat(mStats.getBucketStats())
        .isEqualTo(
            ImmutableMap.of(
                32, new IntPair(0, 0),
                16, new IntPair(1, 0),
                8, new IntPair(0, 0),
                4, new IntPair(0, 0)));
  }

  private void testBasic_4(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats)
      throws Exception {
    MemoryPooledByteBufferOutputStream os4 = new MemoryPooledByteBufferOutputStream(mPool, 32);
    MemoryPooledByteBuffer sb4 = doWrite(os4, mData);
    assertThat(sb4.getCloseableReference().get().getSize()).isEqualTo(32);
    assertArrayEquals(mData, getBytes(sb4), mData.length);
    mStats.refresh();
    assertThat(mStats.getBucketStats())
        .isEqualTo(
            ImmutableMap.of(
                32, new IntPair(1, 0),
                16, new IntPair(0, 0),
                8, new IntPair(0, 0),
                4, new IntPair(0, 0)));
  }

  private static void testClose(final MemoryChunkPool mPool, final PoolStats<byte[]> mStats) {
    MemoryPooledByteBufferOutputStream os = new MemoryPooledByteBufferOutputStream(mPool);
    os.close();
    mStats.refresh();
    assertThat(mStats.getBucketStats())
        .isEqualTo(
            ImmutableMap.of(
                32, new IntPair(0, 0),
                16, new IntPair(0, 0),
                8, new IntPair(0, 0),
                4, new IntPair(0, 1)));
  }

  private static void testToByteBufException(final MemoryChunkPool mPool) {
    MemoryPooledByteBufferOutputStream os1 = new MemoryPooledByteBufferOutputStream(mPool);
    os1.close();
    os1.toByteBuffer();
  }

  private void testWriteAfterToByteBuf(final MemoryChunkPool mPool) throws Exception {
    MemoryPooledByteBufferOutputStream os1 = new MemoryPooledByteBufferOutputStream(mPool);
    MemoryPooledByteBuffer buf1 = doWrite(os1, Arrays.copyOf(mData, 9));
    MemoryPooledByteBuffer buf2 = doWrite(os1, Arrays.copyOf(mData, 3));
    assertThat(buf2.size()).isEqualTo(12);

    final CloseableReference<MemoryChunk> chunk = buf1.getCloseableReference();
    assertThat(chunk.getUnderlyingReferenceTestOnly().getRefCountTestOnly()).isEqualTo(3);
    os1.close();
    buf1.close();
    buf2.close();
    assertThat(chunk.getUnderlyingReferenceTestOnly().getRefCountTestOnly()).isEqualTo(0);
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
    assertThat(expected.length >= length).isTrue();
    assertThat(actual.length >= length).isTrue();
    for (int i = 0; i < length; i++) {
      assertThat(actual[i]).isEqualTo(expected[i]);
    }
  }

  private static byte[] getBytes(MemoryPooledByteBuffer bb) {
    byte[] bytes = new byte[bb.size()];
    bb.getCloseableReference().get().read(0, bytes, 0, bytes.length);
    return bytes;
  }
}
