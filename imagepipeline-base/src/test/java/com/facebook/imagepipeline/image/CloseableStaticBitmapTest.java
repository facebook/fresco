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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CloseableStaticBitmapTest {

  private Bitmap mBitmap;
  private CloseableStaticBitmap mCloseableStaticBitmap;

  @Before
  public void setUp() {
    mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    ResourceReleaser<Bitmap> releaser = SimpleBitmapReleaser.getInstance();
    mCloseableStaticBitmap = new CloseableStaticBitmap(
        mBitmap, releaser, ImmutableQualityInfo.FULL_QUALITY, 0);
  }

  @Test
  public void testClose() {
    mCloseableStaticBitmap.close();
    assertTrue(mCloseableStaticBitmap.isClosed());
    assertTrue(mBitmap.isRecycled());
  }

  @Test
  public void testConvert() {
    CloseableReference<Bitmap> ref = mCloseableStaticBitmap.convertToBitmapReference();
    assertSame(ref.get(), mBitmap);
    assertTrue(mCloseableStaticBitmap.isClosed());
  }

  @Test(expected = NullPointerException.class)
  public void testCannotConvertIfClosed() {
    mCloseableStaticBitmap.close();
    mCloseableStaticBitmap.convertToBitmapReference();
  }
}
