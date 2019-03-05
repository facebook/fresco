/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

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
import com.facebook.soloader.SoLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link ArtDecoder}. */
@RunWith(RobolectricTestRunner.class)
@PrepareOnlyThisForTest({BitmapFactory.class, BitmapRegionDecoder.class})
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
public class ArtDecoderTest {

  private static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  static {
    SoLoader.setInTestMode();
  }

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

  @Before
  public void setUp() throws Exception {
    final Random random = new Random();
    random.setSeed(RANDOM_SEED);
    mEncodedBytes = new byte[ENCODED_BYTES_LENGTH];
    random.nextBytes(mEncodedBytes);

    mPooledByteBuffer = new TrivialPooledByteBuffer(mEncodedBytes);
    mBitmapPool = mock(BitmapPool.class);
    mArtDecoder = new ArtDecoder(mBitmapPool, 1, new Pools.SynchronizedPool(1));

    mByteBufferRef = CloseableReference.of(mPooledByteBuffer);
    mEncodedImage = new EncodedImage(mByteBufferRef);
    mEncodedImage.setImageFormat(DefaultImageFormats.JPEG);
    mBitmap = MockBitmapFactory.create();
    doReturn(mBitmap).when(mBitmapPool).get(MockBitmapFactory.DEFAULT_BITMAP_SIZE);

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

    mBitmapRegionDecoder = mock(BitmapRegionDecoder.class);
    whenBitmapRegionDecoderNewInstance().thenReturn(mBitmapRegionDecoder);

    ByteBuffer buf = mArtDecoder.mDecodeBuffers.acquire();
    mTempStorage = buf.array();
    mArtDecoder.mDecodeBuffers.release(buf);

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
        .thenReturn(MockBitmapFactory.create());
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

  private byte[] getDecodedBytes() {
    ArgumentCaptor<InputStream> inputStreamArgumentCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    verifyStatic(times(2));
    BitmapFactory.decodeStream(
        inputStreamArgumentCaptor.capture(), any(Rect.class), any(BitmapFactory.Options.class));
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

  private OngoingStubbing<Bitmap> whenBitmapFactoryDecodeStream() {
    mockStatic(BitmapFactory.class);
    return when(
        BitmapFactory.decodeStream(
            any(InputStream.class), isNull(Rect.class), any(BitmapFactory.Options.class)));
  }

  private OngoingStubbing<BitmapRegionDecoder> whenBitmapRegionDecoderNewInstance()
      throws IOException {
    mockStatic(BitmapRegionDecoder.class);
    return when(BitmapRegionDecoder.newInstance(any(InputStream.class), anyBoolean()));
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

  private void verifyDecodedFromStream() {
    verifyStatic(times(2));
    BitmapFactory.decodeStream(
        any(ByteArrayInputStream.class), any(Rect.class), any(BitmapFactory.Options.class));
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
