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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BitmapDrawableFactoryTest {

  private Resources mResources;
  private DisplayMetrics mDisplayMetrics;
  private FrescoExperiments mExperiments;

  private ImageOptionsDrawableFactory mDrawableFactory;

  @Before
  public void setup() {
    mResources = mock(Resources.class);
    mDisplayMetrics = new DisplayMetrics();
    when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);

    mExperiments = mock(FrescoExperiments.class);

    mDrawableFactory = new BitmapDrawableFactory(mResources, mExperiments);
  }

  @Test
  public void testCreateDrawable_whenImageUnknown_thenReturnNull() {
    final CloseableImage closeableImage = mock(CloseableImage.class);
    final ImageOptions options = mock(ImageOptions.class);

    Drawable drawable = mDrawableFactory.createDrawable(closeableImage, options);
    assertThat(drawable).isNull();
  }

  @Test
  public void testCreateDrawable_whenImageIsStaticBitmap_thenReturnBitmapDrawable() {
    final CloseableStaticBitmap closeableImage = mock(CloseableStaticBitmap.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(closeableImage.getUnderlyingBitmap()).thenReturn(bitmap);

    final ImageOptions options = mock(ImageOptions.class);

    Drawable drawable = mDrawableFactory.createDrawable(closeableImage, options);
    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(BitmapDrawable.class);
  }

  @Test
  public void testCreateDrawable_whenRoundAsCircle_thenReturnBitmapDrawable() {
    final CloseableStaticBitmap closeableImage = mock(CloseableStaticBitmap.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(closeableImage.getUnderlyingBitmap()).thenReturn(bitmap);

    final ImageOptions options = mock(ImageOptions.class);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.asCircle());

    when(mExperiments.useNativeRounding()).thenReturn(false);

    Drawable drawable = mDrawableFactory.createDrawable(closeableImage, options);
    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable.class);

    when(mExperiments.useNativeRounding()).thenReturn(true);

    drawable = mDrawableFactory.createDrawable(closeableImage, options);
    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(BitmapDrawable.class);
  }

  @Test
  public void testCreateDrawable_whenRoundWithCornerRadius_thenReturnBitmapDrawable() {
    final CloseableStaticBitmap closeableImage = mock(CloseableStaticBitmap.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(closeableImage.getUnderlyingBitmap()).thenReturn(bitmap);

    final ImageOptions options = mock(ImageOptions.class);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.forCornerRadiusPx(123));

    Drawable drawable = mDrawableFactory.createDrawable(closeableImage, options);
    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) drawable).getRadii())
        .isEqualTo(new float[] {123, 123, 123, 123, 123, 123, 123, 123});
  }

  @Test
  public void testCreateDrawable_whenRoundWithCornerRadii_thenReturnBitmapDrawable() {
    final CloseableStaticBitmap closeableImage = mock(CloseableStaticBitmap.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(closeableImage.getUnderlyingBitmap()).thenReturn(bitmap);

    final ImageOptions options = mock(ImageOptions.class);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.forCornerRadii(1, 2, 3, 4));

    Drawable drawable = mDrawableFactory.createDrawable(closeableImage, options);
    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) drawable).getRadii())
        .isEqualTo(new float[] {1, 1, 2, 2, 3, 3, 4, 4});
  }
}
