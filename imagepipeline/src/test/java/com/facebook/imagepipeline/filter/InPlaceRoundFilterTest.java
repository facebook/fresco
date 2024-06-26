/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter;

import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;
import com.facebook.imageutils.BitmapUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class InPlaceRoundFilterTest {

  private final int BITMAP_DIMENSION = (int) BitmapUtil.MAX_BITMAP_DIMENSION;

  @Test
  public void whenMaximumSizeBitmap_thenRoundingReturnsWithoutError() {
    Bitmap bitmap =
        Bitmap.createBitmap(BITMAP_DIMENSION, BITMAP_DIMENSION, Bitmap.Config.ARGB_8888);
    assertNotNull(bitmap);
    InPlaceRoundFilter.roundBitmapInPlace(bitmap);
    bitmap.recycle();
  }

  @Test
  public void whenOnePixelBitmap_thenRoundingReturnsWithoutError() {
    Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    InPlaceRoundFilter.roundBitmapInPlace(bitmap);
    bitmap.recycle();
  }

  @Test(expected = NullPointerException.class)
  public void whenNullBitmap_thenRoundingReturnsWithError() {
    InPlaceRoundFilter.roundBitmapInPlace(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenEmptyBitmap_thenRoundingReturnsWithError() {
    Bitmap bitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888);
    InPlaceRoundFilter.roundBitmapInPlace(bitmap);
    bitmap.recycle();
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenTooBigBitmap_thenRoundingReturnsWithError() {
    Bitmap bitmap =
        Bitmap.createBitmap(BITMAP_DIMENSION + 1, BITMAP_DIMENSION + 1, Bitmap.Config.ARGB_8888);
    InPlaceRoundFilter.roundBitmapInPlace(bitmap);
    bitmap.recycle();
  }
}
