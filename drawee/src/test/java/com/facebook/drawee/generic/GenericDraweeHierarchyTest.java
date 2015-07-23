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

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import com.facebook.drawee.drawable.AndroidGraphicsTestUtils;
import com.facebook.drawee.drawable.DrawableTestUtils;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.drawee.drawable.MatrixDrawable;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.drawee.drawable.RoundedCornersDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.SettableDrawable;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class GenericDraweeHierarchyTest {

  private final BitmapDrawable mPlaceholderImage = mock(BitmapDrawable.class);
  private final BitmapDrawable mFailureImage = mock(BitmapDrawable.class);
  private final BitmapDrawable mRetryImage = mock(BitmapDrawable.class);
  private final BitmapDrawable mProgressBarImage = mock(BitmapDrawable.class);
  private final BitmapDrawable mActualImage1 = mock(BitmapDrawable.class);
  private final BitmapDrawable mActualImage2 = mock(BitmapDrawable.class);
  private final Matrix mActualImageMatrix = mock(Matrix.class);
  private final PointF mFocusPoint = new PointF(0.1f, 0.4f);
  private final Drawable mBackground1 = mock(BitmapDrawable.class);
  private final Drawable mBackground2 = mock(BitmapDrawable.class);
  private final Drawable mOverlay1 = mock(BitmapDrawable.class);
  private final Drawable mOverlay2 = mock(BitmapDrawable.class);

  private GenericDraweeHierarchyBuilder mBuilder;

  @Before
  public void setUp() {
    when(mPlaceholderImage.getBounds()).thenReturn(new Rect());
    when(mPlaceholderImage.getPaint()).thenReturn(new Paint());
    when(mFailureImage.getBounds()).thenReturn(new Rect());
    when(mFailureImage.getPaint()).thenReturn(new Paint());
    when(mRetryImage.getBounds()).thenReturn(new Rect());
    when(mRetryImage.getPaint()).thenReturn(new Paint());
    when(mProgressBarImage.getBounds()).thenReturn(new Rect());
    when(mProgressBarImage.getPaint()).thenReturn(new Paint());
    when(mActualImage1.getBounds()).thenReturn(new Rect());
    when(mActualImage1.getPaint()).thenReturn(new Paint());
    when(mActualImage2.getBounds()).thenReturn(new Rect());
    when(mActualImage2.getPaint()).thenReturn(new Paint());

    mBuilder = new GenericDraweeHierarchyBuilder(null);
  }

  @Test
  public void testHierarchy_WithScaleType() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, ScaleType.CENTER)
        .setRetryImage(mRetryImage, ScaleType.FIT_CENTER)
        .setFailureImage(mFailureImage, ScaleType.CENTER_INSIDE)
        .setProgressBarImage(mProgressBarImage, ScaleType.CENTER_CROP)
        .setActualImageScaleType(ScaleType.FOCUS_CROP)
        .setActualImageFocusPoint(mFocusPoint)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(0).getClass());
    ScaleTypeDrawable placeholderBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(0);
    assertEquals(ScaleType.CENTER, placeholderBranch.getScaleType());
    assertEquals(mPlaceholderImage, placeholderBranch.getCurrent());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    ScaleTypeDrawable actualImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(1);
    assertEquals(ScaleType.FOCUS_CROP, actualImageBranch.getScaleType());
    assertEquals(SettableDrawable.class, actualImageBranch.getCurrent().getClass());
    AndroidGraphicsTestUtils.assertEquals(mFocusPoint, actualImageBranch.getFocusPoint(), 0f);

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    ScaleTypeDrawable progressBarImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(2);
    assertEquals(ScaleType.CENTER_CROP, progressBarImageBranch.getScaleType());
    assertEquals(mProgressBarImage, progressBarImageBranch.getCurrent());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(3).getClass());
    ScaleTypeDrawable retryImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(3);
    assertEquals(ScaleType.FIT_CENTER, retryImageBranch.getScaleType());
    assertEquals(mRetryImage, retryImageBranch.getCurrent());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(4).getClass());
    ScaleTypeDrawable failureImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(4);
    assertEquals(ScaleType.CENTER_INSIDE, failureImageBranch.getScaleType());
    assertEquals(mFailureImage, failureImageBranch.getCurrent());
  }

  @Test
  public void testHierarchy_WithMatrix() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setRetryImage(mRetryImage, null)
        .setFailureImage(mFailureImage, null)
        .setProgressBarImage(mProgressBarImage, null)
        .setActualImageMatrix(mActualImageMatrix)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(0));

    assertEquals(MatrixDrawable.class, fadeDrawable.getDrawable(1).getClass());
    MatrixDrawable actualImageBranch = (MatrixDrawable) fadeDrawable.getDrawable(1);
    assertEquals(mActualImageMatrix, actualImageBranch.getMatrix());
    assertEquals(SettableDrawable.class, actualImageBranch.getCurrent().getClass());

    assertEquals(mProgressBarImage, fadeDrawable.getDrawable(2));

    assertEquals(mRetryImage, fadeDrawable.getDrawable(3));

    assertEquals(mFailureImage, fadeDrawable.getDrawable(4));
  }

  @Test
  public void testHierarchy_NoScaleTypeNorMatrix() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setRetryImage(mRetryImage, null)
        .setFailureImage(mFailureImage, null)
        .setProgressBarImage(mProgressBarImage, null)
        .setActualImageScaleType(null)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(0));
    assertEquals(SettableDrawable.class, fadeDrawable.getDrawable(1).getClass());
    assertEquals(mProgressBarImage, fadeDrawable.getDrawable(2));
    assertEquals(mRetryImage, fadeDrawable.getDrawable(3));
    assertEquals(mFailureImage, fadeDrawable.getDrawable(4));
  }

  @Test
  public void testHierarchy_NoPlaceholderImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    // transparent color drawable will be used as placeholder
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(0).getClass());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    ScaleTypeDrawable actualImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(1);
    assertEquals(ScaleType.CENTER_CROP, actualImageBranch.getScaleType());
    assertEquals(SettableDrawable.class, actualImageBranch.getCurrent().getClass());
  }

  @Test
  public void testHierarchy_WithPlaceholderImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, ScaleType.CENTER)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(0).getClass());
    ScaleTypeDrawable placeholderBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(0);
    assertEquals(ScaleType.CENTER, placeholderBranch.getScaleType());
    assertEquals(mPlaceholderImage, placeholderBranch.getCurrent());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    ScaleTypeDrawable actualImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(1);
    assertEquals(ScaleType.CENTER_CROP, actualImageBranch.getScaleType());
    assertEquals(SettableDrawable.class, actualImageBranch.getCurrent().getClass());
  }

  @Test
  public void testHierarchy_WithFailureImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setFailureImage(mFailureImage, ScaleType.CENTER)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    // transparent color drawable is used as placeholder when not specified otherwise
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(0).getClass());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    ScaleTypeDrawable actualImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(1);
    assertEquals(ScaleType.CENTER_CROP, actualImageBranch.getScaleType());
    assertEquals(SettableDrawable.class, actualImageBranch.getCurrent().getClass());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    ScaleTypeDrawable failureImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(2);
    assertEquals(ScaleType.CENTER, failureImageBranch.getScaleType());
    assertEquals(mFailureImage, failureImageBranch.getCurrent());
  }

  @Test
  public void testHierarchy_WithRetryImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setRetryImage(mRetryImage, ScaleType.CENTER)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    // transparent color drawable is used as placeholder when not specified otherwise
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(0).getClass());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    ScaleTypeDrawable actualImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(1);
    assertEquals(ScaleType.CENTER_CROP, actualImageBranch.getScaleType());
    assertEquals(SettableDrawable.class, actualImageBranch.getCurrent().getClass());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    ScaleTypeDrawable retryImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(2);
    assertEquals(ScaleType.CENTER, retryImageBranch.getScaleType());
    assertEquals(mRetryImage, retryImageBranch.getCurrent());
  }

  @Test
  public void testHierarchy_WithProgressBarImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setProgressBarImage(mProgressBarImage, ScaleType.CENTER)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    // transparent color drawable is used as placeholder when not specified otherwise
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(0).getClass());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    ScaleTypeDrawable actualImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(1);
    assertEquals(ScaleType.CENTER_CROP, actualImageBranch.getScaleType());
    assertEquals(SettableDrawable.class, actualImageBranch.getCurrent().getClass());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    ScaleTypeDrawable progressBarImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(2);
    assertEquals(ScaleType.CENTER, progressBarImageBranch.getScaleType());
    assertEquals(mProgressBarImage, progressBarImageBranch.getCurrent());
  }

  @Test
  public void testHierarchy_WithAllBranches() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, ScaleType.CENTER)
        .setRetryImage(mRetryImage, ScaleType.FIT_CENTER)
        .setFailureImage(mFailureImage, ScaleType.FIT_CENTER)
        .setProgressBarImage(mProgressBarImage, ScaleType.CENTER)
        .setActualImageScaleType(ScaleType.CENTER_CROP)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(6, fadeDrawable.getNumberOfLayers());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(0).getClass());
    ScaleTypeDrawable placeholderBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(0);
    assertEquals(ScaleType.CENTER, placeholderBranch.getScaleType());
    assertEquals(mPlaceholderImage, placeholderBranch.getCurrent());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    ScaleTypeDrawable imageBranch1 = (ScaleTypeDrawable) fadeDrawable.getDrawable(1);
    assertEquals(ScaleType.CENTER_CROP, imageBranch1.getScaleType());
    assertEquals(SettableDrawable.class, imageBranch1.getCurrent().getClass());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    ScaleTypeDrawable progressBarImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(2);
    assertEquals(ScaleType.CENTER, progressBarImageBranch.getScaleType());
    assertEquals(mProgressBarImage, progressBarImageBranch.getCurrent());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(3).getClass());
    ScaleTypeDrawable retryImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(3);
    assertEquals(ScaleType.FIT_CENTER, retryImageBranch.getScaleType());
    assertEquals(mRetryImage, retryImageBranch.getCurrent());

    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(4).getClass());
    ScaleTypeDrawable failureImageBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(4);
    assertEquals(ScaleType.FIT_CENTER, failureImageBranch.getScaleType());
    assertEquals(mFailureImage, failureImageBranch.getCurrent());

    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(5).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithBackgrounds() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setBackgrounds(Arrays.asList(mBackground1, mBackground2))
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(5, fadeDrawable.getNumberOfLayers());
    assertEquals(mBackground1, fadeDrawable.getDrawable(0));
    assertEquals(mBackground2, fadeDrawable.getDrawable(1));
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(2));
    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(3).getClass());
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(4).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithSingleBackground() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setBackground(mBackground1)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(4, fadeDrawable.getNumberOfLayers());
    assertEquals(mBackground1, fadeDrawable.getDrawable(0));
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(1));
    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(3).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithOverlays() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setOverlays(Arrays.asList(mOverlay1, mOverlay2))
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(5, fadeDrawable.getNumberOfLayers());
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(0));
    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    assertEquals(mOverlay1, fadeDrawable.getDrawable(2));
    assertEquals(mOverlay2, fadeDrawable.getDrawable(3));
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(4).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithSingleOverlay() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setOverlay(mOverlay1)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(4, fadeDrawable.getNumberOfLayers());
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(0));
    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(1).getClass());
    assertEquals(mOverlay1, fadeDrawable.getDrawable(2));
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(3).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithBackgroundsAndOverlays() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setBackgrounds(Arrays.asList(mBackground1, mBackground2))
        .setOverlays(Arrays.asList(mOverlay1, mOverlay2))
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(7, fadeDrawable.getNumberOfLayers());
    assertEquals(mBackground1, fadeDrawable.getDrawable(0));
    assertEquals(mBackground2, fadeDrawable.getDrawable(1));
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(2));
    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(3).getClass());
    assertEquals(mOverlay1, fadeDrawable.getDrawable(4));
    assertEquals(mOverlay2, fadeDrawable.getDrawable(5));
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(6).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithSingleBackgroundAndOverlay() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setBackground(mBackground2)
        .setOverlay(mOverlay2)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(5, fadeDrawable.getNumberOfLayers());
    assertEquals(mBackground2, fadeDrawable.getDrawable(0));
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(1));
    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    assertEquals(mOverlay2, fadeDrawable.getDrawable(3));
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(4).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithPressedStateOverlaySetFirst() throws Exception {
    //Setting PressedStateOverlay before Overlays and Backgroud
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setPressedStateOverlay(mOverlay1)
        .setBackground(mBackground2)
        .setOverlay(mOverlay2)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(6, fadeDrawable.getNumberOfLayers());
    assertEquals(mBackground2, fadeDrawable.getDrawable(0));
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(1));
    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    assertEquals(mOverlay2, fadeDrawable.getDrawable(3));
    assertEquals(StateListDrawable.class, fadeDrawable.getDrawable(4).getClass());
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(5).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithPressedStateOverlaySetLast() throws Exception {
    //Setting PressedStateOverlay after Overlays and Backgroud
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setBackground(mBackground2)
        .setOverlay(mOverlay2)
        .setPressedStateOverlay(mOverlay1)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertAssignableFrom(FadeDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(6, fadeDrawable.getNumberOfLayers());
    assertEquals(mBackground2, fadeDrawable.getDrawable(0));
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(1));
    assertEquals(ScaleTypeDrawable.class, fadeDrawable.getDrawable(2).getClass());
    assertEquals(mOverlay2, fadeDrawable.getDrawable(3));
    assertEquals(StateListDrawable.class, fadeDrawable.getDrawable(4).getClass());
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(5).getClass()); // controller overlay
  }

  @Test
  public void testHierarchy_WithRoundedCornersDrawable() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setActualImageScaleType(null)
        .setRoundingParams(RoundingParams.fromCornersRadius(10).setOverlayColor(0xFFFFFFFF))
        .setFadeDuration(250)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertEquals(RoundedCornersDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
  }

  @Test
  public void testHierarchy_WithRoundedCornersDrawableAsCircle() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setActualImageScaleType(null)
        .setRoundingParams(RoundingParams.asCircle().setOverlayColor(0xFFFFFFFF))
        .setFadeDuration(100)
        .build();

    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicWidth());
    assertEquals(-1, dh.getTopLevelDrawable().getIntrinsicHeight());

    assertEquals(RoundedCornersDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());
  }

  @Test
  public void testControlling_WithPlaceholderOnly() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setActualImageScaleType(null)
        .setFadeDuration(250)
        .build();

    // image indexes in DH tree
    final int placeholderImageIndex = 0;
    final int actualImageIndex = 1;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(placeholderImageIndex));
    assertEquals(
        SettableDrawable.class,
        fadeDrawable.getDrawable(actualImageIndex).getClass());

    SettableDrawable actualImageSettableDrawable =
        (SettableDrawable) fadeDrawable.getDrawable(actualImageIndex);

    // initial state -> final image (non-immediate)
    // initial state
    assertEquals(ColorDrawable.class, actualImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set final image (non-immediate)
    dh.setImage(mActualImage1, 1f, false);
    assertEquals(mActualImage1, actualImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());

    // initial state -> final image (immediate)
    // reset hierarchy to initial state
    dh.reset();
    assertEquals(ColorDrawable.class, actualImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set final image (immediate)
    dh.setImage(mActualImage2, 1f, true);
    assertEquals(mActualImage2, actualImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());

    // initial state -> retry
    // reset hierarchy to initial state
    dh.reset();
    assertEquals(ColorDrawable.class, actualImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set retry
    dh.setRetry(new RuntimeException());
    assertEquals(ColorDrawable.class, actualImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());

    // initial state -> failure
    // reset hierarchy to initial state
    dh.reset();
    assertEquals(ColorDrawable.class, actualImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set failure
    dh.setFailure(new RuntimeException());
    assertEquals(ColorDrawable.class, actualImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());
  }

  @Test
  public void testControlling_WithAllLayers() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setBackgrounds(Arrays.asList(mBackground1, mBackground2))
        .setOverlays(Arrays.asList(mOverlay1, mOverlay2))
        .setPlaceholderImage(mPlaceholderImage, null)
        .setRetryImage(mRetryImage, null)
        .setFailureImage(mFailureImage, null)
        .setProgressBarImage(mProgressBarImage, null)
        .setActualImageScaleType(null)
        .setFadeDuration(250)
        .build();

    // image indexes in DH tree
    final int backgroundsIndex = 0;
    final int numBackgrounds = 2;
    final int placeholderImageIndex = numBackgrounds + 0;
    final int actualImageIndex = numBackgrounds + 1;
    final int progressBarImageIndex = numBackgrounds + 2;
    final int retryImageIndex = numBackgrounds + 3;
    final int failureImageIndex = numBackgrounds + 4;
    final int numBranches = 5;
    final int overlaysIndex = numBackgrounds + numBranches;
    final int numOverlays = 2;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(placeholderImageIndex));
    assertEquals(mProgressBarImage, fadeDrawable.getDrawable(progressBarImageIndex));
    assertEquals(mRetryImage, fadeDrawable.getDrawable(retryImageIndex));
    assertEquals(mFailureImage, fadeDrawable.getDrawable(failureImageIndex));
    assertEquals(
        SettableDrawable.class,
        fadeDrawable.getDrawable(actualImageIndex).getClass());

    SettableDrawable finalImageSettableDrawable =
        (SettableDrawable) fadeDrawable.getDrawable(actualImageIndex);

    // initial state -> final image (immediate)
    // initial state, show progress bar
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    dh.setProgress(0f, true);
    assertEquals(ColorDrawable.class, finalImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set final image (immediate)
    dh.setImage(mActualImage2, 1f, true);
    assertEquals(mActualImage2, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());

    // initial state -> final image (non-immediate)
    // reset hierarchy to initial state, show progress bar
    dh.reset();
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    dh.setProgress(0f, true);
    assertEquals(ColorDrawable.class, finalImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set final image (non-immediate)
    dh.setImage(mActualImage2, 1f, false);
    assertEquals(mActualImage2, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());

    // initial state -> temporary image (immediate) -> final image (non-immediate)
    // reset hierarchy to initial state, show progress bar
    dh.reset();
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    dh.setProgress(0f, true);
    assertEquals(ColorDrawable.class, finalImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set temporary image (immediate)
    dh.setImage(mActualImage1, 0.5f, true);
    assertEquals(mActualImage1, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set final image (non-immediate)
    dh.setImage(mActualImage2, 1f, false);
    assertEquals(mActualImage2, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());

    // initial state -> temporary image (non-immediate) -> final image (non-immediate)
    // reset hierarchy to initial state, show progress bar
    dh.reset();
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    dh.setProgress(0f, true);
    assertEquals(ColorDrawable.class, finalImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set temporary image (non-immediate)
    dh.setImage(mActualImage1, 0.5f, false);
    assertEquals(mActualImage1, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());
    // set final image (non-immediate)
    dh.setImage(mActualImage2, 1f, false);
    assertEquals(mActualImage2, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());

    // initial state -> temporary image (immediate) -> retry
    // reset hierarchy to initial state, show progress bar
    dh.reset();
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    dh.setProgress(0f, true);
    assertEquals(ColorDrawable.class, finalImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set temporary image (immediate)
    dh.setImage(mActualImage1, 0.5f, true);
    assertEquals(mActualImage1, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set retry
    dh.setRetry(new RuntimeException());
    assertEquals(mActualImage1, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());

    // initial state -> temporary image (immediate) -> failure
    // reset hierarchy to initial state, show progress bar
    dh.reset();
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    dh.setProgress(0f, true);
    assertEquals(ColorDrawable.class, finalImageSettableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set temporary image (immediate)
    dh.setImage(mActualImage1, 0.5f, true);
    assertEquals(mActualImage1, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    // set failure
    dh.setFailure(new RuntimeException());
    assertEquals(mActualImage1, finalImageSettableDrawable.getCurrent());
    assertEquals(false, fadeDrawable.isLayerOn(placeholderImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(actualImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(progressBarImageIndex));
    assertEquals(false, fadeDrawable.isLayerOn(retryImageIndex));
    assertEquals(true, fadeDrawable.isLayerOn(failureImageIndex));
    assertLayersOn(fadeDrawable, backgroundsIndex, numBackgrounds);
    assertLayersOn(fadeDrawable, overlaysIndex, numOverlays);
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());
  }

  @Test
  public void testControlling_WithCornerRadii() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setActualImageScaleType(null)
        .setRoundingParams(RoundingParams.fromCornersRadius(10))
        .setFadeDuration(250)
        .build();

    // image indexes in DH tree
    final int imageIndex = 1;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();
    SettableDrawable settableDrawable = (SettableDrawable) fadeDrawable.getDrawable(imageIndex);

    // set temporary image
    dh.setImage(mActualImage1, 0.5f, true);
    assertNotSame(mActualImage1, settableDrawable.getCurrent());
    assertEquals(RoundedBitmapDrawable.class, settableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(imageIndex));
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());

    // set final image
    dh.setImage(mActualImage2, 1f, false);
    assertNotSame(mActualImage2, settableDrawable.getCurrent());
    assertEquals(RoundedBitmapDrawable.class, settableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(imageIndex));
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());
  }

  @Test
  public void testControlling_WithControllerOverlay() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setActualImageScaleType(null)
        .setFadeDuration(250)
        .build();

    Drawable controllerOverlay = DrawableTestUtils.mockDrawable();

    // image indexes in DH tree
    final int placeholderImageIndex = 0;
    final int actualImageIndex = 1;
    final int controllerOverlayIndex = 2;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    // initial state
    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(placeholderImageIndex));
    assertEquals(SettableDrawable.class, fadeDrawable.getDrawable(actualImageIndex).getClass());
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(controllerOverlayIndex).getClass());

    // set controller overlay
    dh.setControllerOverlay(controllerOverlay);
    assertEquals(controllerOverlay, fadeDrawable.getDrawable(controllerOverlayIndex));

    // clear controller overlay
    dh.setControllerOverlay(null);
    assertEquals(ColorDrawable.class, fadeDrawable.getDrawable(controllerOverlayIndex).getClass());
  }

  private void assertLayersOn(FadeDrawable fadeDrawable, int firstLayerIndex, int numberOfLayers) {
    for (int i = 0; i < numberOfLayers; i++) {
      assertEquals(true, fadeDrawable.isLayerOn(firstLayerIndex + i));
    }
  }

  @Test
  public void testDrawVisibleDrawableOnly() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .build();
    Canvas mockCanvas = mock(Canvas.class);
    dh.getTopLevelDrawable().setVisible(false, true);
    dh.getTopLevelDrawable().draw(mockCanvas);
    verify(mPlaceholderImage, never()).draw(mockCanvas);
    dh.getTopLevelDrawable().setVisible(true, true);
    dh.getTopLevelDrawable().draw(mockCanvas);
    verify(mPlaceholderImage).draw(mockCanvas);
  }

  @Test
  @TargetApi(11)
  public void testSetPlaceholderImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, ScaleType.FIT_XY)
        .build();

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();
    ScaleTypeDrawable placeholderBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(0);
    assertEquals(mPlaceholderImage, placeholderBranch.getCurrent());

    dh.setPlaceholderImage(null);
    assertTrue(placeholderBranch.getCurrent() instanceof ColorDrawable);
    assertEquals(Color.TRANSPARENT, ((ColorDrawable) placeholderBranch.getCurrent()).getColor());

    Drawable newPlaceholder = mock(Drawable.class);
    dh.setPlaceholderImage(newPlaceholder);
    assertSame(placeholderBranch, fadeDrawable.getDrawable(0));
    assertSame(newPlaceholder, placeholderBranch.getCurrent());
    assertEquals(ScaleType.FIT_XY, placeholderBranch.getScaleType());
  }

  @Test
  public void testSetActualImageFocusPoint() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .setProgressBarImage(mProgressBarImage)
        .setActualImageScaleType(ScaleType.FOCUS_CROP)
        .build();

    // image indexes in DH tree
    final int imageIndex = 1;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();
    ScaleTypeDrawable scaleTypeDrawable = (ScaleTypeDrawable) fadeDrawable.getDrawable(imageIndex);
    assertNull(scaleTypeDrawable.getFocusPoint());

    PointF focus1 = new PointF(0.3f, 0.4f);
    dh.setActualImageFocusPoint(focus1);
    AndroidGraphicsTestUtils.assertEquals(focus1, scaleTypeDrawable.getFocusPoint(), 0f);

    PointF focus2 = new PointF(0.6f, 0.7f);
    dh.setActualImageFocusPoint(focus2);
    AndroidGraphicsTestUtils.assertEquals(focus2, scaleTypeDrawable.getFocusPoint(), 0f);
  }

  @Test
  public void testSetActualImageScaleType() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .build();

    // image indexes in DH tree
    final int imageIndex = 1;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();
    ScaleTypeDrawable scaleTypeDrawable = (ScaleTypeDrawable) fadeDrawable.getDrawable(imageIndex);

    ScaleType scaleType1 = ScaleType.FOCUS_CROP;
    dh.setActualImageScaleType(scaleType1);
    assertEquals(scaleType1, scaleTypeDrawable.getScaleType());

    ScaleType scaleType2 = ScaleType.CENTER;
    dh.setActualImageScaleType(scaleType2);
    assertEquals(scaleType2, scaleTypeDrawable.getScaleType());
  }

  @Test
  public void testSetRoundingParams_OverlayColor() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .setRoundingParams(RoundingParams.asCircle().setOverlayColor(0xC0123456))
        .build();

    assertEquals(RoundedCornersDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());

    assertTrue(dh.getRoundingParams().getRoundAsCircle());
    assertEquals(
        RoundingParams.RoundingMethod.OVERLAY_COLOR,
        dh.getRoundingParams().getRoundingMethod());
    assertEquals(0xC0123456, dh.getRoundingParams().getOverlayColor());

    dh.setRoundingParams(RoundingParams.fromCornersRadius(9).setOverlayColor(0xFFFFFFFF));

    assertFalse(dh.getRoundingParams().getRoundAsCircle());
    assertEquals(
        RoundingParams.RoundingMethod.OVERLAY_COLOR,
        dh.getRoundingParams().getRoundingMethod());
    float[] expectedRadii = new float[] {9, 9, 9, 9, 9, 9, 9, 9};
    assertArrayEquals(expectedRadii, dh.getRoundingParams().getCornersRadii(), 0);
    assertEquals(0xFFFFFFFF, dh.getRoundingParams().getOverlayColor());
  }

  @Test
  public void testSetRoundingParams_Border() {
    int borderColor = Color.CYAN;
    float borderWidth = 0.4f;

    RoundingParams roundingParams = RoundingParams
        .asCircle()
        .setOverlayColor(Color.GRAY)
        .setBorder(borderColor, borderWidth);

    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .setRoundingParams(roundingParams)
        .build();

    assertEquals(RoundedCornersDrawable.class, dh.getTopLevelDrawable().getCurrent().getClass());

    assertTrue(dh.getRoundingParams().getRoundAsCircle());
    assertEquals(borderColor, dh.getRoundingParams().getBorderColor());
    assertEquals(borderWidth, dh.getRoundingParams().getBorderWidth(), 0);
    assertEquals(Color.GRAY, dh.getRoundingParams().getOverlayColor());

    int borderColor2 = Color.RED;
    float borderWidth2 = 0.3f;
    roundingParams = RoundingParams
        .fromCornersRadius(9)
        .setOverlayColor(Color.RED)
        .setBorder(borderColor2, borderWidth2);

    dh.setRoundingParams(roundingParams);

    assertFalse(dh.getRoundingParams().getRoundAsCircle());

    float[] expectedRadii = new float[] {9, 9, 9, 9, 9, 9, 9, 9};
    assertArrayEquals(expectedRadii, dh.getRoundingParams().getCornersRadii(), 0);
    assertEquals(borderColor2, dh.getRoundingParams().getBorderColor());
    assertEquals(borderWidth2, dh.getRoundingParams().getBorderWidth(), 0);
    assertEquals(Color.RED, dh.getRoundingParams().getOverlayColor());
  }

  @Test
  public void testSetRoundingParams_BitmapOnly() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .setRoundingParams(RoundingParams.asCircle())
        .build();

    assertTrue(dh.getRoundingParams().getRoundAsCircle());
    assertEquals(
        RoundingParams.RoundingMethod.BITMAP_ONLY,
        dh.getRoundingParams().getRoundingMethod());

    dh.setRoundingParams(RoundingParams.fromCornersRadius(9));

    assertFalse(dh.getRoundingParams().getRoundAsCircle());
    assertEquals(
        RoundingParams.RoundingMethod.BITMAP_ONLY,
        dh.getRoundingParams().getRoundingMethod());
    float[] expectedRadii = new float[] {9, 9, 9, 9, 9, 9, 9, 9};
    assertArrayEquals(expectedRadii, dh.getRoundingParams().getCornersRadii(), 0);
  }

  @Test
  public void testSetRoundingParamsOverlay_PreviouslyBitmap() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .setRoundingParams(RoundingParams.asCircle())
        .build();

    assertTrue(dh.getTopLevelDrawable().getCurrent() instanceof FadeDrawable);
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();
    ScaleTypeDrawable placeholderBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(0);
    assertTrue(placeholderBranch.getCurrent() instanceof RoundedBitmapDrawable);

    dh.setRoundingParams(
        RoundingParams.asCircle().setOverlayColor(Color.BLUE));
    assertTrue(dh.getTopLevelDrawable().getCurrent() instanceof RoundedCornersDrawable);
  }

  @Test
  public void testSetRoundingParamsBitmap_PreviouslyOverlay() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .setRoundingParams(RoundingParams.asCircle().setOverlayColor(Color.BLACK))
        .build();

    assertTrue(dh.getTopLevelDrawable().getCurrent() instanceof RoundedCornersDrawable);
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent().getCurrent();
    ScaleTypeDrawable placeholderBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(0);
    assertFalse(placeholderBranch.getCurrent() instanceof RoundedBitmapDrawable);

    dh.setRoundingParams(RoundingParams.asCircle());
    assertTrue(dh.getTopLevelDrawable().getCurrent() instanceof FadeDrawable);
    placeholderBranch = (ScaleTypeDrawable) fadeDrawable.getDrawable(0);
    assertTrue(placeholderBranch.getCurrent() instanceof RoundedBitmapDrawable);
  }

  private <T, F> void assertAssignableFrom(Class<T> to, Class<F> from) {
    assertTrue(to + " is not assignable from " + from, to.isAssignableFrom(from));
  }
}
