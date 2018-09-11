/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import com.facebook.common.internal.ByteStreams;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import java.io.IOException;
import java.util.Arrays;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;

@RunWith(RobolectricTestRunner.class)
public class ProgressiveJpegParserTest {

  @Mock public ResourceReleaser mResourceReleaser;

  private ProgressiveJpegParser mProgressiveJpegParser;
  private byte[] mWebpBytes;
  private byte[] mPartialWebpBytes;
  private byte[] mJpegBytes;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    ByteArrayPool byteArrayPool = mock(ByteArrayPool.class);
    when(byteArrayPool.get(anyInt())).thenReturn(new byte[10]);
    mProgressiveJpegParser = new ProgressiveJpegParser(byteArrayPool);

    mJpegBytes = ByteStreams.toByteArray(
        ProgressiveJpegParserTest.class.getResourceAsStream("images/image.jpg"));
    mWebpBytes = ByteStreams.toByteArray(
        ProgressiveJpegParserTest.class.getResourceAsStream(("images/image.webp")));
    mPartialWebpBytes = new byte[mWebpBytes.length / 2];
    System.arraycopy(mWebpBytes, 0, mPartialWebpBytes, 0, mPartialWebpBytes.length);
  }

  @Test
  public void testOnPartialWebp() {
    final TrivialPooledByteBuffer byteBuffer = new TrivialPooledByteBuffer(mPartialWebpBytes);
    mProgressiveJpegParser.parseMoreData(buildEncodedImage(byteBuffer));
    assertFalse(mProgressiveJpegParser.isJpeg());
  }

  @Test
  public void testOnWebp() {
    final TrivialPooledByteBuffer byteBuffer = new TrivialPooledByteBuffer(mWebpBytes);
    mProgressiveJpegParser.parseMoreData(buildEncodedImage(byteBuffer));
    assertFalse(mProgressiveJpegParser.isJpeg());
  }

  @Test
  public void testOnTooShortImage() {
    final TrivialPooledByteBuffer shortByteBuffer = new TrivialPooledByteBuffer(
        new byte[] {(byte) 0xff});
    assertFalse(mProgressiveJpegParser.isJpeg());
    assertFalse(mProgressiveJpegParser.parseMoreData(buildEncodedImage(shortByteBuffer)));
    assertFalse(mProgressiveJpegParser.isJpeg());
    assertEquals(0, mProgressiveJpegParser.getBestScanEndOffset());
    assertEquals(0, mProgressiveJpegParser.getBestScanNumber());
  }

  @Test
  public void testOnShortestJpeg() {
    final TrivialPooledByteBuffer shortByteBuffer = new TrivialPooledByteBuffer(
        new byte[] {(byte) 0xff, (byte) 0xd8});
    assertFalse(mProgressiveJpegParser.parseMoreData(buildEncodedImage(shortByteBuffer)));
    assertTrue(mProgressiveJpegParser.isJpeg());
    assertEquals(0, mProgressiveJpegParser.getBestScanEndOffset());
    assertEquals(0, mProgressiveJpegParser.getBestScanNumber());
  }

  @Test
  public void testBasic() {
    byte[] veryFakeJpeg = new byte[] {
        (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xda,
        (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xda, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0xff, (byte) 0xda,
        (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0xff, (byte) 0xda};

    testFirstNBytes(veryFakeJpeg, 3, false, 0, 0);
    testFirstNBytes(veryFakeJpeg, 6, false, 0, 0);
    testFirstNBytes(veryFakeJpeg, 8, false, 0, 0);
    testFirstNBytes(veryFakeJpeg, 13, true, 1, 11);
    testFirstNBytes(veryFakeJpeg, 13, false, 1, 11);
    testFirstNBytes(veryFakeJpeg, 17, false, 1, 11);
    testFirstNBytes(veryFakeJpeg, 18, true, 2, 16);
    testFirstNBytes(veryFakeJpeg, 20, false, 2, 16);
    testFirstNBytes(veryFakeJpeg, veryFakeJpeg.length, true, 3, 21);
  }

  @Test
  public void testOnRealJpeg() {
    testFirstNBytes(mJpegBytes, 7000, true, 1, 4332);
    testFirstNBytes(mJpegBytes, mJpegBytes.length, true, 10, 32844);
  }

  /**
   * Feeds mProgressiveJpegParser with n initial bytes from byteArray and checks that
   *
   * @param byteArray
   * @param n
   * @param foundNewScan expected return value of ProgressiveJpegParser.parseMoreData
   * @param expectedBestScan expected number of scans found by the parser + 1
   * @param bestScanEndOffset offset of expected best scan found so far
   */
  private void testFirstNBytes(
      byte[] byteArray,
      int n,
      boolean foundNewScan,
      int expectedBestScan,
      int bestScanEndOffset) {
    assertEquals(
        foundNewScan,
        mProgressiveJpegParser.parseMoreData(buildEncodedImage(
                new TrivialPooledByteBuffer(Arrays.copyOf(byteArray, n)))));
    assertTrue(mProgressiveJpegParser.isJpeg());
    assertEquals(expectedBestScan, mProgressiveJpegParser.getBestScanNumber());
    assertEquals(bestScanEndOffset, mProgressiveJpegParser.getBestScanEndOffset());
  }

  private EncodedImage buildEncodedImage(TrivialPooledByteBuffer byteBuffer) {
    return new EncodedImage(CloseableReference.<PooledByteBuffer>of(byteBuffer));
  }
}
