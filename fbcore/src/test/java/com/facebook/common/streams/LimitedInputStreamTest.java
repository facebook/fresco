/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.streams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class LimitedInputStreamTest {

  private static final int RANDOM_SEED = 1023;
  private static final int BYTES_LENGTH = 1024;
  private static final int LIMITED_LENGTH = BYTES_LENGTH / 2;

  private byte[] mData;
  private byte[] mReadBuffer;
  private byte[] mZeroTail;

  private ByteArrayInputStream mOriginalStream;
  private LimitedInputStream mLimitedStream;

  @Before
  public void setUp() {
    mData = new byte[BYTES_LENGTH];
    mReadBuffer = new byte[BYTES_LENGTH];
    final Random random = new Random(RANDOM_SEED);
    random.nextBytes(mData);
    mZeroTail = new byte[BYTES_LENGTH - LIMITED_LENGTH];
    Arrays.fill(mZeroTail, (byte) 0);

    mOriginalStream = new ByteArrayInputStream(mData);
    mLimitedStream = new LimitedInputStream(mOriginalStream, LIMITED_LENGTH);
  }

  @Test
  public void testBasic() throws Exception {
    assertEquals(LIMITED_LENGTH, mLimitedStream.available());
    assertTrue(mLimitedStream.markSupported());
  }

  @Test
  public void testDoesReadSingleBytes() throws Exception {
    for (int i = 0; i < LIMITED_LENGTH; ++i) {
      assertEquals(((int) mData[i]) & 0xFF, mLimitedStream.read());
    }
  }

  @Test
  public void testDoesNotReadTooMuch_singleBytes() throws Exception {
    for (int i = 0; i < BYTES_LENGTH; ++i) {
      final int lastByte = mLimitedStream.read();
      assertEquals(i >= LIMITED_LENGTH, lastByte == -1);
    }
    assertEquals(BYTES_LENGTH - LIMITED_LENGTH, mOriginalStream.available());
  }

  @Test
  public void testDoesReadMultipleBytes() throws Exception {
    assertEquals(LIMITED_LENGTH, mLimitedStream.read(mReadBuffer, 0, LIMITED_LENGTH));
    assertArrayEquals(
        Arrays.copyOfRange(mData, 0, LIMITED_LENGTH),
        Arrays.copyOfRange(mReadBuffer, 0, LIMITED_LENGTH));
    assertArrayEquals(
        mZeroTail,
        Arrays.copyOfRange(mReadBuffer, LIMITED_LENGTH, BYTES_LENGTH));
  }

  @Test
  public void testDoesNotReadTooMuch_multipleBytes() throws Exception {
    assertEquals(LIMITED_LENGTH, mLimitedStream.read(mReadBuffer, 0, BYTES_LENGTH));
    final byte[] readBufferCopy = Arrays.copyOf(mReadBuffer, mReadBuffer.length);
    assertEquals(-1, mLimitedStream.read(mReadBuffer, 0, BYTES_LENGTH));
    assertArrayEquals(readBufferCopy, mReadBuffer);
    assertEquals(BYTES_LENGTH - LIMITED_LENGTH, mOriginalStream.available());
  }

  @Test
  public void testSkip() throws Exception {
    assertEquals(LIMITED_LENGTH / 2, mLimitedStream.skip(LIMITED_LENGTH / 2));
    assertEquals(LIMITED_LENGTH / 2, mLimitedStream.read(mReadBuffer));
    assertArrayEquals(
        Arrays.copyOfRange(mData, LIMITED_LENGTH / 2, LIMITED_LENGTH),
        Arrays.copyOfRange(mReadBuffer, 0, LIMITED_LENGTH / 2));
  }

  @Test
  public void testDoesNotReadTooMuch_skip() throws Exception {
    assertEquals(LIMITED_LENGTH, mLimitedStream.skip(BYTES_LENGTH));
    assertEquals(0, mLimitedStream.skip(BYTES_LENGTH));
    assertEquals(BYTES_LENGTH - LIMITED_LENGTH, mOriginalStream.available());
  }

  @Test
  public void testDoesMark() throws Exception {
    mLimitedStream.mark(BYTES_LENGTH);
    mLimitedStream.read(mReadBuffer);
    final byte[] readBufferCopy = Arrays.copyOf(mReadBuffer, mReadBuffer.length);
    Arrays.fill(mReadBuffer, (byte) 0);
    mLimitedStream.reset();
    assertEquals(LIMITED_LENGTH, mLimitedStream.read(mReadBuffer));
    assertArrayEquals(readBufferCopy, mReadBuffer);
  }

  @Test
  public void testResetsMultipleTimes() throws Exception {
    mLimitedStream.mark(BYTES_LENGTH);
    mLimitedStream.read(mReadBuffer);
    final byte[] readBufferCopy = Arrays.copyOf(mReadBuffer, mReadBuffer.length);

    // first reset
    mLimitedStream.reset();
    assertEquals(LIMITED_LENGTH, mLimitedStream.read(mReadBuffer));

    // second reset
    Arrays.fill(mReadBuffer, (byte) 0);
    mLimitedStream.reset();
    assertEquals(LIMITED_LENGTH, mLimitedStream.read(mReadBuffer));

    assertArrayEquals(readBufferCopy, mReadBuffer);
  }

  @Test
  public void testDoesNotReadTooMuch_reset() throws Exception {
    mLimitedStream.mark(BYTES_LENGTH);
    mLimitedStream.read(mReadBuffer);
    mLimitedStream.reset();
    mLimitedStream.read(mReadBuffer);
    assertEquals(BYTES_LENGTH - LIMITED_LENGTH, mOriginalStream.available());
  }

  @Test(expected = IOException.class)
  public void testDoesNotRestIfNotMarked() throws Exception {
    mLimitedStream.read(mReadBuffer);
    mLimitedStream.reset();
  }

  @Test
  public void testMultipleMarks() throws IOException {
    mLimitedStream.mark(BYTES_LENGTH);
    assertEquals(LIMITED_LENGTH / 2, mLimitedStream.read(mReadBuffer, 0, LIMITED_LENGTH / 2));
    mLimitedStream.mark(BYTES_LENGTH);
    assertEquals(
        LIMITED_LENGTH / 2,
        mLimitedStream.read(mReadBuffer, LIMITED_LENGTH / 2, LIMITED_LENGTH / 2));
    mLimitedStream.reset();
    assertEquals(LIMITED_LENGTH / 2, mLimitedStream.read(mReadBuffer));
    assertArrayEquals(
        Arrays.copyOfRange(mReadBuffer, 0, LIMITED_LENGTH / 2),
        Arrays.copyOfRange(mReadBuffer, LIMITED_LENGTH / 2, LIMITED_LENGTH));
  }
}
