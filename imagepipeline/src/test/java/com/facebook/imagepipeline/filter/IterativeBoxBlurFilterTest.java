/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;
import com.facebook.imageutils.BitmapUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class IterativeBoxBlurFilterTest {

  private final int BITMAP_DIMENSION = (int) BitmapUtil.MAX_BITMAP_DIMENSION;
  private final Bitmap mBitmap =
      Bitmap.createBitmap(BITMAP_DIMENSION, BITMAP_DIMENSION, Bitmap.Config.ARGB_8888);

  @Test
  public void testBitmapBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(mBitmap, 1, 4);
    assertNotNull(mBitmap);
    assertEquals(mBitmap.getWidth(), BITMAP_DIMENSION);
    assertEquals(mBitmap.getHeight(), BITMAP_DIMENSION);
    assertEquals(mBitmap.getConfig(), Bitmap.Config.ARGB_8888);
  }

  @Test
  public void maxRadiusBitmapBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(mBitmap, 1, RenderScriptBlurFilter.BLUR_MAX_RADIUS);
    assertNotNull(mBitmap);
    assertEquals(mBitmap.getWidth(), BITMAP_DIMENSION);
    assertEquals(mBitmap.getHeight(), BITMAP_DIMENSION);
    assertEquals(mBitmap.getConfig(), Bitmap.Config.ARGB_8888);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidNegativeRadiusBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(mBitmap, 1, -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidZeroRadiusBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(mBitmap, 1, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidBigRadiusBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(
        mBitmap, 1, RenderScriptBlurFilter.BLUR_MAX_RADIUS + 1);
  }
}
