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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import org.robolectric.RobolectricTestRunner;

import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class TailAppendingInputStreamTest {

  private static final int RANDOM_SEED = 1023;
  private static final int BYTES_LENGTH = 1024;
  private static final int TAIL_LENGTH = 1024;
  private static final int OUTPUT_LENGTH = BYTES_LENGTH + TAIL_LENGTH;

  private byte[] mBytes;
  private byte[] mTail;

  private byte[] mOutputBuffer;
  private TailAppendingInputStream mTailAppendingInputStream;

  @Before
  public void setUp() {
    Random random = new Random();
    random.setSeed(RANDOM_SEED);
    mBytes = new byte[BYTES_LENGTH];
    mTail = new byte[TAIL_LENGTH];
    mOutputBuffer = new byte[OUTPUT_LENGTH];
    random.nextBytes(mBytes);
    random.nextBytes(mTail);
    InputStream stream = new ByteArrayInputStream(mBytes);
    mTailAppendingInputStream = new TailAppendingInputStream(stream, mTail);
  }

  @Test
  public void testDoesReadSingleBytes() throws Exception {
    for (byte b : mBytes) {
      assertEquals(((int) b) & 0xFF, mTailAppendingInputStream.read());
    }
    for (byte b : mTail) {
      assertEquals(((int) b) & 0xFF, mTailAppendingInputStream.read());
    }
  }

  @Test
  public void testDoesNotReadTooMuch_singleBytes() throws Exception {
    for (int i = 0; i < mBytes.length + mTail.length; ++i) {
      mTailAppendingInputStream.read();
    }
    assertEquals(-1, mTailAppendingInputStream.read());
  }

  @Test
  public void testDoesReadMultipleBytes() throws Exception {
    ByteStreams.readFully(mTailAppendingInputStream, mOutputBuffer);
    assertArrayEquals(mBytes, Arrays.copyOfRange(mOutputBuffer, 0, BYTES_LENGTH));
    assertArrayEquals(mTail, Arrays.copyOfRange(mOutputBuffer, BYTES_LENGTH, OUTPUT_LENGTH));
  }

  @Test
  public void testDoesNotReadTooMuch_multipleBytes() throws Exception {
    byte[] buffer = new byte[OUTPUT_LENGTH + 1];
    assertEquals(
        OUTPUT_LENGTH,
        ByteStreams.read(mTailAppendingInputStream, buffer, 0, OUTPUT_LENGTH + 1));
    assertEquals(-1, mTailAppendingInputStream.read());
  }

  @Test
  public void testUnalignedReads() throws IOException {
    assertEquals(128, mTailAppendingInputStream.read(mOutputBuffer, 256, 128));
    assertArrayEquals(
        Arrays.copyOfRange(mBytes, 0, 128),
        Arrays.copyOfRange(mOutputBuffer, 256, 384));
    Arrays.fill(mOutputBuffer, 256, 384, (byte) 0);
    for (byte b : mOutputBuffer) {
      assertEquals(0, b);
    }

    assertEquals(BYTES_LENGTH - 128, mTailAppendingInputStream.read(mOutputBuffer));
    assertArrayEquals(
        Arrays.copyOfRange(mBytes, 128, BYTES_LENGTH),
        Arrays.copyOfRange(mOutputBuffer, 0, BYTES_LENGTH - 128));
    Arrays.fill(mOutputBuffer, 0, BYTES_LENGTH - 128, (byte) 0);
    for (byte b : mOutputBuffer) {
      assertEquals(0, b);
    }

    assertEquals(128, mTailAppendingInputStream.read(mOutputBuffer, 256, 128));
    assertArrayEquals(
        Arrays.copyOfRange(mTail, 0, 128),
        Arrays.copyOfRange(mOutputBuffer, 256, 384));
    Arrays.fill(mOutputBuffer, 256, 384, (byte) 0);
    for (byte b : mOutputBuffer) {
      assertEquals(0, b);
    }

    assertEquals(TAIL_LENGTH - 128, mTailAppendingInputStream.read(mOutputBuffer));
    assertArrayEquals(
        Arrays.copyOfRange(mTail, 128, TAIL_LENGTH),
        Arrays.copyOfRange(mOutputBuffer, 0, TAIL_LENGTH - 128));
    Arrays.fill(mOutputBuffer, 0, TAIL_LENGTH - 128, (byte) 0);
    for (byte b : mOutputBuffer) {
      assertEquals(0, b);
    }

    assertEquals(-1, mTailAppendingInputStream.read());
  }

  @Test
  public void testMark() throws IOException {
    assertEquals(128, mTailAppendingInputStream.read(mOutputBuffer, 0, 128));
    mTailAppendingInputStream.mark(BYTES_LENGTH);
    assertEquals(
        BYTES_LENGTH,
        ByteStreams.read(mTailAppendingInputStream, mOutputBuffer, 0, BYTES_LENGTH));
    mTailAppendingInputStream.reset();
    for (byte b : Arrays.copyOfRange(mOutputBuffer, 0, BYTES_LENGTH)) {
      assertEquals(((int) b) & 0xFF, mTailAppendingInputStream.read());
    }
  }
}
