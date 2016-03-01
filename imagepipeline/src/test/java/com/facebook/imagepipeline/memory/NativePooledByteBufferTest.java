/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.io.InputStream;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunk;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Basic tests for {@link NativePooledByteBuffer}
 */
@RunWith(RobolectricTestRunner.class)
public class NativePooledByteBufferTest extends TestUsingNativeMemoryChunk {
  private static final byte[] BYTES = new byte[] {1, 4, 5, 0, 100, 34, 0, 1, -1, -1};
  private static final int BUFFER_LENGTH = BYTES.length - 2;

  @Mock public NativeMemoryChunkPool mPool;
  private NativeMemoryChunk mChunk;
  private NativePooledByteBuffer mPooledByteBuffer;

  @Before
  public void setUp() {
    mChunk = new FakeNativeMemoryChunk(BYTES.length);
    mChunk.write(0, BYTES, 0, BYTES.length);
    mPool = mock(NativeMemoryChunkPool.class);
    CloseableReference<NativeMemoryChunk> poolRef = CloseableReference.of(mChunk, mPool);
    mPooledByteBuffer = new NativePooledByteBuffer(
        poolRef,
        BUFFER_LENGTH);
    poolRef.close();
  }

  @Test
  public void testBasic() throws Exception {
    assertFalse(mPooledByteBuffer.isClosed());
    assertSame(mChunk, mPooledByteBuffer.mBufRef.get());
    assertEquals(BUFFER_LENGTH, mPooledByteBuffer.size());
  }

  @Test
  public void testSimpleRead() {
    for (int i = 0; i < 100; ++i) {
      final int offset = i % BUFFER_LENGTH;
      assertEquals(BYTES[offset], mPooledByteBuffer.read(offset));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSimpleReadOutOfBounds() {
    mPooledByteBuffer.read(BUFFER_LENGTH);
  }

  @Test
  public void testRangeRead() {
    byte[] readBuf = new byte[BUFFER_LENGTH];
    mPooledByteBuffer.read(1, readBuf, 1, BUFFER_LENGTH - 2);
    assertEquals(0, readBuf[0]);
    assertEquals(0, readBuf[BUFFER_LENGTH - 1]);
    for (int i = 1; i < BUFFER_LENGTH - 1; ++i) {
      assertEquals(BYTES[i], readBuf[i]);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRangeReadOutOfBounds() {
    byte[] readBuf = new byte[BUFFER_LENGTH];
    mPooledByteBuffer.read(1, readBuf, 0, BUFFER_LENGTH);
  }

  @Test
  public void testReadFromStream() throws Exception {
    InputStream is = new PooledByteBufferInputStream(mPooledByteBuffer);
    byte[] tmp = new byte[BUFFER_LENGTH + 1];
    int bytesRead = is.read(tmp, 0, tmp.length);
    assertEquals(BUFFER_LENGTH, bytesRead);
    for (int i = 0; i < BUFFER_LENGTH; i++) {
      assertEquals(BYTES[i], tmp[i]);
    }
    assertEquals(-1, is.read());
  }

  @Test
  public void testClose() {
    mPooledByteBuffer.close();
    assertTrue(mPooledByteBuffer.isClosed());
    assertNull(mPooledByteBuffer.mBufRef);
    verify(mPool).release(mChunk);
  }

  @Test(expected = PooledByteBuffer.ClosedException.class)
  public void testGettingSizeAfterClose() {
    mPooledByteBuffer.close();
    mPooledByteBuffer.size();
  }
}
