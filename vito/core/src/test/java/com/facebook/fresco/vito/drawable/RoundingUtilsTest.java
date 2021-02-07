/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawable;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.DisplayMetrics;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.drawee.drawable.RoundedColorDrawable;
import com.facebook.drawee.drawable.RoundedNinePatchDrawable;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RoundingUtilsTest {

  private Resources mResources;
  private DisplayMetrics mDisplayMetrics;

  @Before
  public void setup() {
    mResources = mock(Resources.class);
    mDisplayMetrics = new DisplayMetrics();
    when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);
  }

  @Test
  public void
      testRoundedDrawablesWithoutBorder_withBitmap_withAlreadyRounded_thenReturnBitmapDrawable() {
    RoundingUtils roundingUtils = new RoundingUtils(true);
    final Bitmap bitmap = mock(Bitmap.class);

    Drawable drawable =
        roundingUtils.roundedDrawable(mResources, bitmap, null, RoundingOptions.asCircle());

    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(BitmapDrawable.class);
  }

  @Test
  public void
      testRoundedDrawablesWithoutBorder_withBitmap_withNotAlreadyRounded_thenReturnBitmapDrawable() {
    RoundingUtils roundingUtils = new RoundingUtils();
    final Bitmap bitmap = mock(Bitmap.class);

    Drawable drawable =
        roundingUtils.roundedDrawable(mResources, bitmap, null, RoundingOptions.asCircle());

    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) drawable).isCircle()).isTrue();
  }

  @Test
  public void
      testRoundedDrawablesWithBorder_withBitmap_withAlreadyRounded_thenReturnBitmapDrawable() {
    RoundingUtils roundingUtils = new RoundingUtils(true);
    final Bitmap bitmap = mock(Bitmap.class);

    BorderOptions borderOptions = BorderOptions.create(Color.YELLOW, 10);
    Drawable drawable =
        roundingUtils.roundedDrawable(
            mResources, bitmap, borderOptions, RoundingOptions.asCircle());

    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(CircularBorderBitmapDrawable.class);
    assertThat(((CircularBorderBitmapDrawable) drawable).getBorder()).isEqualTo(borderOptions);
  }

  @Test
  public void
      testRoundedDrawablesWithBorder_withBitmap_withNotAlreadyRounded_thenReturnBitmapDrawable() {
    RoundingUtils roundingUtils = new RoundingUtils();
    final Bitmap bitmap = mock(Bitmap.class);

    BorderOptions borderOptions = BorderOptions.create(Color.YELLOW, 10);
    Drawable drawable =
        roundingUtils.roundedDrawable(
            mResources, bitmap, borderOptions, RoundingOptions.asCircle());

    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) drawable).getBorderWidth()).isEqualTo(borderOptions.width);
    assertThat(((RoundedBitmapDrawable) drawable).getBorderColor()).isEqualTo(borderOptions.color);
    assertThat(((RoundedBitmapDrawable) drawable).isCircle()).isTrue();
  }

  @Test
  public void testRoundedDrawablesWithoutBorder_withDrawable_thenReturnBitmapDrawable() {
    RoundingUtils roundingUtils = new RoundingUtils();
    RoundingOptions roundingOptions = RoundingOptions.asCircle();
    Drawable drawable = mock(BitmapDrawable.class);

    Drawable result = roundingUtils.roundedDrawable(mResources, drawable, null, roundingOptions);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) result).isCircle()).isTrue();

    drawable = mock(ColorDrawable.class);

    result = roundingUtils.roundedDrawable(mResources, drawable, null, roundingOptions);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(RoundedColorDrawable.class);
    assertThat(((RoundedColorDrawable) result).isCircle()).isTrue();

    drawable = mock(NinePatchDrawable.class);

    result = roundingUtils.roundedDrawable(mResources, drawable, null, roundingOptions);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(RoundedNinePatchDrawable.class);
    assertThat(((RoundedNinePatchDrawable) result).isCircle()).isTrue();
  }

  @Test
  public void testRoundedDrawablesWithBorder_withDrawable_thenReturnBitmapDrawable() {
    RoundingUtils roundingUtils = new RoundingUtils();
    Drawable drawable = mock(BitmapDrawable.class);
    RoundingOptions roundingOptions = RoundingOptions.asCircle();
    BorderOptions borderOptions = BorderOptions.create(Color.YELLOW, 10);

    Drawable result =
        roundingUtils.roundedDrawable(
            mResources, drawable, borderOptions, RoundingOptions.asCircle());

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) result).getBorderWidth()).isEqualTo(borderOptions.width);
    assertThat(((RoundedBitmapDrawable) result).getBorderColor()).isEqualTo(borderOptions.color);
    assertThat(((RoundedBitmapDrawable) result).isCircle()).isTrue();

    drawable = mock(ColorDrawable.class);

    result =
        roundingUtils.roundedDrawable(
            mResources, drawable, borderOptions, RoundingOptions.asCircle());

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(RoundedColorDrawable.class);
    assertThat(((RoundedColorDrawable) result).getBorderWidth()).isEqualTo(borderOptions.width);
    assertThat(((RoundedColorDrawable) result).getBorderColor()).isEqualTo(borderOptions.color);
    assertThat(((RoundedColorDrawable) result).isCircle()).isTrue();

    drawable = mock(NinePatchDrawable.class);

    result =
        roundingUtils.roundedDrawable(
            mResources, drawable, borderOptions, RoundingOptions.asCircle());

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(RoundedNinePatchDrawable.class);
    assertThat(((RoundedNinePatchDrawable) result).getBorderWidth()).isEqualTo(borderOptions.width);
    assertThat(((RoundedNinePatchDrawable) result).getBorderColor()).isEqualTo(borderOptions.color);
    assertThat(((RoundedNinePatchDrawable) result).isCircle()).isTrue();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testBuildOverlayDrawable_whenInvalidResId_thenThrowNotFoundException() {
    RoundingUtils roundingUtils = new RoundingUtils();
    Drawable drawable = mock(Drawable.class);
    RoundingOptions roundingOptions = mock(RoundingOptions.class);

    roundingUtils.roundedDrawable(mResources, drawable, null, roundingOptions);
  }
}
