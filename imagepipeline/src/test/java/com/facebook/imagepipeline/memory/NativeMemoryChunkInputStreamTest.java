/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.io.IOException;
import java.util.Arrays;

import com.facebook.imagepipeline.testing.FakeNativeMemoryChunk;
import com.facebook.testing.robolectric.v2.WithTestDefaultsRunner;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link NativeMemoryChunkInputStream}
 */
@RunWith(WithTestDefaultsRunner.class)
public class NativeMemoryChunkInputStreamTest {
  private NativeMemoryChunk mChunk;
  private NativeMemoryChunkInputStream mStream;

  @Before
  public void setup() {
    mChunk = new FakeNativeMemoryChunk(64);
    mStream = new NativeMemoryChunkInputStream(mChunk, 10, 30);
  }

  private void writeBytes(NativeMemoryChunk chunk, int chunkStartOffset, byte[] buf, int length) {
    chunk.write(chunkStartOffset, buf, 0, length);
  }

  private byte[] getBytes(NativeMemoryChunk chunk, int startOffset, int length) {
    byte[] buf = new byte[length];
    chunk.read(0, buf, 0, length);
    return buf;
  }

  // assert that the first 'length' bytes of expected are the same as those in 'actual'
  private void assertArrayEquals(byte[] expected, byte[] actual, int length) {
    org.junit.Assert.assertTrue(expected.length >= length);
    org.junit.Assert.assertTrue(actual.length >= length);
    for (int i = 0; i < length; i++) {
      org.junit.Assert.assertEquals(expected[i], actual[i]);
    }
  }

  @Test
  public void testBasic() {
    mStream = new NativeMemoryChunkInputStream(mChunk, 10, 30);
    Assert.assertEquals(10, mStream.mStartOffset);
    Assert.assertEquals(10, mStream.mOffset);
    Assert.assertEquals(10, mStream.mMark);
    Assert.assertEquals(40, mStream.mEndOffset);
    Assert.assertEquals(30, mStream.available());
    Assert.assertTrue(mStream.markSupported());
  }

  @Test
  public void testMark() {
    mStream.mOffset = 37;
    mStream.mark(7);
    Assert.assertEquals(37, mStream.mMark);
    mStream.mOffset = 8;
    mStream.mark(19);
    Assert.assertEquals(8, mStream.mMark);
  }

  @Test
  public void testReset() {
    mStream.mOffset = 37;
    mStream.reset();
    Assert.assertEquals(10, mStream.mOffset);

    mStream.mOffset = 23;
    mStream.mark(0);
    mStream.reset();
    Assert.assertEquals(23, mStream.mOffset);
  }

  @Test
  public void testAvailable() {
    Assert.assertEquals(30, mStream.available());
    mStream.mOffset = 60;
    Assert.assertEquals(0, mStream.available());
    mStream.mOffset = 39;
    Assert.assertEquals(1, mStream.available());
  }

  @Test
  public void testSkip() {
    Assert.assertEquals(0, mStream.skip(-4));

    Assert.assertEquals(5, mStream.skip(5));
    Assert.assertEquals(15, mStream.mOffset);

    Assert.assertEquals(25, mStream.skip(25));
    Assert.assertEquals(40, mStream.mOffset);

    mStream.mOffset = 78;
    Assert.assertEquals(0, mStream.skip(6));
    Assert.assertEquals(78, mStream.mOffset);
  }

  @Test
  public void testReadWithErrors() {
    byte[] buf = new byte[64];

    try {
      mStream.read(buf, 10, 55);
      Assert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
      // expected
    }
  }

  @Test
  public void testRead_SingleByte() throws IOException {
    byte[] fillBytes = new byte[] {(byte)0x12, (byte)0xFF, (byte)0x56, (byte)0x78, (byte)0x90};
    mStream.mOffset = mStream.mEndOffset - 5;
    writeBytes(mChunk, mStream.mOffset, fillBytes, 5);
    Assert.assertEquals(0x12, mStream.read());
    Assert.assertEquals(0xFF, mStream.read());
    Assert.assertEquals(0x56, mStream.read());
    Assert.assertEquals(0x78, mStream.read());
    Assert.assertEquals(0x90, mStream.read());
    Assert.assertEquals(-1, mStream.read());
  }

  @Test
  public void testRead_ToByteArray() {
    byte[] buf = new byte[64];
    byte[] fillBytes = new byte[64];

    Assert.assertEquals(0, mStream.read(buf, 0, 0));
    Assert.assertEquals(10, mStream.mOffset);

    mStream.mOffset = 67;
    Assert.assertEquals(-1, mStream.read(buf, 0, 1));
    Assert.assertEquals(67, mStream.mOffset);

    mStream.mOffset = 10;
    int readLength = 10;
    Arrays.fill(fillBytes, (byte)3);
    writeBytes(mChunk, mStream.mOffset, fillBytes, readLength);
    Assert.assertEquals(readLength, mStream.read(buf, 0, readLength));
    Assert.assertEquals(20, mStream.mOffset);
    assertArrayEquals(fillBytes, buf, readLength);

    readLength = 30;
    int available = mStream.mEndOffset - mStream.mOffset;
    Assert.assertEquals(20, available);
    Arrays.fill(fillBytes, (byte)4);
    writeBytes(mChunk, mStream.mOffset, fillBytes, available);
    Assert.assertEquals(available, mStream.read(buf, 0, readLength));
    Assert.assertEquals(mStream.mEndOffset, mStream.mOffset);
    assertArrayEquals(fillBytes, buf, available);
  }

  @Test
  public void testCreateEmptyStream() throws Exception {
    NativeMemoryChunkInputStream is = new NativeMemoryChunkInputStream(mChunk, 0, 0);
    Assert.assertEquals(-1, is.read());
  }
}
