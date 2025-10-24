/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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
    assertThat(mLimitedStream.available()).isEqualTo(LIMITED_LENGTH);
    assertThat(mLimitedStream.markSupported()).isTrue();
  }

  @Test
  public void testDoesReadSingleBytes() throws Exception {
    for (int i = 0; i < LIMITED_LENGTH; ++i) {
      assertThat(mLimitedStream.read()).isEqualTo(((int) mData[i]) & 0xFF);
    }
  }

  @Test
  public void testDoesNotReadTooMuch_singleBytes() throws Exception {
    for (int i = 0; i < BYTES_LENGTH; ++i) {
      final int lastByte = mLimitedStream.read();
      assertThat(lastByte == -1).isEqualTo(i >= LIMITED_LENGTH);
    }
    assertThat(mOriginalStream.available()).isEqualTo(BYTES_LENGTH - LIMITED_LENGTH);
  }

  @Test
  public void testDoesReadMultipleBytes() throws Exception {
    assertThat(mLimitedStream.read(mReadBuffer, 0, LIMITED_LENGTH)).isEqualTo(LIMITED_LENGTH);
    assertThat(Arrays.copyOfRange(mReadBuffer, 0, LIMITED_LENGTH))
        .containsExactly(Arrays.copyOfRange(mData, 0, LIMITED_LENGTH));
    assertThat(Arrays.copyOfRange(mReadBuffer, LIMITED_LENGTH, BYTES_LENGTH))
        .containsExactly(mZeroTail);
  }

  @Test
  public void testDoesNotReadTooMuch_multipleBytes() throws Exception {
    assertThat(mLimitedStream.read(mReadBuffer, 0, BYTES_LENGTH)).isEqualTo(LIMITED_LENGTH);
    final byte[] readBufferCopy = Arrays.copyOf(mReadBuffer, mReadBuffer.length);
    assertThat(mLimitedStream.read(mReadBuffer, 0, BYTES_LENGTH)).isEqualTo(-1);
    assertThat(mReadBuffer).containsExactly(readBufferCopy);
    assertThat(mOriginalStream.available()).isEqualTo(BYTES_LENGTH - LIMITED_LENGTH);
  }

  @Test
  public void testSkip() throws Exception {
    assertThat(mLimitedStream.skip(LIMITED_LENGTH / 2)).isEqualTo(LIMITED_LENGTH / 2);
    assertThat(mLimitedStream.read(mReadBuffer)).isEqualTo(LIMITED_LENGTH / 2);
    assertThat(Arrays.copyOfRange(mReadBuffer, 0, LIMITED_LENGTH / 2))
        .containsExactly(Arrays.copyOfRange(mData, LIMITED_LENGTH / 2, LIMITED_LENGTH));
  }

  @Test
  public void testDoesNotReadTooMuch_skip() throws Exception {
    assertThat(mLimitedStream.skip(BYTES_LENGTH)).isEqualTo(LIMITED_LENGTH);
    assertThat(mLimitedStream.skip(BYTES_LENGTH)).isEqualTo(0);
    assertThat(mOriginalStream.available()).isEqualTo(BYTES_LENGTH - LIMITED_LENGTH);
  }

  @Test
  public void testDoesMark() throws Exception {
    mLimitedStream.mark(BYTES_LENGTH);
    mLimitedStream.read(mReadBuffer);
    final byte[] readBufferCopy = Arrays.copyOf(mReadBuffer, mReadBuffer.length);
    Arrays.fill(mReadBuffer, (byte) 0);
    mLimitedStream.reset();
    assertThat(mLimitedStream.read(mReadBuffer)).isEqualTo(LIMITED_LENGTH);
    assertThat(mReadBuffer).containsExactly(readBufferCopy);
  }

  @Test
  public void testResetsMultipleTimes() throws Exception {
    mLimitedStream.mark(BYTES_LENGTH);
    mLimitedStream.read(mReadBuffer);
    final byte[] readBufferCopy = Arrays.copyOf(mReadBuffer, mReadBuffer.length);

    // first reset
    mLimitedStream.reset();
    assertThat(mLimitedStream.read(mReadBuffer)).isEqualTo(LIMITED_LENGTH);

    // second reset
    Arrays.fill(mReadBuffer, (byte) 0);
    mLimitedStream.reset();
    assertThat(mLimitedStream.read(mReadBuffer)).isEqualTo(LIMITED_LENGTH);

    assertThat(mReadBuffer).containsExactly(readBufferCopy);
  }

  @Test
  public void testDoesNotReadTooMuch_reset() throws Exception {
    mLimitedStream.mark(BYTES_LENGTH);
    mLimitedStream.read(mReadBuffer);
    mLimitedStream.reset();
    mLimitedStream.read(mReadBuffer);
    assertThat(mOriginalStream.available()).isEqualTo(BYTES_LENGTH - LIMITED_LENGTH);
  }

  @Test
  public void testDoesNotRestIfNotMarked() throws Exception {
    assertThatThrownBy(
            () -> {
              mLimitedStream.read(mReadBuffer);
              mLimitedStream.reset();
            })
        .isInstanceOf(IOException.class);
  }

  @Test
  public void testMultipleMarks() throws IOException {
    mLimitedStream.mark(BYTES_LENGTH);
    assertThat(mLimitedStream.read(mReadBuffer, 0, LIMITED_LENGTH / 2))
        .isEqualTo(LIMITED_LENGTH / 2);
    mLimitedStream.mark(BYTES_LENGTH);
    assertThat(mLimitedStream.read(mReadBuffer, LIMITED_LENGTH / 2, LIMITED_LENGTH / 2))
        .isEqualTo(LIMITED_LENGTH / 2);
    mLimitedStream.reset();
    assertThat(mLimitedStream.read(mReadBuffer)).isEqualTo(LIMITED_LENGTH / 2);
    assertThat(Arrays.copyOfRange(mReadBuffer, LIMITED_LENGTH / 2, LIMITED_LENGTH))
        .containsExactly(Arrays.copyOfRange(mReadBuffer, 0, LIMITED_LENGTH / 2));
  }
}
