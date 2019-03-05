/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.soloader.SoLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;

/** Basic tests for BitmapPool */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class BitmapPoolTest {

  static {
    SoLoader.setInTestMode();
  }

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  public BucketsBitmapPool mPool;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            int size=(Integer) invocation.getArguments()[0];
            return MockBitmapFactory.create(
                1,
                (int) Math.ceil(size / (double) BitmapUtil.RGB_565_BYTES_PER_PIXEL),
                Bitmap.Config.RGB_565);
          }
        }).when(mPool).alloc(any(Integer.class));
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            final Bitmap bitmap = (Bitmap) invocation.getArguments()[0];
            return BitmapUtil.getSizeInByteForBitmap(
                bitmap.getWidth(),
                bitmap.getHeight(),
                bitmap.getConfig());
          }
        }).when(mPool).getBucketedSizeForValue(any(Bitmap.class));
  }

  @Test
  public void testFree() throws Exception {
    Bitmap bitmap = mPool.alloc(12);
    mPool.free(bitmap);
    verify(bitmap).recycle();
  }

  // tests out the getBucketedSize method
  @Test
  public void testGetBucketedSize() throws Exception {
    assertEquals(12, (int) mPool.getBucketedSize(12));
    assertEquals(56, (int) mPool.getBucketedSize(56));
  }

  // tests out the getBucketedSizeForValue method
  @Test
  public void testGetBucketedSizeForValue() throws Exception {
    Bitmap bitmap1 = mPool.alloc(12);
    Bitmap bitmap2 = mPool.alloc(56);
    Bitmap bitmap3 = MockBitmapFactory.create(7, 8, Config.RGB_565);
    Bitmap bitmap4 = MockBitmapFactory.create(7, 8, Config.ARGB_8888);
    assertEquals(12, (int) mPool.getBucketedSizeForValue(bitmap1));
    assertEquals(56, (int) mPool.getBucketedSizeForValue(bitmap2));
    assertEquals(112, (int) mPool.getBucketedSizeForValue(bitmap3));
    assertEquals(224, (int) mPool.getBucketedSizeForValue(bitmap4));
  }

  @Test
  public void testGetSizeInBytes() throws Exception {
    assertEquals(48, mPool.getSizeInBytes(48));
    assertEquals(224, mPool.getSizeInBytes(224));
  }

  // Test out bitmap reusability
  @Test
  public void testIsReusable() throws Exception {
    Bitmap b1 = mPool.alloc(12);
    assertTrue(mPool.isReusable(b1));
    Bitmap b2 = MockBitmapFactory.create(3, 4, Bitmap.Config.ARGB_8888);
    assertTrue(mPool.isReusable(b2));
    Bitmap b3 = MockBitmapFactory.create(3, 4, Config.ARGB_4444);
    assertTrue(mPool.isReusable(b3));
    Bitmap b4 = MockBitmapFactory.create(3, 4, Bitmap.Config.ARGB_8888);
    doReturn(true).when(b4).isRecycled();
    assertFalse(mPool.isReusable(b4));
    Bitmap b5 = MockBitmapFactory.create(3, 4, Bitmap.Config.ARGB_8888);
    doReturn(false).when(b5).isMutable();
    assertFalse(mPool.isReusable(b5));
  }
}
