/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imagepipeline.testing.MockBitmapFactory;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.powermock.core.classloader.annotations.*;
import org.robolectric.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Basic tests for BitmapPool
 */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@org.robolectric.annotation.Config(manifest= org.robolectric.annotation.Config.NONE)
public class BitmapPoolTest {

  static {
    SoLoaderShim.setInTestMode();
  }

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  public BitmapPool mPool;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            return MockBitmapFactory.create((Integer) invocation.getArguments()[0], 1);
          }
        }).when(mPool).alloc(any(Integer.class));
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            final Bitmap bitmap = (Bitmap) invocation.getArguments()[0];
            return bitmap.getWidth() * bitmap.getHeight();
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
    Bitmap bitmap3 = MockBitmapFactory.create(7, 8, Config.ARGB_4444);

    assertEquals(12, (int) mPool.getBucketedSizeForValue(bitmap1));
    assertEquals(56, (int) mPool.getBucketedSizeForValue(bitmap2));
    assertEquals(56, (int) mPool.getBucketedSizeForValue(bitmap3));
  }

  @Test
  public void testGetSizeInBytes() throws Exception {
    assertEquals(48, mPool.getSizeInBytes(12));
    assertEquals(224, mPool.getSizeInBytes(56));
  }

  // Test out bitmap reusability
  @Test
  public void testIsReusable() throws Exception {
    Bitmap b1 = mPool.alloc(12);
    assertTrue(mPool.isReusable(b1));
    Bitmap b2 = MockBitmapFactory.create(3, 4);
    assertTrue(mPool.isReusable(b2));
    Bitmap b3 = MockBitmapFactory.create(3, 4, Config.ARGB_4444);
    assertFalse(mPool.isReusable(b3));
    Bitmap b4 = MockBitmapFactory.create(3, 4);
    doReturn(true).when(b4).isRecycled();
    assertFalse(mPool.isReusable(b4));
    Bitmap b5 = MockBitmapFactory.create(3, 4);
    doReturn(false).when(b5).isMutable();
    assertFalse(mPool.isReusable(b5));
  }
}
