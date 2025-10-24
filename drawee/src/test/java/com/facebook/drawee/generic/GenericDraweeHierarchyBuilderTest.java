/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.generic;

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import com.facebook.drawee.drawable.AndroidGraphicsTestUtils;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GenericDraweeHierarchyBuilderTest {

  private final Drawable mBackgroundDrawable2 = mock(BitmapDrawable.class);
  private final Drawable mOverlayDrawable1 = mock(BitmapDrawable.class);
  private final Drawable mOverlayDrawable2 = mock(BitmapDrawable.class);
  private final BitmapDrawable mPlaceholderDrawable1 = mock(BitmapDrawable.class);
  private final BitmapDrawable mFailureDrawable1 = mock(BitmapDrawable.class);
  private final BitmapDrawable mRetryDrawable1 = mock(BitmapDrawable.class);
  private final BitmapDrawable mPlaceholderDrawable2 = mock(BitmapDrawable.class);
  private final BitmapDrawable mFailureDrawable2 = mock(BitmapDrawable.class);
  private final BitmapDrawable mRetryDrawable2 = mock(BitmapDrawable.class);
  private final BitmapDrawable mProgressBarDrawable1 = mock(BitmapDrawable.class);
  private final BitmapDrawable mProgressBarDrawable2 = mock(BitmapDrawable.class);
  private final BitmapDrawable mPressedStateDrawable = mock(BitmapDrawable.class);
  private final PointF mFocusPoint = mock(PointF.class);
  private final RoundingParams mRoundingParams = mock(RoundingParams.class);

  private void testInitialState(GenericDraweeHierarchyBuilder builder) {
    assertThat(builder.getFadeDuration()).isEqualTo(300);
    assertThat(builder.getDesiredAspectRatio()).isEqualTo(0);
    assertThat(builder.getPlaceholderImage()).isNull();
    assertThat(builder.getPlaceholderImageScaleType()).isEqualTo(ScaleType.CENTER_INSIDE);
    assertThat(builder.getRetryImage()).isNull();
    assertThat(builder.getRetryImageScaleType()).isEqualTo(ScaleType.CENTER_INSIDE);
    assertThat(builder.getFailureImage()).isNull();
    assertThat(builder.getFailureImageScaleType()).isEqualTo(ScaleType.CENTER_INSIDE);
    assertThat(builder.getProgressBarImage()).isNull();
    assertThat(builder.getProgressBarImageScaleType()).isEqualTo(ScaleType.CENTER_INSIDE);
    assertThat(builder.getActualImageScaleType()).isEqualTo(ScaleType.CENTER_CROP);
    assertThat(builder.getActualImageFocusPoint()).isNull();
    assertThat(builder.getBackground()).isNull();
    assertThat(builder.getOverlays()).isNull();
    assertThat(builder.getRoundingParams()).isNull();
  }

  @Test
  public void testBuilder() throws Exception {
    GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(null);

    // test defaults
    testInitialState(builder);

    // test fade duration
    builder.setFadeDuration(100);
    assertThat(builder.getFadeDuration()).isEqualTo(100);

    // test desired aspect ratio
    builder.setDesiredAspectRatio(1.33f);
    assertThat(builder.getDesiredAspectRatio()).isEqualTo(1.33f);

    // test image setters without modifying scale type (default expected)
    builder.setPlaceholderImage(mPlaceholderDrawable1);
    builder.setRetryImage(mRetryDrawable1);
    builder.setFailureImage(mFailureDrawable1);
    builder.setProgressBarImage(mProgressBarDrawable1);
    assertThat(builder.getPlaceholderImage()).isEqualTo(mPlaceholderDrawable1);
    assertThat(builder.getPlaceholderImageScaleType()).isEqualTo(ScaleType.CENTER_INSIDE);
    assertThat(builder.getRetryImage()).isEqualTo(mRetryDrawable1);
    assertThat(builder.getRetryImageScaleType()).isEqualTo(ScaleType.CENTER_INSIDE);
    assertThat(builder.getFailureImage()).isEqualTo(mFailureDrawable1);
    assertThat(builder.getFailureImageScaleType()).isEqualTo(ScaleType.CENTER_INSIDE);
    assertThat(builder.getProgressBarImage()).isEqualTo(mProgressBarDrawable1);
    assertThat(builder.getProgressBarImageScaleType()).isEqualTo(ScaleType.CENTER_INSIDE);

    // test image setters with explicit scale type
    builder.setPlaceholderImage(mPlaceholderDrawable2, ScaleType.CENTER);
    builder.setRetryImage(mRetryDrawable2, ScaleType.FIT_CENTER);
    builder.setFailureImage(mFailureDrawable2, ScaleType.FIT_END);
    builder.setProgressBarImage(mProgressBarDrawable2, ScaleType.CENTER_CROP);
    assertThat(builder.getPlaceholderImage()).isEqualTo(mPlaceholderDrawable2);
    assertThat(builder.getPlaceholderImageScaleType()).isEqualTo(ScaleType.CENTER);
    assertThat(builder.getRetryImage()).isEqualTo(mRetryDrawable2);
    assertThat(builder.getRetryImageScaleType()).isEqualTo(ScaleType.FIT_CENTER);
    assertThat(builder.getFailureImage()).isEqualTo(mFailureDrawable2);
    assertThat(builder.getFailureImageScaleType()).isEqualTo(ScaleType.FIT_END);
    assertThat(builder.getProgressBarImage()).isEqualTo(mProgressBarDrawable2);
    assertThat(builder.getProgressBarImageScaleType()).isEqualTo(ScaleType.CENTER_CROP);

    // test image setters without modifying scale type (previous scaletype expected)
    builder.setPlaceholderImage(mPlaceholderDrawable1);
    builder.setRetryImage(mRetryDrawable1);
    builder.setFailureImage(mFailureDrawable1);
    builder.setProgressBarImage(mProgressBarDrawable1);
    assertThat(builder.getPlaceholderImage()).isEqualTo(mPlaceholderDrawable1);
    assertThat(builder.getPlaceholderImageScaleType()).isEqualTo(ScaleType.CENTER);
    assertThat(builder.getRetryImage()).isEqualTo(mRetryDrawable1);
    assertThat(builder.getRetryImageScaleType()).isEqualTo(ScaleType.FIT_CENTER);
    assertThat(builder.getFailureImage()).isEqualTo(mFailureDrawable1);
    assertThat(builder.getFailureImageScaleType()).isEqualTo(ScaleType.FIT_END);
    assertThat(builder.getProgressBarImage()).isEqualTo(mProgressBarDrawable1);
    assertThat(builder.getProgressBarImageScaleType()).isEqualTo(ScaleType.CENTER_CROP);

    // test actual image scale type
    builder.setActualImageScaleType(ScaleType.FIT_START);
    assertThat(builder.getActualImageScaleType()).isEqualTo(ScaleType.FIT_START);

    // test actual image focus point
    builder.setActualImageFocusPoint(mFocusPoint);
    AndroidGraphicsTestUtils.assertEquals(mFocusPoint, builder.getActualImageFocusPoint(), 0f);
    builder.setActualImageScaleType(ScaleType.FOCUS_CROP);
    assertThat(builder.getActualImageScaleType()).isSameAs(ScaleType.FOCUS_CROP);

    // test backgrounds & overlays
    builder.setOverlays(Arrays.asList(mOverlayDrawable1, mOverlayDrawable2));
    assertThat(builder.getOverlays().toArray())
        .containsExactly(mOverlayDrawable1, mOverlayDrawable2);
    builder.setBackground(mBackgroundDrawable2);
    builder.setOverlay(mOverlayDrawable2);
    builder.setPressedStateOverlay(mPressedStateDrawable);
    assertThat(builder.getBackground()).isSameAs(mBackgroundDrawable2);
    assertThat(builder.getOverlays().toArray()).containsExactly(mOverlayDrawable2);
    assertThat(builder.getPressedStateOverlay().getClass()).isEqualTo(StateListDrawable.class);
    // test clearing backgrounds & overlays
    builder.setBackground(null);
    assertThat(builder.getBackground()).isNull();
    builder.setOverlay(null);
    assertThat(builder.getOverlays()).isNull();
    builder.setPressedStateOverlay(null);
    assertThat(builder.getPressedStateOverlay()).isNull();

    // test rounding params
    builder.setRoundingParams(mRoundingParams);
    assertThat(builder.getRoundingParams()).isEqualTo(mRoundingParams);

    // test reset
    builder.reset();
    testInitialState(builder);
  }
}
