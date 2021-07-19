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
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BitmapDrawableFactoryTest {

  private Resources mResources;
  private DisplayMetrics mDisplayMetrics;

  private ImageOptionsDrawableFactory mDrawableFactory;

  private static final Map<String, Object> mImageExtrasRounded =
      new HashMap<String, Object>() {
        {
          put("is_rounded", true);
        }
      };
  private static final Map<String, Object> mImageExtrasNotRounded =
      new HashMap<String, Object>() {
        {
          put("is_rounded", true);
        }
      };

  @Before
  public void setup() {
    mResources = mock(Resources.class);
    mDisplayMetrics = new DisplayMetrics();
    when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);

    mDrawableFactory = new BitmapDrawableFactory(mResources);
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
  public void
      testCreateDrawable_whenRoundAsCircleAndJavaRounding_thenReturnRoundedBitmapDrawable() {
    final CloseableStaticBitmap closeableImage = mock(CloseableStaticBitmap.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(closeableImage.getUnderlyingBitmap()).thenReturn(bitmap);

    final ImageOptions options = mock(ImageOptions.class);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.asCircle());

    ImageOptionsDrawableFactory factory = new BitmapDrawableFactory(mResources);

    Drawable drawable = factory.createDrawable(closeableImage, options);
    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable.class);
  }

  @Test
  public void testCreateDrawable_whenRoundAsCircleAndNativeRounding_thenReturnBitmapDrawable() {
    final CloseableStaticBitmap closeableImage = mock(CloseableStaticBitmap.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(closeableImage.getUnderlyingBitmap()).thenReturn(bitmap);
    when(closeableImage.getExtras()).thenReturn(mImageExtrasRounded);

    final ImageOptions options = mock(ImageOptions.class);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.asCircle());

    Drawable drawable = mDrawableFactory.createDrawable(closeableImage, options);
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
    when(closeableImage.getExtras()).thenReturn(mImageExtrasNotRounded);

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
    when(closeableImage.getExtras()).thenReturn(mImageExtrasNotRounded);

    Drawable drawable = mDrawableFactory.createDrawable(closeableImage, options);
    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) drawable).getRadii())
        .isEqualTo(new float[] {1, 1, 2, 2, 3, 3, 4, 4});
  }

  @Test
  public void testCreateDrawable_whenAlreadyRoundedWithBorder_thenReturnBitmapDrawable() {
    final CloseableStaticBitmap closeableImage = mock(CloseableStaticBitmap.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(closeableImage.getUnderlyingBitmap()).thenReturn(bitmap);
    when(closeableImage.getExtras()).thenReturn(mImageExtrasRounded);

    final ImageOptions options = mock(ImageOptions.class);
    BorderOptions borderOptions = BorderOptions.create(Color.YELLOW, 10);

    when(options.getBorderOptions()).thenReturn(borderOptions);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.asCircle(false, false));
    Drawable drawable = mDrawableFactory.createDrawable(closeableImage, options);

    assertThat(drawable).isNotNull();
    assertThat(drawable).isInstanceOf(CircularBorderBitmapDrawable.class);
    assertThat(((CircularBorderBitmapDrawable) drawable).getBorder()).isEqualTo(borderOptions);
  }
}
