/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imageutils.BitmapUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

/** Basic tests for BitmapPool */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class BitmapPoolTest {

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  public BucketsBitmapPool mPool;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                int size = (Integer) invocation.getArguments()[0];
                return MockBitmapFactory.create(
                    1,
                    (int) Math.ceil(size / (double) BitmapUtil.RGB_565_BYTES_PER_PIXEL),
                    Bitmap.Config.RGB_565);
              }
            })
        .when(mPool)
        .alloc(any(Integer.class));
    doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                final Bitmap bitmap = (Bitmap) invocation.getArguments()[0];
                return BitmapUtil.getSizeInByteForBitmap(
                    bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
              }
            })
        .when(mPool)
        .getBucketedSizeForValue(any(Bitmap.class));
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
    assertThat((int) mPool.getBucketedSize(12)).isEqualTo(12);
    assertThat((int) mPool.getBucketedSize(56)).isEqualTo(56);
  }

  // tests out the getBucketedSizeForValue method
  @Test
  public void testGetBucketedSizeForValue() throws Exception {
    Bitmap bitmap1 = mPool.alloc(12);
    Bitmap bitmap2 = mPool.alloc(56);
    Bitmap bitmap3 = MockBitmapFactory.create(7, 8, Config.RGB_565);
    Bitmap bitmap4 = MockBitmapFactory.create(7, 8, Config.ARGB_8888);
    assertThat((int) mPool.getBucketedSizeForValue(bitmap1)).isEqualTo(12);
    assertThat((int) mPool.getBucketedSizeForValue(bitmap2)).isEqualTo(56);
    assertThat((int) mPool.getBucketedSizeForValue(bitmap3)).isEqualTo(112);
    assertThat((int) mPool.getBucketedSizeForValue(bitmap4)).isEqualTo(224);
  }

  @Test
  public void testGetSizeInBytes() throws Exception {
    assertThat(mPool.getSizeInBytes(48)).isEqualTo(48);
    assertThat(mPool.getSizeInBytes(224)).isEqualTo(224);
  }

  // Test out bitmap reusability
  @Test
  public void testIsReusable() throws Exception {
    Bitmap b1 = mPool.alloc(12);
    assertThat(mPool.isReusable(b1)).isTrue();
    Bitmap b2 = MockBitmapFactory.create(3, 4, Bitmap.Config.ARGB_8888);
    assertThat(mPool.isReusable(b2)).isTrue();
    Bitmap b3 = MockBitmapFactory.create(3, 4, Config.ARGB_4444);
    assertThat(mPool.isReusable(b3)).isTrue();
    Bitmap b4 = MockBitmapFactory.create(3, 4, Bitmap.Config.ARGB_8888);
    doReturn(true).when(b4).isRecycled();
    assertThat(mPool.isReusable(b4)).isFalse();
    Bitmap b5 = MockBitmapFactory.create(3, 4, Bitmap.Config.ARGB_8888);
    doReturn(false).when(b5).isMutable();
    assertThat(mPool.isReusable(b5)).isFalse();
  }
}
