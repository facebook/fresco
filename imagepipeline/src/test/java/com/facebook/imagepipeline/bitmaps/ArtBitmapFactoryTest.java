/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;

import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Throwables;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import com.facebook.imageutils.JfifUtil;

import org.junit.Rule;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * Tests for {@link ArtBitmapFactory}.
 */
@RunWith(RobolectricTestRunner.class)
@PrepareOnlyThisForTest({BitmapFactory.class})
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
public class ArtBitmapFactoryTest {

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  static {
    SoLoaderShim.setInTestMode();
  }

  private static final int RANDOM_SEED = 10101;
  private static final int ENCODED_BYTES_LENGTH = 128;

  private BitmapPool mBitmapPool;
  private PooledByteBuffer mPooledByteBuffer;
  private CloseableReference<PooledByteBuffer> mByteBufferRef;

  private ArtBitmapFactory mArtBitmapFactory;

  public Bitmap mBitmap;
  public Answer<Bitmap> mBitmapFactoryDefaultAnswer;
  private EncodedImage mEncodedImage;
  private byte[] mEncodedBytes;
  private byte[] mTempStorage;

  @Before
  public void setUp() throws Exception {
    final Random random = new Random();
    random.setSeed(RANDOM_SEED);
    mEncodedBytes = new byte[ENCODED_BYTES_LENGTH];
    random.nextBytes(mEncodedBytes);

    mPooledByteBuffer = new TrivialPooledByteBuffer(mEncodedBytes);
    mBitmapPool = mock(BitmapPool.class);
    mArtBitmapFactory = new ArtBitmapFactory(mBitmapPool, 1, false);

    mByteBufferRef = CloseableReference.of(mPooledByteBuffer);
    mEncodedImage = new EncodedImage(mByteBufferRef);
    mEncodedImage.setImageFormat(ImageFormat.JPEG);
    mBitmap = MockBitmapFactory.create();
    doReturn(mBitmap).when(mBitmapPool).get(MockBitmapFactory.DEFAULT_BITMAP_PIXELS);

    mBitmapFactoryDefaultAnswer = new Answer<Bitmap>() {
      @Override
      public Bitmap answer(InvocationOnMock invocation) throws Throwable {
        final BitmapFactory.Options options = (BitmapFactory.Options) invocation.getArguments()[2];
        options.outWidth = MockBitmapFactory.DEFAULT_BITMAP_WIDTH;
        options.outHeight = MockBitmapFactory.DEFAULT_BITMAP_HEIGHT;
        verifyBitmapFactoryOptions(options);
        return options.inJustDecodeBounds ? null : mBitmap;
      }
    };
    whenBitmapFactoryDecodeStream().thenAnswer(mBitmapFactoryDefaultAnswer);

    ByteBuffer buf = mArtBitmapFactory.mDecodeBuffers.acquire();
    mTempStorage = buf.array();
    mArtBitmapFactory.mDecodeBuffers.release(buf);

  }

  @Test
  public void testDecodeStaticDecodesFromStream() {
    mArtBitmapFactory.decodeFromEncodedImage(mEncodedImage);
    verifyDecodedFromStream();
  }

  @Test
  public void testDecodeStaticDoesNotLeak() {
    mArtBitmapFactory.decodeFromEncodedImage(mEncodedImage);
    verifyNoLeaks();
  }

  @Test
  public void testStaticImageUsesPooledByteBufferWithPixels() {
    CloseableReference<Bitmap> decodedImage =
        mArtBitmapFactory.decodeFromEncodedImage(mEncodedImage);
    closeAndVerifyClosed(decodedImage);
  }

  @Test(expected = NullPointerException.class)
  public void testPoolsReturnsNull() {
    doReturn(null).when(mBitmapPool).get(anyInt());
    mArtBitmapFactory.decodeFromEncodedImage(mEncodedImage);
  }

  @Test(expected = IllegalStateException.class)
  public void testBitmapFactoryReturnsNewBitmap() {
    whenBitmapFactoryDecodeStream()
        .thenAnswer(mBitmapFactoryDefaultAnswer)
        .thenReturn(MockBitmapFactory.create());
    try {
      mArtBitmapFactory.decodeFromEncodedImage(mEncodedImage);
    } finally {
      verify(mBitmapPool).release(mBitmap);
    }
  }

