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
import java.io.IOException;
import java.io.InputStream;

import com.facebook.common.references.ResourceReleaser;
import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PooledByteArrayBufferedInputStreamTest {

  private ResourceReleaser mResourceReleaser;
  private byte[] mBuffer;
  private PooledByteArrayBufferedInputStream mPooledByteArrayBufferedInputStream;

  @Before
  public void setUp() {
    mResourceReleaser = mock(ResourceReleaser.class);
    final byte[] bytes = new byte[256];
    for (int i = 0; i < 256; ++i) {
      bytes[i] = (byte) i;
    }
    InputStream unbufferedStream = new ByteArrayInputStream(bytes);
    mBuffer = new byte[10];
    mPooledByteArrayBufferedInputStream = new PooledByteArrayBufferedInputStream(
        unbufferedStream,
        mBuffer,
        mResourceReleaser);
  }

  @Test
  public void testSingleByteRead() throws IOException {
    for (int i = 0; i < 256; ++i) {
      assertEquals(i, mPooledByteArrayBufferedInputStream.read());
    }
    assertEquals(-1, mPooledByteArrayBufferedInputStream.read());
  }

  @Test
  public void testReleaseOnClose() throws IOException {
    mPooledByteArrayBufferedInputStream.close();
    verify(mResourceReleaser).release(mBuffer);
    mPooledByteArrayBufferedInputStream.close();
    // we do not expect second close to release resource again,
    // the one checked bellow is the one that happened when close was called for the first time
    verify(mResourceReleaser).release(any(byte[].class));
  }

  @Test
  public void testSkip() throws IOException {
    // buffer some data
    mPooledByteArrayBufferedInputStream.read();
    assertEquals(99, mPooledByteArrayBufferedInputStream.skip(99));
    assertEquals(100, mPooledByteArrayBufferedInputStream.read());
  }

  @Test
  public void testSkip2() throws IOException {
    int i = 0;
    while (i < 256) {
      assertEquals(i, mPooledByteArrayBufferedInputStream.read());
      i += mPooledByteArrayBufferedInputStream.skip(7) + 1;
    }
  }

  @Test
  public void testMark() {
    assertFalse(mPooledByteArrayBufferedInputStream.markSupported());
  }

  @Test
  public void testReadWithByteArray() throws IOException {
    byte[] readBuffer = new byte[5];
    assertEquals(5, mPooledByteArrayBufferedInputStream.read(readBuffer));
    assertFilledWithConsecutiveBytes(readBuffer, 0, 5, 0);
  }

  @Test
  public void testNonFullRead() throws IOException {
    byte[] readBuffer = new byte[200];
    assertEquals(10, mPooledByteArrayBufferedInputStream.read(readBuffer));
    assertFilledWithConsecutiveBytes(readBuffer, 0, 10, 0);
    assertFilledWithZeros(readBuffer, 10, 200);
  }

  @Test
  public void testNonFullReadWithOffset() throws IOException {
    byte[] readBuffer = new byte[200];
    assertEquals(10, mPooledByteArrayBufferedInputStream.read(readBuffer, 45, 75));
    assertFilledWithZeros(readBuffer, 0, 45);
    assertFilledWithConsecutiveBytes(readBuffer, 45, 55, 0);
    assertFilledWithZeros(readBuffer, 55, 200);
  }

  @Test
 public void testReadsCombined() throws IOException {
    byte[] readBuffer = new byte[5];
    int i = 0;
    while (i <= 245) {
      assertEquals(i, mPooledByteArrayBufferedInputStream.read());

      assertEquals(5, mPooledByteArrayBufferedInputStream.read(readBuffer));
      assertFilledWithConsecutiveBytes(readBuffer, 0, readBuffer.length, i + 1);

      assertEquals(3, mPooledByteArrayBufferedInputStream.read(readBuffer, 1, 3));
      assertEquals((byte) (i + 1), readBuffer[0]);
      assertFilledWithConsecutiveBytes(readBuffer, 1, 4, i + 6);
      assertEquals((byte) (i + 5), readBuffer[4]);

      assertEquals(2, mPooledByteArrayBufferedInputStream.skip(2));

      i += 11;
    }

    assertEquals(256 - i, mPooledByteArrayBufferedInputStream.available());
  }

  /**
   * Given byte array, asserts that bytes in [startOffset, endOffset) range are all zeroed;
   *
   * @param byteArray
   * @param startOffset
   * @param endOffset
   */
  private static void assertFilledWithZeros(
      final byte[] byteArray,
      final int startOffset,
      final int endOffset) {
    for (int i = startOffset; i < endOffset; ++i) {
      assertEquals(0, byteArray[i]);
    }
  }

  /**
   * Given byte array, asserts that each byte in (startOffset, endOffset) range has value equal
   * to value of previous byte plus one (mod 255) and byteArray[startOffset] is equal to firstByte.
   *
   * @param byteArray
   * @param startOffset
   * @param endOffset
   * @param firstByte
   */
  private static void assertFilledWithConsecutiveBytes(
      final byte[] byteArray,
      final int startOffset,
      final int endOffset,
      int firstByte) {
    for (int i = startOffset; i < endOffset; ++i) {
      assertEquals((byte) firstByte++, byteArray[i]);
    }
  }
}
