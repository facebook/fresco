/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.generic;

import java.util.Arrays;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import com.facebook.drawee.drawable.AndroidGraphicsTestUtils;
import com.facebook.drawee.drawable.ScalingUtils;
import org.robolectric.RobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class GenericDraweeHierarchyBuilderTest {

  private final Drawable mBackgroundDrawable1 = mock(BitmapDrawable.class);
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
  private final Matrix mActualImageMatrix = mock(Matrix.class);
  private final PointF mFocusPoint = mock(PointF.class);
  private final RoundingParams mRoundingParams = mock(RoundingParams.class);

  private void testInitialState(GenericDraweeHierarchyBuilder builder) {
    assertEquals(300, builder.getFadeDuration());
    assertEquals(null, builder.getPlaceholderImage());
    assertEquals(null, builder.getPlaceholderImageScaleType());
    assertEquals(null, builder.getRetryImage());
    assertEquals(null, builder.getRetryImageScaleType());
    assertEquals(null, builder.getFailureImage());
    assertEquals(null, builder.getFailureImageScaleType());
    assertEquals(null, builder.getProgressBarImage());
    assertEquals(null, builder.getProgressBarImageScaleType());
    assertEquals(ScalingUtils.ScaleType.CENTER_CROP, builder.getActualImageScaleType());
    assertEquals(null, builder.getActualImageMatrix());
    assertEquals(null, builder.getActualImageFocusPoint());
    assertEquals(null, builder.getBackgrounds());
    assertEquals(null, builder.getOverlays());
    assertEquals(null, builder.getRoundingParams());
  }

  @Test
  public void testBuilder() throws Exception {
    GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(null);

    // test defaults
    testInitialState(builder);

    // test fade duration
    builder.setFadeDuration(100);
    assertEquals(100, builder.getFadeDuration());

    // test image setters with default scale type
    builder.setPlaceholderImage(mPlaceholderDrawable1);
    builder.setRetryImage(mRetryDrawable1);
    builder.setFailureImage(mFailureDrawable1);
    builder.setProgressBarImage(mProgressBarDrawable1);
    assertEquals(mPlaceholderDrawable1, builder.getPlaceholderImage());
    assertEquals(ScalingUtils.ScaleType.CENTER_INSIDE, builder.getPlaceholderImageScaleType());
    assertEquals(mRetryDrawable1, builder.getRetryImage());
    assertEquals(ScalingUtils.ScaleType.CENTER_INSIDE, builder.getRetryImageScaleType());
    assertEquals(mFailureDrawable1, builder.getFailureImage());
    assertEquals(ScalingUtils.ScaleType.CENTER_INSIDE, builder.getFailureImageScaleType());
    assertEquals(mProgressBarDrawable1, builder.getProgressBarImage());
    assertEquals(ScalingUtils.ScaleType.CENTER_INSIDE, builder.getProgressBarImageScaleType());

    // test image setters with explicit scale type
    builder.setPlaceholderImage(mPlaceholderDrawable2, ScalingUtils.ScaleType.CENTER);
    builder.setRetryImage(mRetryDrawable2, ScalingUtils.ScaleType.FIT_CENTER);
    builder.setFailureImage(mFailureDrawable2, ScalingUtils.ScaleType.FIT_END);
    builder.setProgressBarImage(mProgressBarDrawable2, ScalingUtils.ScaleType.CENTER_CROP);
    assertEquals(mPlaceholderDrawable2, builder.getPlaceholderImage());
    assertEquals(ScalingUtils.ScaleType.CENTER, builder.getPlaceholderImageScaleType());
    assertEquals(mRetryDrawable2, builder.getRetryImage());
    assertEquals(ScalingUtils.ScaleType.FIT_CENTER, builder.getRetryImageScaleType());
    assertEquals(mFailureDrawable2, builder.getFailureImage());
    assertEquals(ScalingUtils.ScaleType.FIT_END, builder.getFailureImageScaleType());
    assertEquals(mProgressBarDrawable2, builder.getProgressBarImage());
    assertEquals(ScalingUtils.ScaleType.CENTER_CROP, builder.getProgressBarImageScaleType());

    // test actual image matrix
    builder.setActualImageMatrix(mActualImageMatrix);
    assertSame(mActualImageMatrix, builder.getActualImageMatrix());
    assertSame(null, builder.getActualImageScaleType());

    // test actual image scale type
    builder.setActualImageScaleType(ScalingUtils.ScaleType.FIT_START);
    assertEquals(ScalingUtils.ScaleType.FIT_START, builder.getActualImageScaleType());

    // test actual image focus point
    builder.setActualImageFocusPoint(mFocusPoint);
    AndroidGraphicsTestUtils.assertEquals(mFocusPoint, builder.getActualImageFocusPoint(), 0f);
    builder.setActualImageScaleType(ScalingUtils.ScaleType.FOCUS_CROP);
    assertSame(ScalingUtils.ScaleType.FOCUS_CROP, builder.getActualImageScaleType());
    assertSame(null, builder.getActualImageMatrix());

    // test backgrounds & overlays
    builder.setBackgrounds(Arrays.asList(mBackgroundDrawable1, mBackgroundDrawable2));
    builder.setOverlays(Arrays.asList(mOverlayDrawable1, mOverlayDrawable2));
    assertArrayEquals(
        builder.getBackgrounds().toArray(),
        new Drawable[]{mBackgroundDrawable1, mBackgroundDrawable2});
    assertArrayEquals(
        builder.getOverlays().toArray(),
        new Drawable[]{mOverlayDrawable1, mOverlayDrawable2});
    builder.setBackground(mBackgroundDrawable2);
    builder.setOverlay(mOverlayDrawable2);
    builder.setPressedStateOverlay(mPressedStateDrawable);
    assertArrayEquals(builder.getBackgrounds().toArray(), new Drawable[]{mBackgroundDrawable2});
    assertArrayEquals(builder.getOverlays().toArray(), new Drawable[] {mOverlayDrawable2});
    assertEquals(builder.getPressedStateOverlay().getClass(), StateListDrawable.class);

    // test rounding params
    builder.setRoundingParams(mRoundingParams);
    assertEquals(mRoundingParams, builder.getRoundingParams());

    // test reset
    builder.reset();
    testInitialState(builder);
  }
}
