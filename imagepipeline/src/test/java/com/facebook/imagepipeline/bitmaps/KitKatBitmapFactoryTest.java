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

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imagepipeline.testing.MockBitmapFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * Tests for {@link HoneycombBitmapFactory}.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@RunWith(RobolectricTestRunner.class)
@PrepareOnlyThisForTest({
    BitmapCounterProvider.class,
    BitmapFactory.class,
    Bitmaps.class})
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
public class KitKatBitmapFactoryTest extends DalvikBitmapFactoryTest{

  private EmptyJpegGenerator mEmptyJpegGenerator;

  @Before
  public void setUp() {
    super.setUp();
    mEmptyJpegGenerator = mock(EmptyJpegGenerator.class);
  }

  @Override
  protected DalvikBitmapFactory createDalvikBitmapFactoryImpl(
      FlexByteArrayPool flexByteArrayPool) {
    return new KitKatBitmapFactory(mEmptyJpegGenerator, flexByteArrayPool);
  }

  @Test
  public void testDecode_Jpeg_Detailed() {
    assumeNotNull(mDalvikBitmapFactory);
    setUpJpegDecode();
    CloseableReference<Bitmap> result = mDalvikBitmapFactory.decodeJPEGFromEncodedImage(
        mEncodedImage,
        DEFAULT_BITMAP_CONFIG,
        IMAGE_SIZE);
    verifyDecodesJpeg(result);
  }

  @Test
  public void testDecodeJpeg_incomplete() {
    assumeNotNull(mDalvikBitmapFactory);
    when(mFlexByteArrayPool.get(IMAGE_SIZE + 2)).thenReturn(mDecodeBufRef);
    CloseableReference<Bitmap> result =
        mDalvikBitmapFactory.decodeJPEGFromEncodedImage(
            mEncodedImage,
            DEFAULT_BITMAP_CONFIG,
            IMAGE_SIZE);
    verify(mFlexByteArrayPool).get(IMAGE_SIZE + 2);
    verifyStatic();
    BitmapFactory.decodeByteArray(
        same(mDecodeBuf),
        eq(0),
        eq(IMAGE_SIZE + 2),
        argThat(new BitmapFactoryOptionsMatcher()));
    assertEquals((byte) 0xff, mDecodeBuf[5]);
    assertEquals((byte) 0xd9, mDecodeBuf[6]);
    assertEquals(2, mByteBufferRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(mBitmap, result.get());
    assertTrue(result.isValid());
    assertEquals(1, mBitmapCounter.getCount());
    assertEquals(MockBitmapFactory.DEFAULT_BITMAP_SIZE, mBitmapCounter.getSize());
  }


  @Test(expected = TooManyBitmapsException.class)
  public void testHitBitmapLimit_static() {
    assumeNotNull(mDalvikBitmapFactory);
    mBitmapCounter.increase(
        MockBitmapFactory.createForSize(MAX_BITMAP_SIZE, DEFAULT_BITMAP_CONFIG));
    try {
      mDalvikBitmapFactory.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG);
    } finally {
      verify(mBitmap).recycle();
      assertEquals(1, mBitmapCounter.getCount());
      assertEquals(MAX_BITMAP_SIZE, mBitmapCounter.getSize());
    }
  }

  @Test(expected = ConcurrentModificationException.class)
  public void testPinBitmapFailure_static() {
    assumeNotNull(mDalvikBitmapFactory);
    PowerMockito.doThrow(new ConcurrentModificationException()).when(Bitmaps.class);
    Bitmaps.pinBitmap(any(Bitmap.class));
    try {
      mDalvikBitmapFactory.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG);
    } finally {
      verify(mBitmap).recycle();
      assertEquals(0, mBitmapCounter.getCount());
      assertEquals(0, mBitmapCounter.getSize());
    }
  }

  private void setUpJpegDecode() {
    mInputBuf[3] = (byte) 0xff;
    mInputBuf[4] = (byte) 0xd9;
    when(mFlexByteArrayPool.get(IMAGE_SIZE + 2)).thenReturn(mDecodeBufRef);
  }

  private void verifyDecodesJpeg(CloseableReference<Bitmap> result) {
    verify(mFlexByteArrayPool).get(IMAGE_SIZE + 2);
    verifyStatic();
    BitmapFactory.decodeByteArray(
        same(mDecodeBuf),
        eq(0),
        eq(IMAGE_SIZE),
        argThat(new BitmapFactoryOptionsMatcher()));
    assertEquals(2, mByteBufferRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(mBitmap, result.get());
    assertTrue(result.isValid());
    assertEquals(1, mBitmapCounter.getCount());
    assertEquals(MockBitmapFactory.DEFAULT_BITMAP_SIZE, mBitmapCounter.getSize());
  }

  private static class BitmapFactoryOptionsMatcher
      extends ArgumentMatcher<BitmapFactory.Options> {
    @Override
    public boolean matches(Object argument) {
      if (argument == null) {
        return false;
      }
      BitmapFactory.Options options = (BitmapFactory.Options) argument;
      return options.inDither &&
          options.inPurgeable;
    }
  }
}
