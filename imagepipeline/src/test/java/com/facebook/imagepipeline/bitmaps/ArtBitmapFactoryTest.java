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
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import com.facebook.imageutils.JfifUtil;
import com.facebook.testing.robolectric.v2.WithTestDefaultsRunner;

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
@RunWith(WithTestDefaultsRunner.class)
@PrepareOnlyThisForTest({BitmapFactory.class})
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ArtBitmapFactoryTest {

  static {
    SoLoaderShim.setInTestMode();
  }

  private static final int RANDOM_SEED = 10101;
  private static final int ENCODED_BYTES_LENGTH = 128;

  private BitmapPool mBitmapPool;
  private PooledByteBuffer mPooledByteBuffer;

  private ArtBitmapFactory mArtBitmapFactory;

  public Bitmap mBitmap;
  public Answer<Bitmap> mBitmapFactoryDefaultAnswer;
  private CloseableReference<PooledByteBuffer> mEncodedImageRef;
  private byte[] mEncodedBytes;


  @Before
  public void setUp() throws Exception {
    final Random random = new Random();
    random.setSeed(RANDOM_SEED);
    mEncodedBytes = new byte[ENCODED_BYTES_LENGTH];
    random.nextBytes(mEncodedBytes);

    mPooledByteBuffer = new TrivialPooledByteBuffer(mEncodedBytes);
    mBitmapPool = mock(BitmapPool.class);
    mArtBitmapFactory = new ArtBitmapFactory(mBitmapPool);

    mEncodedImageRef = CloseableReference.of(mPooledByteBuffer);
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
  }

  @Test
  public void testDecodeStaticDecodesFromStream() {
    mArtBitmapFactory.decodeFromPooledByteBuffer(mEncodedImageRef);
    verifyDecodedFromStream();
  }

  @Test
  public void testDecodeStaticDoesNotLeak() {
    mArtBitmapFactory.decodeFromPooledByteBuffer(mEncodedImageRef);
    verifyNoLeaks();
  }

  @Test
  public void testStaticImageUsesPooledByteBufferWithPixels() {
    CloseableReference<Bitmap> decodedImage =
        mArtBitmapFactory.decodeFromPooledByteBuffer(mEncodedImageRef);
    closeAndVerifyClosed(decodedImage);
  }

  @Test(expected = NullPointerException.class)
  public void testPoolsReturnsNull() {
    doReturn(null).when(mBitmapPool).get(anyInt());
    mArtBitmapFactory.decodeFromPooledByteBuffer(mEncodedImageRef);
  }

  @Test(expected = IllegalStateException.class)
  public void testBitmapFactoryReturnsNewBitmap() {
    whenBitmapFactoryDecodeStream()
        .thenAnswer(mBitmapFactoryDefaultAnswer)
        .thenReturn(MockBitmapFactory.create());
    try {
      mArtBitmapFactory.decodeFromPooledByteBuffer(mEncodedImageRef);
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
      mArtBitmapFactory.decodeFromPooledByteBuffer(mEncodedImageRef);
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
        mArtBitmapFactory.decodeJPEGFromPooledByteBuffer(mEncodedImageRef, dataLength);
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
    assertNotNull(options.inTempStorage);
    if (!options.inJustDecodeBounds) {
      assertTrue(options.inDither);
      assertTrue(options.inMutable);
      assertSame(Bitmaps.BITMAP_CONFIG, options.inPreferredConfig);
      assertNotNull(options.inBitmap);
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
    assertEquals(1, mEncodedImageRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
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