  @Test(expected = ConcurrentModificationException.class)
  public void testBitmapFactoryThrowsAnException() {
    whenBitmapFactoryDecodeStream()
        .thenAnswer(mBitmapFactoryDefaultAnswer)
        .thenThrow(new ConcurrentModificationException());
    try {
      mArtBitmapFactory.decodeFromEncodedImage(mEncodedImage);
    } finally {
      verify(mBitmapPool).release(mBitmap);
    }
  }

  @Test
  public void testDecodeJpeg_allBytes_complete() {
    jpegTestCase(true, ENCODED_BYTES_LENGTH);
  }

  @Test
  public void testDecodeJpeg_notAllBytes_complete() {
    jpegTestCase(true, ENCODED_BYTES_LENGTH / 2);
  }

  @Test
  public void testDecodeJpeg_allBytes_incomplete() {
    jpegTestCase(false, ENCODED_BYTES_LENGTH);
  }

  @Test
  public void testDecodeJpeg_notAllBytes_incomplete() {
    jpegTestCase(false, ENCODED_BYTES_LENGTH / 2);
  }

  private void jpegTestCase(boolean complete, int dataLength) {
    if (complete) {
      mEncodedBytes[dataLength - 2] = (byte) JfifUtil.MARKER_FIRST_BYTE;
      mEncodedBytes[dataLength - 1] = (byte) JfifUtil.MARKER_EOI;
    }
    CloseableReference<Bitmap> result =
        mArtBitmapFactory.decodeJPEGFromEncodedImage(mEncodedImage, dataLength);
    verifyDecodedFromStream();
    verifyNoLeaks();
    verifyDecodedBytes(complete, dataLength);
    closeAndVerifyClosed(result);
  }

  private byte[] getDecodedBytes() {
    ArgumentCaptor<InputStream> inputStreamArgumentCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    verifyStatic(times(2));
    BitmapFactory.decodeStream(
        inputStreamArgumentCaptor.capture(),
        isNull(Rect.class),
        any(BitmapFactory.Options.class));
    InputStream decodedStream = inputStreamArgumentCaptor.getValue();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ByteStreams.copy(decodedStream, baos);
    } catch (IOException ioe) {
      throw Throwables.propagate(ioe);
    }
    return baos.toByteArray();
  }

  private void verifyBitmapFactoryOptions(BitmapFactory.Options options) {
    if (!options.inJustDecodeBounds) {
      assertTrue(options.inDither);
      assertTrue(options.inMutable);
      assertSame(Bitmaps.BITMAP_CONFIG, options.inPreferredConfig);
      assertNotNull(options.inBitmap);
      assertSame(mTempStorage, options.inTempStorage);
      final int inBitmapWidth = options.inBitmap.getWidth();
      final int inBitmapHeight = options.inBitmap.getHeight();
      assertTrue(inBitmapWidth * inBitmapHeight >= MockBitmapFactory.DEFAULT_BITMAP_PIXELS);
    }
  }

  private OngoingStubbing<Bitmap> whenBitmapFactoryDecodeStream() {
    mockStatic(BitmapFactory.class);
    return when(BitmapFactory.decodeStream(
            any(InputStream.class),
            isNull(Rect.class),
            any(BitmapFactory.Options.class)));
  }

  private void closeAndVerifyClosed(CloseableReference<Bitmap> closeableImage) {
    verify(mBitmapPool, never()).release(mBitmap);
    closeableImage.close();
    verify(mBitmapPool).release(mBitmap);
  }

  private void verifyNoLeaks() {
    assertEquals(2, mByteBufferRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  private void verifyDecodedFromStream() {
    verifyStatic(times(2));
    BitmapFactory.decodeStream(
        any(ByteArrayInputStream.class),
        isNull(Rect.class),
        any(BitmapFactory.Options.class));
  }

  private void verifyDecodedBytes(boolean complete, int length) {
    byte[] decodedBytes = getDecodedBytes();
    assertArrayEquals(
        Arrays.copyOfRange(
            mEncodedBytes,
            0,
            length),
        Arrays.copyOfRange(
            decodedBytes,
            0,
            length));
    if (complete) {
      assertEquals(length, decodedBytes.length);
    } else {
      assertEquals(length + 2, decodedBytes.length);
      assertEquals((byte) JfifUtil.MARKER_FIRST_BYTE, decodedBytes[length]);
      assertEquals((byte) JfifUtil.MARKER_EOI, decodedBytes[length + 1]);
    }
  }
}
