/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.streams;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.common.internal.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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
      assertThat(mTailAppendingInputStream.read()).isEqualTo(((int) b) & 0xFF);
    }
    for (byte b : mTail) {
      assertThat(mTailAppendingInputStream.read()).isEqualTo(((int) b) & 0xFF);
    }
  }

  @Test
  public void testDoesNotReadTooMuch_singleBytes() throws Exception {
    for (int i = 0; i < mBytes.length + mTail.length; ++i) {
      mTailAppendingInputStream.read();
    }
    assertThat(mTailAppendingInputStream.read()).isEqualTo(-1);
  }

  @Test
  public void testDoesReadMultipleBytes() throws Exception {
    ByteStreams.readFully(mTailAppendingInputStream, mOutputBuffer, 0, OUTPUT_LENGTH);
    assertThat(Arrays.copyOfRange(mOutputBuffer, 0, BYTES_LENGTH)).containsExactly(mBytes);
    assertThat(Arrays.copyOfRange(mOutputBuffer, BYTES_LENGTH, OUTPUT_LENGTH))
        .containsExactly(mTail);
  }

  @Test
  public void testDoesNotReadTooMuch_multipleBytes() throws Exception {
    byte[] buffer = new byte[OUTPUT_LENGTH + 1];
    assertThat(ByteStreams.read(mTailAppendingInputStream, buffer, 0, OUTPUT_LENGTH + 1))
        .isEqualTo(OUTPUT_LENGTH);
    assertThat(mTailAppendingInputStream.read()).isEqualTo(-1);
  }

  @Test
  public void testUnalignedReads() throws IOException {
    assertThat(mTailAppendingInputStream.read(mOutputBuffer, 256, 128)).isEqualTo(128);
    assertThat(Arrays.copyOfRange(mOutputBuffer, 256, 384))
        .containsExactly(Arrays.copyOfRange(mBytes, 0, 128));
    Arrays.fill(mOutputBuffer, 256, 384, (byte) 0);
    for (byte b : mOutputBuffer) {
      assertThat(b).isEqualTo((byte) 0);
    }

    assertThat(mTailAppendingInputStream.read(mOutputBuffer)).isEqualTo(BYTES_LENGTH - 128);
    assertThat(Arrays.copyOfRange(mOutputBuffer, 0, BYTES_LENGTH - 128))
        .containsExactly(Arrays.copyOfRange(mBytes, 128, BYTES_LENGTH));
    Arrays.fill(mOutputBuffer, 0, BYTES_LENGTH - 128, (byte) 0);
    for (byte b : mOutputBuffer) {
      assertThat(b).isEqualTo((byte) 0);
    }

    assertThat(mTailAppendingInputStream.read(mOutputBuffer, 256, 128)).isEqualTo(128);
    assertThat(Arrays.copyOfRange(mOutputBuffer, 256, 384))
        .containsExactly(Arrays.copyOfRange(mTail, 0, 128));
    Arrays.fill(mOutputBuffer, 256, 384, (byte) 0);
    for (byte b : mOutputBuffer) {
      assertThat(b).isEqualTo((byte) 0);
    }

    assertThat(mTailAppendingInputStream.read(mOutputBuffer)).isEqualTo(TAIL_LENGTH - 128);
    assertThat(Arrays.copyOfRange(mOutputBuffer, 0, TAIL_LENGTH - 128))
        .containsExactly(Arrays.copyOfRange(mTail, 128, TAIL_LENGTH));
    Arrays.fill(mOutputBuffer, 0, TAIL_LENGTH - 128, (byte) 0);
    for (byte b : mOutputBuffer) {
      assertThat(b).isEqualTo((byte) 0);
    }

    assertThat(mTailAppendingInputStream.read()).isEqualTo(-1);
  }

  @Test
  public void testMark() throws IOException {
    assertThat(mTailAppendingInputStream.read(mOutputBuffer, 0, 128)).isEqualTo(128);
    mTailAppendingInputStream.mark(BYTES_LENGTH);
    assertThat(ByteStreams.read(mTailAppendingInputStream, mOutputBuffer, 0, BYTES_LENGTH))
        .isEqualTo(BYTES_LENGTH);
    mTailAppendingInputStream.reset();
    for (byte b : Arrays.copyOfRange(mOutputBuffer, 0, BYTES_LENGTH)) {
      assertThat(mTailAppendingInputStream.read()).isEqualTo(((int) b) & 0xFF);
    }
  }
}
