/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.drawee.drawable.RoundedColorDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.vito.core.Hierarcher;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.fresco.vito.options.RoundingOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HierarcherImplTest {

  private static final int RES_ID = 123;
  private static final int INVALID_RES_ID = 999;

  private Resources mResources;
  private DisplayMetrics mDisplayMetrics;
  private Drawable mDrawable;

  private Hierarcher mHierarcher;

  private ImageOptionsDrawableFactory mDrawableFactory;

  @Before
  public void setup() {
    mResources = mock(Resources.class);
    mDrawable = mock(Drawable.class);
    mDrawableFactory = mock(ImageOptionsDrawableFactory.class);
    mDisplayMetrics = new DisplayMetrics();
    when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);
    when(mResources.getDrawable(eq(RES_ID))).thenReturn(mDrawable);
    when(mResources.getDrawable(not(eq(RES_ID)))).thenThrow(new Resources.NotFoundException());

    mHierarcher = new HierarcherImpl(mDrawableFactory);
  }

  @Test
  public void testBuildPlaceholderRes() {
    ImageOptions options = ImageOptions.create().placeholderRes(RES_ID).build();

    Drawable errorDrawable = mHierarcher.buildPlaceholderDrawable(mResources, options);

    assertThat(errorDrawable).isNotNull();
    assertThat(errorDrawable).isInstanceOf(ScaleTypeDrawable.class);

    ScaleTypeDrawable scaleTypeDrawable = (ScaleTypeDrawable) errorDrawable;
    assertThat(scaleTypeDrawable.getScaleType())
        .isEqualTo(ImageOptions.defaults().getPlaceholderScaleType());
    assertThat(scaleTypeDrawable.getFocusPoint())
        .isEqualTo(ImageOptions.defaults().getPlaceholderFocusPoint());
    assertThat(scaleTypeDrawable.getCurrent()).isEqualTo(mDrawable);
  }

  @Test
  public void testBuildPlaceholderDrawable() {
    final Drawable expected = new ColorDrawable(Color.YELLOW);
    ImageOptions options =
        ImageOptions.create().placeholder(expected).placeholderScaleType(null).build();

    final Drawable result = mHierarcher.buildPlaceholderDrawable(mResources, options);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testBuildPlaceholderDrawableScale() {
    final Drawable expected = new ColorDrawable(Color.YELLOW);
    ImageOptions options =
        ImageOptions.create()
            .placeholder(expected)
            .placeholderScaleType(ScalingUtils.ScaleType.CENTER)
            .build();

    final Drawable result = mHierarcher.buildPlaceholderDrawable(mResources, options);

    assertThat(result).isExactlyInstanceOf(ScaleTypeDrawable.class);
    assertThat(result.getCurrent()).isEqualTo(expected);
  }

  @Test
  public void testApplyRoundingOptions_whenRoundAsCircle_thenReturnDrawable() {
    final Drawable drawable = new ColorDrawable(Color.YELLOW);
    when(mResources.getDrawable(RES_ID)).thenReturn(drawable);

    ImageOptions options = mock(ImageOptions.class);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.asCircle());
    when(options.getPlaceholderDrawable()).thenReturn(drawable);
    when(options.getPlaceholderApplyRoundingOptions()).thenReturn(true);

    Drawable result = mHierarcher.buildPlaceholderDrawable(mResources, options);

    assertThat(result).isExactlyInstanceOf(RoundedColorDrawable.class);

    when(options.getPlaceholderDrawable()).thenReturn(null);
    when(options.getPlaceholderRes()).thenReturn(RES_ID);

    result = mHierarcher.buildPlaceholderDrawable(mResources, options);

    assertThat(result).isExactlyInstanceOf(RoundedColorDrawable.class);
  }

  @Test
  public void testApplyRoundingOptions_whenRoundWithCornerRadius_thenReturnDrawable() {
    final BitmapDrawable drawable = mock(BitmapDrawable.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(drawable.getBitmap()).thenReturn(bitmap);

    ImageOptions options = mock(ImageOptions.class);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.forCornerRadiusPx(123));
    when(options.getPlaceholderDrawable()).thenReturn(drawable);
    when(options.getPlaceholderApplyRoundingOptions()).thenReturn(true);

    Drawable result = mHierarcher.buildPlaceholderDrawable(mResources, options);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) result).getRadii())
        .isEqualTo(new float[] {123, 123, 123, 123, 123, 123, 123, 123});
  }

  @Test
  public void testApplyRoundingOptions_whenRoundWithCornerRadii_thenReturnDrawable() {
    final BitmapDrawable drawable = mock(BitmapDrawable.class);
    final Bitmap bitmap = mock(Bitmap.class);
    when(drawable.getBitmap()).thenReturn(bitmap);

    ImageOptions options = mock(ImageOptions.class);
    when(options.getRoundingOptions()).thenReturn(RoundingOptions.forCornerRadii(1, 2, 3, 4));
    when(options.getPlaceholderDrawable()).thenReturn(drawable);
    when(options.getPlaceholderApplyRoundingOptions()).thenReturn(true);

    Drawable result = mHierarcher.buildPlaceholderDrawable(mResources, options);

    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(RoundedBitmapDrawable.class);
    assertThat(((RoundedBitmapDrawable) result).getRadii())
        .isEqualTo(new float[] {1, 1, 2, 2, 3, 3, 4, 4});
  }

  @Test
  public void testBuildErrorDrawable_whenNoScaleTypeSet_thenUseDefaultScaleType() {
    ImageOptions options = ImageOptions.create().errorRes(RES_ID).build();

    Drawable errorDrawable = mHierarcher.buildErrorDrawable(mResources, options);

    assertThat(errorDrawable).isNotNull();
    assertThat(errorDrawable).isInstanceOf(ScaleTypeDrawable.class);

    ScaleTypeDrawable scaleTypeDrawable = (ScaleTypeDrawable) errorDrawable;
    assertThat(scaleTypeDrawable.getScaleType())
        .isEqualTo(ImageOptions.defaults().getErrorScaleType());
    assertThat(scaleTypeDrawable.getFocusPoint())
        .isEqualTo(ImageOptions.defaults().getErrorFocusPoint());
    assertThat(scaleTypeDrawable.getCurrent()).isEqualTo(mDrawable);
  }

  @Test
  public void testBuildErrorDrawable_whenScaleTypeNull_thenDoNotWrapDrawable() {
    ImageOptions options = ImageOptions.create().errorRes(RES_ID).errorScaleType(null).build();

    Drawable errorDrawable = mHierarcher.buildErrorDrawable(mResources, options);

    assertThat(errorDrawable).isEqualTo(mDrawable);
  }

  @Test(expected = Resources.NotFoundException.class)
  public void testBuildErrorDrawable_whenInvalidResId_thenThrowNotFoundException() {
    ImageOptions options = ImageOptions.create().errorRes(INVALID_RES_ID).build();

    mHierarcher.buildErrorDrawable(mResources, options);
  }

  @Test
  public void testBuildErrorDrawable_whenScaleTypeSet_thenReturnScaleTypeDrawable() {
    PointF focusPoint = new PointF(100, 234);
    ImageOptions options =
        ImageOptions.create()
            .errorRes(RES_ID)
            .errorScaleType(ScalingUtils.ScaleType.FOCUS_CROP)
            .errorFocusPoint(focusPoint)
            .build();

    Drawable errorDrawable = mHierarcher.buildErrorDrawable(mResources, options);

    assertThat(errorDrawable).isNotNull();
    assertThat(errorDrawable).isInstanceOf(ScaleTypeDrawable.class);

    ScaleTypeDrawable scaleTypeDrawable = (ScaleTypeDrawable) errorDrawable;
    assertThat(scaleTypeDrawable.getScaleType()).isEqualTo(ScalingUtils.ScaleType.FOCUS_CROP);
    assertThat(scaleTypeDrawable.getFocusPoint()).isEqualTo(focusPoint);
    assertThat(scaleTypeDrawable.getCurrent()).isEqualTo(mDrawable);
  }

  @Test
  public void testBuildErrorDrawable_whenNotSet_thenReturnNopDrawable() {
    ImageOptions options = ImageOptions.create().build();

    Drawable errorDrawable = mHierarcher.buildErrorDrawable(mResources, options);

    assertThat(errorDrawable).isNull();
  }

  @Test
  public void testBuildProgressDrawable() {
    final Drawable drawable = new ColorDrawable(0x0);
    final ImageOptions imageOptions =
        ImageOptions.create()
            .progress(drawable)
            .progressScaleType(ScalingUtils.ScaleType.FIT_CENTER)
            .build();

    final Drawable actual = mHierarcher.buildProgressDrawable(mResources, imageOptions);
    assertThat(actual).isInstanceOf(ScaleTypeDrawable.class);
    final ScaleTypeDrawable scaleTypeActual = (ScaleTypeDrawable) actual;
    assertThat(scaleTypeActual.getScaleType()).isEqualTo(ScalingUtils.ScaleType.FIT_CENTER);
    assertThat(scaleTypeActual.getCurrent()).isEqualTo(drawable);
  }

  @Test
  public void testBuildActualImageWrapper() {
    final PointF expectedFocusPoint = new PointF(1, 2);
    final ImageOptions imageOptions =
        ImageOptions.create()
            .scale(ScalingUtils.ScaleType.FIT_CENTER)
            .focusPoint(expectedFocusPoint)
            .build();

    final Drawable actual = mHierarcher.buildActualImageWrapper(imageOptions, null);
    assertThat(actual).isInstanceOf(ScaleTypeDrawable.class);
    final ScaleTypeDrawable scaleTypeActual = (ScaleTypeDrawable) actual;
    assertThat(scaleTypeActual.getScaleType()).isEqualTo(ScalingUtils.ScaleType.FIT_CENTER);
    assertThat(scaleTypeActual.getFocusPoint()).isEqualTo(expectedFocusPoint);
  }

  @Test
  public void testBuildOverlayRes_whenUnset_thenReturnNull() {
    ImageOptions options = ImageOptions.create().build();

    Drawable overlayDrawable = mHierarcher.buildOverlayDrawable(mResources, options);

    assertThat(overlayDrawable).isNull();
  }

  @Test
  public void testBuildOverlayRes_whenSet_thenReturnDrawable() {
    ImageOptions options = ImageOptions.create().overlayRes(RES_ID).build();

    Drawable overlayDrawable = mHierarcher.buildOverlayDrawable(mResources, options);

    assertThat(overlayDrawable).isEqualTo(mDrawable);
  }

  @Test(expected = Resources.NotFoundException.class)
  public void testBuildOverlayDrawable_whenInvalidResId_thenThrowNotFoundException() {
    ImageOptions options = ImageOptions.create().overlayRes(INVALID_RES_ID).build();

    mHierarcher.buildOverlayDrawable(mResources, options);
  }
}
