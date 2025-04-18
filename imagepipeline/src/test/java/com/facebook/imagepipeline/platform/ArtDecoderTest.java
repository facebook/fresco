/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Build;
import androidx.core.util.Pools;
import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Throwables;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import com.facebook.imageutils.JfifUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link ArtDecoder}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
public class ArtDecoderTest {

  private static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

  private static final int RANDOM_SEED = 10101;
  private static final int ENCODED_BYTES_LENGTH = 128;

  private BitmapPool mBitmapPool;
  private PooledByteBuffer mPooledByteBuffer;
  private CloseableReference<PooledByteBuffer> mByteBufferRef;
  private BitmapRegionDecoder mBitmapRegionDecoder;

  private ArtDecoder mArtDecoder;

  public Bitmap mBitmap;
  public Answer<Bitmap> mBitmapFactoryDefaultAnswer;
  private EncodedImage mEncodedImage;
  private byte[] mEncodedBytes;
  private byte[] mTempStorage;
  private static MockedStatic<BitmapFactory> mockedBitmapFactory;
  private static MockedStatic<BitmapRegionDecoder> mockedBitmapRegionDecoder;

  @Before
  public void setUp() throws Exception {
    mockedBitmapRegionDecoder = mockStatic(BitmapRegionDecoder.class);
    mockedBitmapFactory = mockStatic(BitmapFactory.class);
    final Random random = new Random();
    random.setSeed(RANDOM_SEED);
    mEncodedBytes = new byte[ENCODED_BYTES_LENGTH];
    random.nextBytes(mEncodedBytes);

    mPooledByteBuffer = new TrivialPooledByteBuffer(mEncodedBytes);
    mBitmapPool = mock(BitmapPool.class);
    Pools.SynchronizedPool<ByteBuffer> pool = new Pools.SynchronizedPool<ByteBuffer>(1);
    pool.release(ByteBuffer.allocate(16 * 1024));
    mArtDecoder = new ArtDecoder(mBitmapPool, pool, new PlatformDecoderOptions());

    mByteBufferRef = CloseableReference.of(mPooledByteBuffer);
    mEncodedImage = new EncodedImage(mByteBufferRef);
    mEncodedImage.setImageFormat(DefaultImageFormats.JPEG);
    mBitmap = MockBitmapFactory.create();
    doReturn(mBitmap).when(mBitmapPool).get(MockBitmapFactory.DEFAULT_BITMAP_SIZE);

    mBitmapFactoryDefaultAnswer =
        invocation -> {
          final BitmapFactory.Options options =
              (BitmapFactory.Options) invocation.getArguments()[2];
          options.outWidth = MockBitmapFactory.DEFAULT_BITMAP_WIDTH;
          options.outHeight = MockBitmapFactory.DEFAULT_BITMAP_HEIGHT;
          verifyBitmapFactoryOptions(options);
          return options.inJustDecodeBounds ? null : mBitmap;
        };
    whenBitmapFactoryDecodeStream().thenAnswer(mBitmapFactoryDefaultAnswer);

    mBitmapRegionDecoder = mock(BitmapRegionDecoder.class);
    whenBitmapRegionDecoderNewInstance()
        .thenAnswer((Answer<BitmapRegionDecoder>) invocation -> mBitmapRegionDecoder);

    ByteBuffer buf = mArtDecoder.mDecodeBuffers.acquire();
    mTempStorage = buf.array();
    mArtDecoder.mDecodeBuffers.release(buf);
  }

  @After
  public void tearDownStaticMocks() {
    mockedBitmapFactory.close();
    mockedBitmapRegionDecoder.close();
  }

  @Test
  public void testDecodeStaticDecodesFromStream() {
    mArtDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
    verifyDecodedFromStream();
  }

  @Test
  public void testDecodeStaticDoesNotLeak() {
    mArtDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
    verifyNoLeaks();
  }

  @Test
  public void testStaticImageUsesPooledByteBufferWithPixels() {
    CloseableReference<Bitmap> decodedImage =
        mArtDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
    closeAndVerifyClosed(decodedImage);
  }

  @Test(expected = NullPointerException.class)
  public void testPoolsReturnsNull() {
    doReturn(null).when(mBitmapPool).get(anyInt());
    mArtDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
  }

