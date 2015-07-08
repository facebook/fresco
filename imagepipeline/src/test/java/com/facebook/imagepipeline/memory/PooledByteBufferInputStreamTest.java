/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.*;

/**
 * Tests for {@link NativeMemoryChunkInputStream}
 */
@RunWith(RobolectricTestRunner.class)
public class PooledByteBufferInputStreamTest {
  private static final byte[] BYTES = new byte[] {1, 123, -20, 3, 6, 23, 1};
  private PooledByteBufferInputStream mStream;

  @Before
  public void setup() {
    PooledByteBuffer buffer = new TrivialPooledByteBuffer(BYTES);
    mStream = new PooledByteBufferInputStream(buffer);
  }

  @Test
  public void testBasic() {
    assertEquals(0, mStream.mOffset);
    assertEquals(0, mStream.mMark);
    assertEquals(BYTES.length, mStream.available());
    assertTrue(mStream.markSupported());
  }

  @Test
  public void testMark() {
    mStream.skip(2);
    mStream.mark(0);
    assertEquals(2, mStream.mMark);
    mStream.read();
    assertEquals(2, mStream.mMark);
    mStream.mark(0);
    assertEquals(3, mStream.mMark);
  }

  @Test
  public void testReset() {
    mStream.skip(2);
    mStream.reset();
    assertEquals(0, mStream.mOffset);
  }

  @Test
  public void testAvailable() {
    assertEquals(BYTES.length, mStream.available());
    mStream.skip(3);
    assertEquals(BYTES.length - 3, mStream.available());
    mStream.skip(BYTES.length);
    assertEquals(0, mStream.available());
  }

  @Test
  public void testSkip() {
    assertEquals(2, mStream.skip(2));
    assertEquals(2, mStream.mOffset);

    assertEquals(3, mStream.skip(3));
    assertEquals(5, mStream.mOffset);

    assertEquals(BYTES.length - 5, mStream.skip(BYTES.length));
    assertEquals(0, mStream.skip(BYTES.length));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSkipNegative() {
    mStream.skip(-4);
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testReadWithErrors() {
    mStream.read(new byte[64], 10, 55);
  }

  @Test
  public void testRead_SingleByte() {
    for (int i = 0; i < BYTES.length; ++i) {
      assertEquals(((int) BYTES[i]) & 0xFF, mStream.read());
    }
    assertEquals(-1, mStream.read());
  }

  @Test
  public void testRead_ToByteArray() {
    byte[] buf = new byte[64];

    assertEquals(0, mStream.read(buf, 0, 0));
    assertEquals(0, mStream.mOffset);

    assertEquals(3, mStream.read(buf, 0, 3));
    assertEquals(3, mStream.mOffset);
    assertArrayEquals(BYTES, buf, 3);
    for (int i = 3; i < buf.length; ++i) {
      assertEquals(0, buf[i]);
    }

    int available = BYTES.length - mStream.mOffset;
    assertEquals(available, mStream.read(buf, 3, available + 1));
    assertEquals(BYTES.length, mStream.mOffset);
    assertArrayEquals(BYTES, buf, available);

    assertEquals(-1, mStream.read(buf, 0, 1));
    assertEquals(BYTES.length, mStream.mOffset);
  }

  @Test
  public void testRead_ToByteArray2() {
    byte[] buf = new byte[BYTES.length + 10];
    assertEquals(BYTES.length, mStream.read(buf));
    assertArrayEquals(BYTES, buf, BYTES.length);
  }

  @Test
  public void testRead_ToByteArray3() {
    byte[] buf = new byte[BYTES.length -1];
    assertEquals(buf.length, mStream.read(buf));
    assertEquals(buf.length, mStream.mOffset);
    assertArrayEquals(BYTES, buf, buf.length);
  }

  @Test
  public void testCreateEmptyStream() throws Exception {
    PooledByteBufferInputStream is = new PooledByteBufferInputStream(
        new TrivialPooledByteBuffer(new byte[] {}));
    assertEquals(-1, is.read());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreatingStreamAfterClose() {
    PooledByteBuffer buffer = new TrivialPooledByteBuffer(new byte[] {});
    buffer.close();
    new PooledByteBufferInputStream(buffer);
  }

  // assert that the first 'length' bytes of expected are the same as those in 'actual'
  private static void assertArrayEquals(byte[] expected, byte[] actual, int length) {
    assertTrue(expected.length >= length);
    assertTrue(actual.length >= length);
    for (int i = 0; i < length; i++) {
      assertEquals(expected[i], actual[i]);
    }
  }
}
