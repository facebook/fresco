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

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;

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
    assertEquals(0, builder.getDesiredAspectRatio(), 0);
    assertEquals(null, builder.getPlaceholderImage());
    assertEquals(ScaleType.CENTER_INSIDE, builder.getPlaceholderImageScaleType());
    assertEquals(null, builder.getRetryImage());
    assertEquals(ScaleType.CENTER_INSIDE, builder.getRetryImageScaleType());
    assertEquals(null, builder.getFailureImage());
    assertEquals(ScaleType.CENTER_INSIDE, builder.getFailureImageScaleType());
    assertEquals(null, builder.getProgressBarImage());
    assertEquals(ScaleType.CENTER_INSIDE, builder.getProgressBarImageScaleType());
    assertEquals(ScaleType.CENTER_CROP, builder.getActualImageScaleType());
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

    // test desired aspect ratio
    builder.setDesiredAspectRatio(1.33f);
    assertEquals(1.33f, builder.getDesiredAspectRatio(), 0);

    // test image setters without modifying scale type (default expected)
    builder.setPlaceholderImage(mPlaceholderDrawable1);
    builder.setRetryImage(mRetryDrawable1);
    builder.setFailureImage(mFailureDrawable1);
    builder.setProgressBarImage(mProgressBarDrawable1);
    assertEquals(mPlaceholderDrawable1, builder.getPlaceholderImage());
    assertEquals(ScaleType.CENTER_INSIDE, builder.getPlaceholderImageScaleType());
    assertEquals(mRetryDrawable1, builder.getRetryImage());
    assertEquals(ScaleType.CENTER_INSIDE, builder.getRetryImageScaleType());
    assertEquals(mFailureDrawable1, builder.getFailureImage());
    assertEquals(ScaleType.CENTER_INSIDE, builder.getFailureImageScaleType());
    assertEquals(mProgressBarDrawable1, builder.getProgressBarImage());
    assertEquals(ScaleType.CENTER_INSIDE, builder.getProgressBarImageScaleType());

    // test image setters with explicit scale type
    builder.setPlaceholderImage(mPlaceholderDrawable2, ScaleType.CENTER);
    builder.setRetryImage(mRetryDrawable2, ScaleType.FIT_CENTER);
    builder.setFailureImage(mFailureDrawable2, ScaleType.FIT_END);
    builder.setProgressBarImage(mProgressBarDrawable2, ScaleType.CENTER_CROP);
    assertEquals(mPlaceholderDrawable2, builder.getPlaceholderImage());
    assertEquals(ScaleType.CENTER, builder.getPlaceholderImageScaleType());
    assertEquals(mRetryDrawable2, builder.getRetryImage());
    assertEquals(ScaleType.FIT_CENTER, builder.getRetryImageScaleType());
    assertEquals(mFailureDrawable2, builder.getFailureImage());
    assertEquals(ScaleType.FIT_END, builder.getFailureImageScaleType());
    assertEquals(mProgressBarDrawable2, builder.getProgressBarImage());
    assertEquals(ScaleType.CENTER_CROP, builder.getProgressBarImageScaleType());

    // test image setters without modifying scale type (previous scaletype expected)
    builder.setPlaceholderImage(mPlaceholderDrawable1);
    builder.setRetryImage(mRetryDrawable1);
    builder.setFailureImage(mFailureDrawable1);
    builder.setProgressBarImage(mProgressBarDrawable1);
    assertEquals(mPlaceholderDrawable1, builder.getPlaceholderImage());
    assertEquals(ScaleType.CENTER, builder.getPlaceholderImageScaleType());
    assertEquals(mRetryDrawable1, builder.getRetryImage());
    assertEquals(ScaleType.FIT_CENTER, builder.getRetryImageScaleType());
    assertEquals(mFailureDrawable1, builder.getFailureImage());
    assertEquals(ScaleType.FIT_END, builder.getFailureImageScaleType());
    assertEquals(mProgressBarDrawable1, builder.getProgressBarImage());
    assertEquals(ScaleType.CENTER_CROP, builder.getProgressBarImageScaleType());

    // test actual image matrix
    builder.setActualImageMatrix(mActualImageMatrix);
    assertSame(mActualImageMatrix, builder.getActualImageMatrix());
    assertSame(null, builder.getActualImageScaleType());

    // test actual image scale type
    builder.setActualImageScaleType(ScaleType.FIT_START);
    assertEquals(ScaleType.FIT_START, builder.getActualImageScaleType());

    // test actual image focus point
    builder.setActualImageFocusPoint(mFocusPoint);
    AndroidGraphicsTestUtils.assertEquals(mFocusPoint, builder.getActualImageFocusPoint(), 0f);
    builder.setActualImageScaleType(ScaleType.FOCUS_CROP);
    assertSame(ScaleType.FOCUS_CROP, builder.getActualImageScaleType());
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
    // test clearing backgrounds & overlays
    builder.setBackground(null);
    assertNull(builder.getBackgrounds());
    builder.setOverlay(null);
    assertNull(builder.getOverlays());
    builder.setPressedStateOverlay(null);
    assertNull(builder.getPressedStateOverlay());

    // test rounding params
    builder.setRoundingParams(mRoundingParams);
    assertEquals(mRoundingParams, builder.getRoundingParams());

    // test reset
    builder.reset();
    testInitialState(builder);
  }
}
