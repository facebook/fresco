/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imagepipeline.nativecode.DalvikPurgeableDecoder;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import com.facebook.soloader.SoLoader;
import java.io.FileDescriptor;
import java.util.ConcurrentModificationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link GingerbreadPurgeableDecoder}. */
@RunWith(RobolectricTestRunner.class)
@PrepareOnlyThisForTest({
  BitmapCounterProvider.class,
  BitmapFactory.class,
  DalvikPurgeableDecoder.class,
  Bitmaps.class
})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
public class GingerbreadPurgeableDecoderTest {

  protected static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
//  protected FlexByteArrayPool mFlexByteArrayPool;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  static {
    SoLoader.setInTestMode();
  }

  protected static final int IMAGE_SIZE = 5;
  protected static final int LENGTH = 10;
  protected static final long POINTER = 1000L;
  protected static final int MAX_BITMAP_COUNT = 2;
  protected static final int MAX_BITMAP_SIZE =
      MAX_BITMAP_COUNT * MockBitmapFactory.DEFAULT_BITMAP_SIZE;

  protected GingerbreadPurgeableDecoder mGingerbreadPurgeableDecoder;
  protected CloseableReference<PooledByteBuffer> mByteBufferRef;
  protected EncodedImage mEncodedImage;
  protected byte[] mInputBuf;
  protected byte[] mDecodeBuf;
  protected CloseableReference<byte[]> mDecodeBufRef;
  protected Bitmap mBitmap;
  protected BitmapCounter mBitmapCounter;

  @Before
  public void setUp() {

    mBitmap = MockBitmapFactory.create();
    mBitmapCounter = new BitmapCounter(MAX_BITMAP_COUNT, MAX_BITMAP_SIZE);

    mockStatic(DalvikPurgeableDecoder.class);
    when(DalvikPurgeableDecoder.getBitmapFactoryOptions(anyInt(), any(Bitmap.Config.class)))
        .thenCallRealMethod();
    when(DalvikPurgeableDecoder.endsWithEOI(any(CloseableReference.class), anyInt()))
        .thenCallRealMethod();
    mockStatic(BitmapCounterProvider.class);
    when(BitmapCounterProvider.get()).thenReturn(mBitmapCounter);

    mockStatic(BitmapFactory.class);
    when(BitmapFactory.decodeFileDescriptor(
            any(FileDescriptor.class),
            any(Rect.class),
            any(BitmapFactory.Options.class)))
        .thenReturn(mBitmap);

    mInputBuf = new byte[LENGTH];
    PooledByteBuffer input = new TrivialPooledByteBuffer(mInputBuf, POINTER);
    mByteBufferRef = CloseableReference.of(input);
    mEncodedImage = new EncodedImage(mByteBufferRef);

    mDecodeBuf = new byte[LENGTH + 2];
    mDecodeBufRef = CloseableReference.of(mDecodeBuf, mock(ResourceReleaser.class));

    mockStatic(Bitmaps.class);
    mGingerbreadPurgeableDecoder = new GingerbreadPurgeableDecoder();
  }

  @Test(expected = ConcurrentModificationException.class)
  public void testPinBitmapFailure() {
    GingerbreadPurgeableDecoder decoder = mock(GingerbreadPurgeableDecoder.class);
    PowerMockito.doThrow(new ConcurrentModificationException())
        .when(decoder)
        .pinBitmap(any(Bitmap.class));
    decoder.pinBitmap(any(Bitmap.class));
    try {
      decoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
    } finally {
      verify(mBitmap).recycle();
      assertEquals(0, mBitmapCounter.getCount());
      assertEquals(0, mBitmapCounter.getSize());
    }
  }

  @Test
  public void testDecode_Jpeg_Detailed() {
    assumeNotNull(mGingerbreadPurgeableDecoder);
  }

  @Test
  public void testDecodeJpeg_incomplete() {
    assumeNotNull(mGingerbreadPurgeableDecoder);
  }
}
