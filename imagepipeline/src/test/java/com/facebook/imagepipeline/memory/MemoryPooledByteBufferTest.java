/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferInputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunk;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Basic tests for {@link MemoryPooledByteBuffer} */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MemoryPooledByteBufferTest extends TestUsingNativeMemoryChunk {
  private static final byte[] BYTES = new byte[] {1, 4, 5, 0, 100, 34, 0, 1, -1, -1};
  private static final int BUFFER_LENGTH = BYTES.length - 2;

  @Mock private NativeMemoryChunkPool mNativePool;
  private NativeMemoryChunk mNativeChunk;
  private MemoryPooledByteBuffer mNativePooledByteBuffer;

  @Mock private BufferMemoryChunkPool mBufferPool;
  private BufferMemoryChunk mBufferChunk;
  private MemoryPooledByteBuffer mBufferPooledByteBuffer;

  @Before
  public void setUp() {
    mNativeChunk = new FakeNativeMemoryChunk(BYTES.length);
    mNativeChunk.write(0, BYTES, 0, BYTES.length);
    mNativePool = mock(NativeMemoryChunkPool.class);
    CloseableReference<MemoryChunk> nativePoolRef =
        CloseableReference.of(mNativeChunk, mNativePool);
    mNativePooledByteBuffer = new MemoryPooledByteBuffer(nativePoolRef, BUFFER_LENGTH);
    nativePoolRef.close();

    mBufferChunk = new BufferMemoryChunk(BYTES.length);
    mBufferChunk.write(0, BYTES, 0, BYTES.length);
    mBufferPool = mock(BufferMemoryChunkPool.class);
    CloseableReference<MemoryChunk> bufferPoolRef =
        CloseableReference.of(mBufferChunk, mBufferPool);
    mBufferPooledByteBuffer = new MemoryPooledByteBuffer(bufferPoolRef, BUFFER_LENGTH);
    bufferPoolRef.close();
  }

  @Test
  public void testBasic() {
    testBasic(mNativePooledByteBuffer, mNativeChunk);
    testBasic(mBufferPooledByteBuffer, mBufferChunk);
  }

  @Test
  public void testSimpleRead() {
    testSimpleRead(mNativePooledByteBuffer);
    testSimpleRead(mBufferPooledByteBuffer);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSimpleReadOutOfBoundsUsingNativePool() {
    mNativePooledByteBuffer.read(BUFFER_LENGTH);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSimpleReadOutOfBoundsUsingBufferPool() {
    mBufferPooledByteBuffer.read(BUFFER_LENGTH);
  }

  @Test
  public void testRangeRead() {
    testRangeRead(mNativePooledByteBuffer);
    testRangeRead(mBufferPooledByteBuffer);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRangeReadOutOfBoundsUsingNativePool() {
    testRangeReadOutOfBounds(mNativePooledByteBuffer);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRangeReadOutOfBoundsUsingBufferPool() {
    testRangeReadOutOfBounds(mBufferPooledByteBuffer);
  }

  @Test
  public void testReadFromStream() throws Exception {
    testReadFromStream(mNativePooledByteBuffer);
    testReadFromStream(mBufferPooledByteBuffer);
  }

  @Test
  public void testClose() {
    testClose(mNativePooledByteBuffer, mNativeChunk, mNativePool);
    testClose(mBufferPooledByteBuffer, mBufferChunk, mBufferPool);
  }

  @Test(expected = PooledByteBuffer.ClosedException.class)
  public void testGettingSizeAfterCloseUsingNativePool() {
    mNativePooledByteBuffer.close();
    mNativePooledByteBuffer.size();
  }

  @Test(expected = PooledByteBuffer.ClosedException.class)
  public void testGettingSizeAfterCloseUsingBufferPool() {
    mBufferPooledByteBuffer.close();
    mBufferPooledByteBuffer.size();
  }

  private static void testBasic(
      final MemoryPooledByteBuffer mPooledByteBuffer, final MemoryChunk mChunk) {
    assertFalse(mPooledByteBuffer.isClosed());
    assertSame(mChunk, mPooledByteBuffer.mBufRef.get());
    assertEquals(BUFFER_LENGTH, mPooledByteBuffer.size());
  }

  private static void testSimpleRead(final MemoryPooledByteBuffer mPooledByteBuffer) {
    for (int i = 0; i < 100; ++i) {
      final int offset = i % BUFFER_LENGTH;
      assertEquals(BYTES[offset], mPooledByteBuffer.read(offset));
    }
  }

  private static void testRangeRead(final MemoryPooledByteBuffer mPooledByteBuffer) {
    byte[] readBuf = new byte[BUFFER_LENGTH];
    mPooledByteBuffer.read(1, readBuf, 1, BUFFER_LENGTH - 2);
    assertEquals(0, readBuf[0]);
    assertEquals(0, readBuf[BUFFER_LENGTH - 1]);
    for (int i = 1; i < BUFFER_LENGTH - 1; ++i) {
      assertEquals(BYTES[i], readBuf[i]);
    }
  }

  private static void testRangeReadOutOfBounds(final MemoryPooledByteBuffer mPooledByteBuffer) {
    byte[] readBuf = new byte[BUFFER_LENGTH];
    mPooledByteBuffer.read(1, readBuf, 0, BUFFER_LENGTH);
  }

  private static void testReadFromStream(final MemoryPooledByteBuffer mPooledByteBuffer)
      throws Exception {
    InputStream is = new PooledByteBufferInputStream(mPooledByteBuffer);
    byte[] tmp = new byte[BUFFER_LENGTH + 1];
    int bytesRead = is.read(tmp, 0, tmp.length);
    assertEquals(BUFFER_LENGTH, bytesRead);
    for (int i = 0; i < BUFFER_LENGTH; i++) {
      assertEquals(BYTES[i], tmp[i]);
    }
    assertEquals(-1, is.read());
  }

  private static void testClose(
      final MemoryPooledByteBuffer mPooledByteBuffer,
      final MemoryChunk mChunk,
      final MemoryChunkPool mPool) {
    mPooledByteBuffer.close();
    assertTrue(mPooledByteBuffer.isClosed());
    assertNull(mPooledByteBuffer.mBufRef);
    verify(mPool).release(mChunk);
  }
}
