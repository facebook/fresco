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
import com.facebook.common.references.ResourceReleaser;
import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.SharedByteArray;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import com.facebook.testing.robolectric.v2.WithTestDefaultsRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;


/**
 * Tests for {@link DalvikBitmapFactory}.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@RunWith(WithTestDefaultsRunner.class)
@PrepareOnlyThisForTest({
    BitmapCounterProvider.class,
    BitmapFactory.class,
    Bitmaps.class})
public class DalvikBitmapFactoryTest {

  static {
    SoLoaderShim.setInTestMode();
  }

  private static final int IMAGE_SIZE = 5;
  private static final int LENGTH = 10;
  private static final long POINTER = 1000L;
  private static final int MAX_BITMAP_COUNT = 2;
  private static final int MAX_BITMAP_SIZE =
      MAX_BITMAP_COUNT * MockBitmapFactory.DEFAULT_BITMAP_SIZE;

  private SharedByteArray mSharedByteArray;

  private DalvikBitmapFactory mDalvikBitmapFactory;
  private CloseableReference<PooledByteBuffer> mInputImageRef;
  private byte[] mInputBuf;
  private byte[] mDecodeBuf;
  private CloseableReference<byte[]> mDecodeBufRef;
  private Bitmap mBitmap;
  private BitmapCounter mBitmapCounter;

  @Before
  public void setUp() {
    mSharedByteArray = mock(SharedByteArray.class);

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
    mInputImageRef = CloseableReference.of(input);

    mDecodeBuf = new byte[LENGTH + 2];
    mDecodeBufRef = CloseableReference.of(mDecodeBuf, mock(ResourceReleaser.class));
    when(mSharedByteArray.get(Integer.valueOf(LENGTH))).thenReturn(mDecodeBufRef);

    mockStatic(Bitmaps.class);
    mDalvikBitmapFactory = new DalvikBitmapFactory(
        null,
        mSharedByteArray);
  }

  @Test
  public void testDecode_Jpeg_Detailed() {
    setUpJpegDecode();
    CloseableReference<Bitmap> result = mDalvikBitmapFactory.decodeJPEGFromPooledByteBuffer(
        mInputImageRef,
        IMAGE_SIZE);
    verifyDecodesJpeg(result);
  }

  @Test
  public void testDecodeJpeg_incomplete() {
    when(mSharedByteArray.get(IMAGE_SIZE + 2)).thenReturn(mDecodeBufRef);
    CloseableReference<Bitmap> result =
        mDalvikBitmapFactory.decodeJPEGFromPooledByteBuffer(mInputImageRef, IMAGE_SIZE);
    verify(mSharedByteArray).get(IMAGE_SIZE + 2);
    verifyStatic();
    BitmapFactory.decodeByteArray(
        same(mDecodeBuf),
        eq(0),
        eq(IMAGE_SIZE + 2),
        argThat(new BitmapFactoryOptionsMatcher()));
    assertEquals((byte) 0xff, mDecodeBuf[5]);
    assertEquals((byte) 0xd9, mDecodeBuf[6]);
    assertEquals(1, mInputImageRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(mBitmap, result.get());
    assertTrue(result.isValid());
    assertEquals(1, mBitmapCounter.getCount());
    assertEquals(MockBitmapFactory.DEFAULT_BITMAP_SIZE, mBitmapCounter.getSize());
  }


  @Test(expected = TooManyBitmapsException.class)
  public void testHitBitmapLimit_static() {
    mBitmapCounter.increase(MockBitmapFactory.createForSize(MAX_BITMAP_SIZE));
    try {
      mDalvikBitmapFactory.decodeFromPooledByteBuffer(mInputImageRef);
    } finally {
      verify(mBitmap).recycle();
      assertEquals(1, mBitmapCounter.getCount());
      assertEquals(MAX_BITMAP_SIZE, mBitmapCounter.getSize());
    }
  }

  @Test(expected = ConcurrentModificationException.class)
  public void testPinBitmapFailure_static() {
    PowerMockito.doThrow(new ConcurrentModificationException()).when(Bitmaps.class);
    Bitmaps.pinBitmap(any(Bitmap.class));
    try {
      mDalvikBitmapFactory.decodeFromPooledByteBuffer(mInputImageRef);
    } finally {
      verify(mBitmap).recycle();
      assertEquals(0, mBitmapCounter.getCount());
      assertEquals(0, mBitmapCounter.getSize());
    }
  }

  private void verifyDecodesStatic(CloseableReference<Bitmap> result) {
    assertEquals(1, mInputImageRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(mBitmap, result.get());
    assertTrue(result.isValid());
    assertEquals(1, mBitmapCounter.getCount());
    assertEquals(MockBitmapFactory.DEFAULT_BITMAP_SIZE, mBitmapCounter.getSize());
    verifyStatic();
    Bitmaps.pinBitmap(mBitmap);
    assertFalse(CloseableReference.isValid(mDecodeBufRef));
  }

  private void setUpJpegDecode() {
    mInputBuf[3] = (byte) 0xff;
    mInputBuf[4] = (byte) 0xd9;
    when(mSharedByteArray.get(IMAGE_SIZE + 2)).thenReturn(mDecodeBufRef);
  }

  private void verifyDecodesJpeg(CloseableReference<Bitmap> result) {
    verify(mSharedByteArray).get(IMAGE_SIZE + 2);
    verifyStatic();
    BitmapFactory.decodeByteArray(
        same(mDecodeBuf),
        eq(0),
        eq(IMAGE_SIZE),
        argThat(new BitmapFactoryOptionsMatcher()));
    assertEquals(1, mInputImageRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
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
          options.inPreferredConfig == Bitmaps.BITMAP_CONFIG &&
          options.inPurgeable;
    }
  }
}