  @Test(expected = IllegalStateException.class)
  public void testBitmapFactoryReturnsNewBitmap() {
    whenBitmapFactoryDecodeStream()
        .thenAnswer(mBitmapFactoryDefaultAnswer)
        .thenAnswer((Answer<Bitmap>) invocation -> MockBitmapFactory.create());
    try {
      mArtDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
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
      mArtDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
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

  @Test
  public void testDecodeJpeg_regionDecodingEnabled() {
    Rect region = new Rect(0, 0, 200, 100);
    int size = MockBitmapFactory.bitmapSize(region.width(), region.height(), DEFAULT_BITMAP_CONFIG);

    Bitmap bitmap =
        MockBitmapFactory.create(region.width(), region.height(), DEFAULT_BITMAP_CONFIG);

    when(mBitmapRegionDecoder.decodeRegion(any(Rect.class), any(BitmapFactory.Options.class)))
        .thenReturn(bitmap);

    doReturn(bitmap).when(mBitmapPool).get(size);
    CloseableReference<Bitmap> decodedImage =
        mArtDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, region);

    assertTrue(decodedImage.get().getWidth() == region.width());
    assertTrue(decodedImage.get().getHeight() == region.height());
    closeAndVerifyClosed(decodedImage, bitmap);
    verify(mBitmapRegionDecoder).recycle();
  }

  @Test
  public void testDecodeFromEncodedImage_regionDecodingEnabled() {
    Rect region = new Rect(0, 0, 200, 100);
    int size = MockBitmapFactory.bitmapSize(region.width(), region.height(), DEFAULT_BITMAP_CONFIG);

    Bitmap bitmap =
        MockBitmapFactory.create(region.width(), region.height(), DEFAULT_BITMAP_CONFIG);

    when(mBitmapRegionDecoder.decodeRegion(any(Rect.class), any(BitmapFactory.Options.class)))
        .thenReturn(bitmap);

    doReturn(bitmap).when(mBitmapPool).get(size);
    CloseableReference<Bitmap> decodedImage =
        mArtDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, region);

    assertTrue(decodedImage.get().getWidth() == region.width());
    assertTrue(decodedImage.get().getHeight() == region.height());
    closeAndVerifyClosed(decodedImage, bitmap);
    verify(mBitmapRegionDecoder).recycle();
  }

  private void jpegTestCase(boolean complete, int dataLength) {
    if (complete) {
      mEncodedBytes[dataLength - 2] = (byte) JfifUtil.MARKER_FIRST_BYTE;
      mEncodedBytes[dataLength - 1] = (byte) JfifUtil.MARKER_EOI;
    }
    CloseableReference<Bitmap> result =
        mArtDecoder.decodeJPEGFromEncodedImage(
            mEncodedImage, DEFAULT_BITMAP_CONFIG, null, dataLength);
    verifyDecodedFromStream();
    verifyNoLeaks();
    verifyDecodedBytes(complete, dataLength);
    closeAndVerifyClosed(result);
  }

  private static byte[] getDecodedBytes() {
    ArgumentCaptor<InputStream> inputStreamArgumentCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    mockedBitmapFactory.verify(
        () ->
            BitmapFactory.decodeStream(
                inputStreamArgumentCaptor.capture(),
                isNull(Rect.class),
                any(BitmapFactory.Options.class)),
        times(2));
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
      assertNotNull(options.inBitmap);
      assertSame(mTempStorage, options.inTempStorage);
      final int inBitmapWidth = options.inBitmap.getWidth();
      final int inBitmapHeight = options.inBitmap.getHeight();
      assertTrue(inBitmapWidth * inBitmapHeight >= MockBitmapFactory.DEFAULT_BITMAP_PIXELS);
    }
  }

  private static OngoingStubbing<Bitmap> whenBitmapFactoryDecodeStream() {
    return mockedBitmapFactory.when(
        () ->
            BitmapFactory.decodeStream(
                any(InputStream.class), isNull(Rect.class), any(BitmapFactory.Options.class)));
  }

  private static OngoingStubbing<BitmapRegionDecoder> whenBitmapRegionDecoderNewInstance()
      throws IOException {
    return mockedBitmapRegionDecoder.when(
        () -> BitmapRegionDecoder.newInstance(any(InputStream.class), anyBoolean()));
  }

  private void closeAndVerifyClosed(CloseableReference<Bitmap> closeableImage) {
    verify(mBitmapPool, never()).release(mBitmap);
    closeableImage.close();
    verify(mBitmapPool).release(mBitmap);
  }

  private void closeAndVerifyClosed(CloseableReference<Bitmap> closeableImage, Bitmap bitmap) {
    verify(mBitmapPool, never()).release(bitmap);
    closeableImage.close();
    verify(mBitmapPool).release(bitmap);
  }

  private void verifyNoLeaks() {
    assertEquals(2, mByteBufferRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  private static void verifyDecodedFromStream() {
    mockedBitmapFactory.verify(
        () ->
            BitmapFactory.decodeStream(
                (ByteArrayInputStream) anyObject(),
                isNull(Rect.class),
                any(BitmapFactory.Options.class)),
        times(2));
  }

  private void verifyDecodedBytes(boolean complete, int length) {
    byte[] decodedBytes = getDecodedBytes();
    assertArrayEquals(
        Arrays.copyOfRange(mEncodedBytes, 0, length), Arrays.copyOfRange(decodedBytes, 0, length));
    if (complete) {
      assertEquals(length, decodedBytes.length);
    } else {
      assertEquals(length + 2, decodedBytes.length);
      assertEquals((byte) JfifUtil.MARKER_FIRST_BYTE, decodedBytes[length]);
      assertEquals((byte) JfifUtil.MARKER_EOI, decodedBytes[length + 1]);
    }
  }
}
