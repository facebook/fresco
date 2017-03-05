/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.image;

import android.graphics.Bitmap;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.bitmaps.SimpleBitmapReleaser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CloseableStaticBitmapTest {

  private static final int WIDTH = 10;
  private static final int HEIGHT = 5;

  private Bitmap mBitmap;
  private CloseableStaticBitmap mCloseableStaticBitmap;

  @Before
  public void setUp() {
    mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
    ResourceReleaser<Bitmap> releaser = SimpleBitmapReleaser.getInstance();
    mCloseableStaticBitmap = new CloseableStaticBitmap(
        mBitmap, releaser, ImmutableQualityInfo.FULL_QUALITY, 0);
  }

  @Test
  public void testWidthAndHeight() {
    assertThat(mCloseableStaticBitmap.getWidth()).isEqualTo(WIDTH);
    assertThat(mCloseableStaticBitmap.getHeight()).isEqualTo(HEIGHT);
  }

  @Test
  public void testWidthAndHeightWithRotatedImage() {
    // Reverse width and height as the rotation angle should put them back again
    mBitmap = Bitmap.createBitmap(HEIGHT, WIDTH, Bitmap.Config.ARGB_8888);
    ResourceReleaser<Bitmap> releaser = SimpleBitmapReleaser.getInstance();
    mCloseableStaticBitmap = new CloseableStaticBitmap(
        mBitmap, releaser, ImmutableQualityInfo.FULL_QUALITY, 90);

    assertThat(mCloseableStaticBitmap.getWidth()).isEqualTo(WIDTH);
    assertThat(mCloseableStaticBitmap.getHeight()).isEqualTo(HEIGHT);
  }

  @Test
  public void testClose() {
    mCloseableStaticBitmap.close();
    assertThat(mCloseableStaticBitmap.isClosed()).isTrue();
    assertThat(mBitmap.isRecycled()).isTrue();
  }

  @Test
  public void testConvert() {
    CloseableReference<Bitmap> ref = mCloseableStaticBitmap.convertToBitmapReference();
    assertThat(ref.get()).isSameAs(mBitmap);
    assertThat(mCloseableStaticBitmap.isClosed()).isTrue();
  }

  @Test(expected = NullPointerException.class)
  public void testCannotConvertIfClosed() {
    mCloseableStaticBitmap.close();
    mCloseableStaticBitmap.convertToBitmapReference();
  }
}
