/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import java.util.ConcurrentModificationException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * Base class for Tests about {@link DalvikBitmapFactory} implementations.
 */
@RunWith(RobolectricTestRunner.class)
@PrepareOnlyThisForTest({
    BitmapCounterProvider.class,
    BitmapFactory.class,
    Bitmaps.class})
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
public class DalvikBitmapFactoryTest {

  protected static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  static {
    SoLoaderShim.setInTestMode();
  }

  protected static final int IMAGE_SIZE = 5;
  protected static final int LENGTH = 10;
  protected static final long POINTER = 1000L;
  protected static final int MAX_BITMAP_COUNT = 2;
  protected static final int MAX_BITMAP_SIZE =
      MAX_BITMAP_COUNT * MockBitmapFactory.DEFAULT_BITMAP_SIZE;

  protected FlexByteArrayPool mFlexByteArrayPool;

  protected DalvikBitmapFactory mDalvikBitmapFactory;
  protected CloseableReference<PooledByteBuffer> mByteBufferRef;
  protected EncodedImage mEncodedImage;
  protected byte[] mInputBuf;
  protected byte[] mDecodeBuf;
  protected CloseableReference<byte[]> mDecodeBufRef;
  protected Bitmap mBitmap;
  protected BitmapCounter mBitmapCounter;

  @Before
  public void setUp() {
    mFlexByteArrayPool = mock(FlexByteArrayPool.class);

    mBitmap = MockBitmapFactory.create();
    mBitmapCounter = new BitmapCounter(MAX_BITMAP_COUNT, MAX_BITMAP_SIZE);

    mockStatic(BitmapCounterProvider.class);
    when(BitmapCounterProvider.get()).thenReturn(mBitmapCounter);

    mockStatic(BitmapFactory.class);
    when(BitmapFactory.decodeByteArray(
            any(byte[].class),
            anyInt(),
            anyInt(),
            any(BitmapFactory.Options.class)))
        .thenReturn(mBitmap);

    mInputBuf = new byte[LENGTH];
    PooledByteBuffer input = new TrivialPooledByteBuffer(mInputBuf, POINTER);
    mByteBufferRef = CloseableReference.of(input);
    mEncodedImage = new EncodedImage(mByteBufferRef);

    mDecodeBuf = new byte[LENGTH + 2];
    mDecodeBufRef = CloseableReference.of(mDecodeBuf, mock(ResourceReleaser.class));
    when(mFlexByteArrayPool.get(Integer.valueOf(LENGTH))).thenReturn(mDecodeBufRef);

    mockStatic(Bitmaps.class);
    mDalvikBitmapFactory = createDalvikBitmapFactoryImpl(mFlexByteArrayPool);
  }

  /**
   * Specialization provides the specific DalvikBitmapFactory implementation to test
   * @param flexByteArrayPool The ByteArraypool
   * @return The DalvikBitmapFactory implementation to test
   */
  protected DalvikBitmapFactory createDalvikBitmapFactoryImpl(
      FlexByteArrayPool flexByteArrayPool) {
    return null;
  }

  @Test
  public void testDecode_Jpeg_Detailed() {
    assumeNotNull(mDalvikBitmapFactory);
  }

  @Test
  public void testDecodeJpeg_incomplete() {
    assumeNotNull(mDalvikBitmapFactory);
  }
}
