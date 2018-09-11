/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.generic;

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import com.facebook.drawee.drawable.AndroidGraphicsTestUtils;
import com.facebook.drawee.drawable.DrawableTestUtils;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.Rounded;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.drawee.drawable.RoundedCornersDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GenericDraweeHierarchyTest {

  private GenericDraweeHierarchyBuilder mBuilder;

  private Drawable mBackground;
  private Drawable mOverlay1;
  private Drawable mOverlay2;
  private BitmapDrawable mPlaceholderImage;
  private BitmapDrawable mFailureImage;
  private BitmapDrawable mRetryImage;
  private BitmapDrawable mProgressBarImage;
  private BitmapDrawable mActualImage1;
  private BitmapDrawable mActualImage2;
  private ColorDrawable mWrappedLeaf;
  private ForwardingDrawable mWrappedImage;
  private PointF mFocusPoint;

  @Before
  public void setUp() {
    mBuilder = new GenericDraweeHierarchyBuilder(null);

    mBackground = DrawableTestUtils.mockDrawable();
    mOverlay1 = DrawableTestUtils.mockDrawable();
    mOverlay2 = DrawableTestUtils.mockDrawable();
    mPlaceholderImage = DrawableTestUtils.mockBitmapDrawable();
    mFailureImage = DrawableTestUtils.mockBitmapDrawable();
    mRetryImage = DrawableTestUtils.mockBitmapDrawable();
    mProgressBarImage = DrawableTestUtils.mockBitmapDrawable();
    mActualImage1 = DrawableTestUtils.mockBitmapDrawable();
    mActualImage2 = DrawableTestUtils.mockBitmapDrawable();
    mWrappedLeaf = new ColorDrawable(Color.BLUE);
    mWrappedImage = new ForwardingDrawable(new ForwardingDrawable(mWrappedLeaf));
    mFocusPoint = new PointF(0.1f, 0.4f);
  }

  @Test
  public void testHierarchy_WithScaleType() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, ScaleType.CENTER)
        .setRetryImage(mRetryImage, ScaleType.FIT_CENTER)
        .setFailureImage(mFailureImage, ScaleType.CENTER_INSIDE)
        .setProgressBarImage(mProgressBarImage, ScaleType.CENTER)
        .setActualImageScaleType(ScaleType.FOCUS_CROP)
        .setActualImageFocusPoint(mFocusPoint)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(7, fadeDrawable.getNumberOfLayers());
    assertNull(fadeDrawable.getDrawable(0));
    assertScaleTypeAndDrawable(mPlaceholderImage, ScaleType.CENTER, fadeDrawable.getDrawable(1));
    assertActualImageScaleType(ScaleType.FOCUS_CROP, mFocusPoint, fadeDrawable.getDrawable(2));
    assertScaleTypeAndDrawable(mProgressBarImage, ScaleType.CENTER, fadeDrawable.getDrawable(3));
    assertScaleTypeAndDrawable(mRetryImage, ScaleType.FIT_CENTER, fadeDrawable.getDrawable(4));
    assertScaleTypeAndDrawable(mFailureImage, ScaleType.CENTER_INSIDE, fadeDrawable.getDrawable(5));
    assertNull(fadeDrawable.getDrawable(6));
    verifyCallback(rootDrawable, mPlaceholderImage);
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
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(7, fadeDrawable.getNumberOfLayers());
    assertNull(fadeDrawable.getDrawable(0));
    assertSame(mPlaceholderImage, fadeDrawable.getDrawable(1));
    assertSame(ForwardingDrawable.class, fadeDrawable.getDrawable(2).getClass());
    assertSame(mProgressBarImage, fadeDrawable.getDrawable(3));
    assertSame(mRetryImage, fadeDrawable.getDrawable(4));
    assertSame(mFailureImage, fadeDrawable.getDrawable(5));
    assertNull(fadeDrawable.getDrawable(6));
    verifyCallback(rootDrawable, mPlaceholderImage);
  }

  @Test
  public void testHierarchy_NoBranches() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(7, fadeDrawable.getNumberOfLayers());
    assertNull(fadeDrawable.getDrawable(0));
    assertNull(fadeDrawable.getDrawable(1));
    assertActualImageScaleType(ScaleType.CENTER_CROP, null, fadeDrawable.getDrawable(2));
    assertNull(fadeDrawable.getDrawable(3));
    assertNull(fadeDrawable.getDrawable(4));
    assertNull(fadeDrawable.getDrawable(5));
    assertNull(fadeDrawable.getDrawable(6));
    verifyCallback(rootDrawable, fadeDrawable);
  }

  @Test
  public void testHierarchy_WithPlaceholderImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, ScaleType.CENTER)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertScaleTypeAndDrawable(mPlaceholderImage, ScaleType.CENTER, fadeDrawable.getDrawable(1));
    verifyCallback(rootDrawable, mPlaceholderImage);
  }

  @Test
  public void testHierarchy_WithFailureImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setFailureImage(mFailureImage, ScaleType.CENTER)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertScaleTypeAndDrawable(mFailureImage, ScaleType.CENTER, fadeDrawable.getDrawable(5));
    verifyCallback(rootDrawable, mFailureImage);
  }

  @Test
  public void testHierarchy_WithRetryImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setRetryImage(mRetryImage, ScaleType.CENTER)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertScaleTypeAndDrawable(mRetryImage, ScaleType.CENTER, fadeDrawable.getDrawable(4));
    verifyCallback(rootDrawable, mRetryImage);
  }

  @Test
  public void testHierarchy_WithProgressBarImage() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setProgressBarImage(mProgressBarImage, ScaleType.CENTER)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertScaleTypeAndDrawable(mProgressBarImage, ScaleType.CENTER, fadeDrawable.getDrawable(3));
    verifyCallback(rootDrawable, mProgressBarImage);
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
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(7, fadeDrawable.getNumberOfLayers());
    assertNull(fadeDrawable.getDrawable(0));
    assertScaleTypeAndDrawable(mPlaceholderImage, ScaleType.CENTER, fadeDrawable.getDrawable(1));
    assertActualImageScaleType(ScaleType.CENTER_CROP, null, fadeDrawable.getDrawable(2));
    assertScaleTypeAndDrawable(mProgressBarImage, ScaleType.CENTER, fadeDrawable.getDrawable(3));
    assertScaleTypeAndDrawable(mRetryImage, ScaleType.FIT_CENTER, fadeDrawable.getDrawable(4));
    assertScaleTypeAndDrawable(mFailureImage, ScaleType.FIT_CENTER, fadeDrawable.getDrawable(5));
    assertNull(fadeDrawable.getDrawable(6));
    verifyCallback(rootDrawable, mPlaceholderImage);
  }

  @Test
  public void testHierarchy_WithBackground() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setBackground(mBackground)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(7, fadeDrawable.getNumberOfLayers());
    assertSame(mBackground, fadeDrawable.getDrawable(0));
    verifyCallback(rootDrawable, mBackground);
  }

  @Test
  public void testHierarchy_WithOverlays() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setOverlays(Arrays.asList(mOverlay1, mOverlay2))
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(8, fadeDrawable.getNumberOfLayers());
    assertSame(mOverlay1, fadeDrawable.getDrawable(6));
    assertSame(mOverlay2, fadeDrawable.getDrawable(7));
    verifyCallback(rootDrawable, mOverlay1);
    verifyCallback(rootDrawable, mOverlay2);
  }

  @Test
  public void testHierarchy_WithSingleOverlay() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setOverlay(mOverlay1)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(7, fadeDrawable.getNumberOfLayers());
    assertSame(mOverlay1, fadeDrawable.getDrawable(6));
    verifyCallback(rootDrawable, mOverlay1);
  }

  @Test
  public void testHierarchy_WithBackgroundAndSingleOverlay() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setBackground(mBackground)
        .setOverlay(mOverlay2)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(7, fadeDrawable.getNumberOfLayers());
    assertSame(mBackground, fadeDrawable.getDrawable(0));
    assertSame(mOverlay2, fadeDrawable.getDrawable(6));
    verifyCallback(rootDrawable, mBackground);
    verifyCallback(rootDrawable, mOverlay2);
  }

  @Test
  public void testHierarchy_WithBackgroundAndMultipleOverlays() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, ScaleType.CENTER)
        .setRetryImage(mRetryImage, ScaleType.FIT_CENTER)
        .setFailureImage(mFailureImage, ScaleType.FIT_CENTER)
        .setProgressBarImage(mProgressBarImage, ScaleType.CENTER)
        .setActualImageScaleType(ScaleType.CENTER_CROP)
        .setBackground(mBackground)
        .setOverlays(Arrays.asList(mOverlay1, mOverlay2))
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(8, fadeDrawable.getNumberOfLayers());
    assertSame(mBackground, fadeDrawable.getDrawable(0));
    assertScaleTypeAndDrawable(mPlaceholderImage, ScaleType.CENTER, fadeDrawable.getDrawable(1));
    assertActualImageScaleType(ScaleType.CENTER_CROP, null, fadeDrawable.getDrawable(2));
    assertScaleTypeAndDrawable(mProgressBarImage, ScaleType.CENTER, fadeDrawable.getDrawable(3));
    assertScaleTypeAndDrawable(mRetryImage, ScaleType.FIT_CENTER, fadeDrawable.getDrawable(4));
    assertScaleTypeAndDrawable(mFailureImage, ScaleType.FIT_CENTER, fadeDrawable.getDrawable(5));
    assertSame(mOverlay1, fadeDrawable.getDrawable(6));
    assertSame(mOverlay2, fadeDrawable.getDrawable(7));
    verifyCallback(rootDrawable, mBackground);
    verifyCallback(rootDrawable, mPlaceholderImage);
    verifyCallback(rootDrawable, mOverlay2);
  }

  @Test
  public void testHierarchy_WithPressedStateOverlay() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setOverlay(mOverlay2)
        .setPressedStateOverlay(mOverlay1)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertEquals(8, fadeDrawable.getNumberOfLayers());
    assertSame(mOverlay2, fadeDrawable.getDrawable(6));
    StateListDrawable stateListDrawable = (StateListDrawable) fadeDrawable.getDrawable(7);
    assertNotNull(stateListDrawable);
  }

  @Test
  public void testHierarchy_WithRoundedOverlayColor() throws Exception {
    RoundingParams roundingParams =
        RoundingParams.fromCornersRadius(10).setOverlayColor(0xFFFFFFFF);
    GenericDraweeHierarchy dh = mBuilder
        .setRoundingParams(roundingParams)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    RoundedCornersDrawable roundedDrawable = (RoundedCornersDrawable) rootDrawable.getCurrent();
    assertRoundingParams(roundingParams, roundedDrawable);
    assertEquals(roundingParams.getOverlayColor(), roundedDrawable.getOverlayColor());
    FadeDrawable fadeDrawable = (FadeDrawable) roundedDrawable.getCurrent();
    assertNotNull(fadeDrawable);
    verifyCallback(rootDrawable, fadeDrawable);
  }

  @Test
  public void testHierarchy_WithRoundedLeafs() throws Exception {
    RoundingParams roundingParams = RoundingParams.asCircle();
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mWrappedImage, ScaleType.CENTER)
        .setFailureImage(mFailureImage, ScaleType.CENTER)
        .setRetryImage(mRetryImage, null)
        .setRoundingParams(roundingParams)
        .build();
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    assertNotNull(fadeDrawable);
    assertScaleTypeAndDrawable(mWrappedImage, ScaleType.CENTER, fadeDrawable.getDrawable(1));
    Rounded roundedPlaceholder = (Rounded) mWrappedImage.getCurrent().getCurrent();
    assertRoundingParams(roundingParams, roundedPlaceholder);
    Rounded roundedFailureImage = (Rounded) fadeDrawable.getDrawable(5).getCurrent();
    assertRoundingParams(roundingParams, roundedFailureImage);
    Rounded roundedRetryImage = (Rounded) fadeDrawable.getDrawable(4);
    assertRoundingParams(roundingParams, roundedRetryImage);
    verifyCallback(rootDrawable, mWrappedImage.getCurrent().getCurrent());
    verifyCallback(rootDrawable, (Drawable) roundedFailureImage);
    verifyCallback(rootDrawable, (Drawable) roundedRetryImage);
  }

  @Test
  public void testControlling_WithPlaceholderOnly() throws Exception {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setActualImageScaleType(null)
        .setFadeDuration(250)
        .build();

    // image indexes in DH tree
    final int placeholderImageIndex = 1;
    final int actualImageIndex = 2;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(placeholderImageIndex));
    assertEquals(
        ForwardingDrawable.class,
        fadeDrawable.getDrawable(actualImageIndex).getClass());

    ForwardingDrawable actualImageSettableDrawable =
        (ForwardingDrawable) fadeDrawable.getDrawable(actualImageIndex);

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
        .setBackground(mBackground)
        .setOverlays(Arrays.asList(mOverlay1, mOverlay2))
        .setPlaceholderImage(mPlaceholderImage, null)
        .setRetryImage(mRetryImage, null)
        .setFailureImage(mFailureImage, null)
        .setProgressBarImage(mProgressBarImage, null)
        .setActualImageScaleType(null)
        .setFadeDuration(250)
        .build();

    // image indexes in DH tree
    final int backgroundIndex = 0;
    final int placeholderImageIndex = 1;
    final int actualImageIndex = 2;
    final int progressBarImageIndex = 3;
    final int retryImageIndex = 4;
    final int failureImageIndex = 5;
    final int overlaysIndex = 6;
    int numOverlays = 2;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();

    assertEquals(mPlaceholderImage, fadeDrawable.getDrawable(placeholderImageIndex));
    assertEquals(mProgressBarImage, fadeDrawable.getDrawable(progressBarImageIndex));
    assertEquals(mRetryImage, fadeDrawable.getDrawable(retryImageIndex));
    assertEquals(mFailureImage, fadeDrawable.getDrawable(failureImageIndex));
    assertEquals(
        ForwardingDrawable.class,
        fadeDrawable.getDrawable(actualImageIndex).getClass());

    ForwardingDrawable finalImageSettableDrawable =
        (ForwardingDrawable) fadeDrawable.getDrawable(actualImageIndex);

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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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
    assertEquals(true, fadeDrawable.isLayerOn(backgroundIndex));
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

    // actual image index in DH tree
    final int imageIndex = 2;

    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();
    ForwardingDrawable settableDrawable = (ForwardingDrawable) fadeDrawable.getDrawable(imageIndex);

    // set temporary image
    dh.setImage(mActualImage1, 0.5f, true);
    assertNotSame(mActualImage1, settableDrawable.getCurrent());
    assertEquals(RoundedBitmapDrawable.class, settableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(imageIndex));
    assertEquals(FadeDrawable.TRANSITION_NONE, fadeDrawable.getTransitionState());
    verifyCallback(dh.getTopLevelDrawable(), settableDrawable.getCurrent());

    // set final image
    dh.setImage(mActualImage2, 1f, false);
    assertNotSame(mActualImage2, settableDrawable.getCurrent());
    assertEquals(RoundedBitmapDrawable.class, settableDrawable.getCurrent().getClass());
    assertEquals(true, fadeDrawable.isLayerOn(imageIndex));
    assertEquals(FadeDrawable.TRANSITION_STARTING, fadeDrawable.getTransitionState());
    assertEquals(250, fadeDrawable.getTransitionDuration());
    verifyCallback(dh.getTopLevelDrawable(), settableDrawable.getCurrent());
  }

  @Test
  public void testControlling_WithControllerOverlay() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, null)
        .setActualImageScaleType(null)
        .setFadeDuration(250)
        .build();

    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    // set controller overlay
    Drawable controllerOverlay = DrawableTestUtils.mockDrawable();
    dh.setControllerOverlay(controllerOverlay);
    assertSame(controllerOverlay, rootDrawable.mControllerOverlay);

    // clear controller overlay
    dh.setControllerOverlay(null);
    assertNull(rootDrawable.mControllerOverlay);
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
  public void testSetPlaceholderImage() throws Exception {
    final GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage, ScaleType.FIT_XY)
        .build();
    testSetDrawable(dh, 1, new SetDrawableCallback() {
      @Override
      public void setDrawable(Drawable drawable) {
        dh.setPlaceholderImage(drawable);
      }
      @Override
      public void setDrawable(Drawable drawable, ScaleType scaleType) {
        dh.setPlaceholderImage(drawable, scaleType);
      }
    });
  }

  @Test
  public void testSetFailureImage() throws Exception {
    final GenericDraweeHierarchy dh = mBuilder
        .setFailureImage(mFailureImage, null)
        .build();
    testSetDrawable(dh, 5, new SetDrawableCallback() {
      @Override
      public void setDrawable(Drawable drawable) {
        dh.setFailureImage(drawable);
      }
      @Override
      public void setDrawable(Drawable drawable, ScaleType scaleType) {
        dh.setFailureImage(drawable, scaleType);
      }
    });
  }

  @Test
  public void testSetRetryImage() throws Exception {
    final GenericDraweeHierarchy dh = mBuilder
        .setRetryImage(mRetryImage, null)
        .build();
    testSetDrawable(dh, 4, new SetDrawableCallback() {
      @Override
      public void setDrawable(Drawable drawable) {
        dh.setRetryImage(drawable);
      }
      @Override
      public void setDrawable(Drawable drawable, ScaleType scaleType) {
        dh.setRetryImage(drawable, scaleType);
      }
    });
  }

  @Test
  public void testSetProgressBarImage() throws Exception {
    final GenericDraweeHierarchy dh = mBuilder
        .setProgressBarImage(mProgressBarImage, null)
        .build();
    testSetDrawable(dh, 3, new SetDrawableCallback() {
      @Override
      public void setDrawable(Drawable drawable) {
        dh.setProgressBarImage(drawable);
      }
      @Override
      public void setDrawable(Drawable drawable, ScaleType scaleType) {
        dh.setProgressBarImage(drawable, scaleType);
      }
    });
  }

  private interface SetDrawableCallback {
    void setDrawable(Drawable drawable);
    void setDrawable(Drawable drawable, ScaleType scaleType);
  }

  private void testSetDrawable(GenericDraweeHierarchy dh, int index, SetDrawableCallback callback) {
    FadeDrawable fadeDrawable = (FadeDrawable) dh.getTopLevelDrawable().getCurrent();
    // null
    callback.setDrawable(null);
    assertNull(fadeDrawable.getDrawable(index));
    // null -> null
    callback.setDrawable(null);
    assertNull(fadeDrawable.getDrawable(index));
    // null -> drawable
    Drawable drawable1 = DrawableTestUtils.mockDrawable();
    callback.setDrawable(drawable1);
    assertSame(drawable1, fadeDrawable.getDrawable(index));
    // drawable -> drawable
    Drawable drawable2 = DrawableTestUtils.mockDrawable();
    callback.setDrawable(drawable2);
    assertSame(drawable2, fadeDrawable.getDrawable(index));
    // drawable -> null
    callback.setDrawable(null);
    assertNull(fadeDrawable.getDrawable(index));
    // null -> scaletype + drawable
    Drawable drawable3 = DrawableTestUtils.mockDrawable();
    callback.setDrawable(drawable3, ScaleType.FOCUS_CROP);
    assertScaleTypeAndDrawable(drawable3, ScaleType.FOCUS_CROP, fadeDrawable.getDrawable(index));
    // scaletype + drawable -> scaletype + drawable
    Drawable drawable4 = DrawableTestUtils.mockDrawable();
    callback.setDrawable(drawable4, ScaleType.CENTER);
    assertScaleTypeAndDrawable(drawable4, ScaleType.CENTER, fadeDrawable.getDrawable(index));
    // scaletype + drawable -> null
    callback.setDrawable(null);
    assertNull(fadeDrawable.getDrawable(index));
    // drawable -> scaletype + drawable
    callback.setDrawable(drawable1);
    Drawable drawable5 = DrawableTestUtils.mockDrawable();
    callback.setDrawable(drawable5, ScaleType.FIT_CENTER);
    assertScaleTypeAndDrawable(drawable5, ScaleType.FIT_CENTER, fadeDrawable.getDrawable(index));
    // scaletype + drawable -> drawable (kep the old scaletype)
    Drawable drawable6 = DrawableTestUtils.mockDrawable();
    callback.setDrawable(drawable6);
    assertScaleTypeAndDrawable(drawable6, ScaleType.FIT_CENTER, fadeDrawable.getDrawable(index));
  }

  @Test
  public void testSetActualImageFocusPoint() {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mPlaceholderImage)
        .setProgressBarImage(mProgressBarImage)
        .setActualImageScaleType(ScaleType.FOCUS_CROP)
        .build();

    // actual image index in DH tree
    final int imageIndex = 2;

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

    // actual image index in DH tree
    final int imageIndex = 2;

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
  public void testSetRoundingParams_NoneToNone() {
    testSetRoundingParams_ToNoneFrom(null);
  }

  @Test
  public void testSetRoundingParams_OverlayToNone() {
    testSetRoundingParams_ToNoneFrom(RoundingParams.asCircle().setOverlayColor(0x12345678));
  }

  @Test
  public void testSetRoundingParams_RoundedLeafsToNone() {
    testSetRoundingParams_ToNoneFrom(RoundingParams.asCircle());
  }

  private void testSetRoundingParams_ToNoneFrom(RoundingParams prev) {
    RoundingParams roundingParams = null;
    GenericDraweeHierarchy dh = testRoundingParams_createHierarchy(prev, roundingParams);
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    if (prev != null && prev.getRoundingMethod() == RoundingParams.RoundingMethod.BITMAP_ONLY) {
      // Rounded leafs remain rounded, but with reset params, so no leaf rounding actually occurs
      testRoundingParams_RoundedLeafs(rootDrawable, fadeDrawable, roundingParams);
    } else {
      testRoundingParams_NoRoundedLeafs(rootDrawable, fadeDrawable);
    }
  }

  @Test
  public void testSetRoundingParams_NoneToOverlay() {
    testSetRoundingParams_ToOverlayFrom(null);
  }

  @Test
  public void testSetRoundingParams_OverlayToOverlay() {
    testSetRoundingParams_ToOverlayFrom(RoundingParams.asCircle().setOverlayColor(0x12345678));
  }

  @Test
  public void testSetRoundingParams_RoundedLeafsToOverlay() {
    testSetRoundingParams_ToOverlayFrom(RoundingParams.fromCornersRadius(10));
  }

  private void testSetRoundingParams_ToOverlayFrom(RoundingParams prev) {
    RoundingParams roundingParams = RoundingParams.fromCornersRadius(7)
        .setOverlayColor(0xFFFFFFFF)
        .setBorder(0x12345678, 5)
        .setPadding(10);
    GenericDraweeHierarchy dh = testRoundingParams_createHierarchy(prev, roundingParams);
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    RoundedCornersDrawable roundedDrawable = (RoundedCornersDrawable) rootDrawable.getCurrent();
    assertRoundingParams(roundingParams, roundedDrawable);
    assertEquals(roundingParams.getOverlayColor(), roundedDrawable.getOverlayColor());
    FadeDrawable fadeDrawable = (FadeDrawable) roundedDrawable.getCurrent();
    if (prev != null && prev.getRoundingMethod() == RoundingParams.RoundingMethod.BITMAP_ONLY) {
      // Rounded leafs remain rounded, but with reset params, so no leaf rounding actually occurs
      testRoundingParams_RoundedLeafs(rootDrawable, fadeDrawable, null);
    } else {
      testRoundingParams_NoRoundedLeafs(rootDrawable, fadeDrawable);
    }
  }

  @Test
  public void testSetRoundingParams_NoneToRoundedLeafs() {
    testSetRoundingParams_ToRoundedLeafsFrom(null);
  }

  @Test
  public void testSetRoundingParams_OverlayToRoundedLeafs() {
    testSetRoundingParams_ToRoundedLeafsFrom(RoundingParams.asCircle().setOverlayColor(0x12345678));
  }

  @Test
  public void testSetRoundingParams_RoundedLeafsToRoundedLeafs() {
    testSetRoundingParams_ToRoundedLeafsFrom(RoundingParams.fromCornersRadius(10));
  }

  private void testSetRoundingParams_ToRoundedLeafsFrom(RoundingParams prev) {
    RoundingParams roundingParams = RoundingParams.asCircle().setBorder(0xAAAAAAAA, 4);
    GenericDraweeHierarchy dh = testRoundingParams_createHierarchy(prev, roundingParams);
    RootDrawable rootDrawable = (RootDrawable) dh.getTopLevelDrawable();
    FadeDrawable fadeDrawable = (FadeDrawable) rootDrawable.getCurrent();
    testRoundingParams_RoundedLeafs(rootDrawable, fadeDrawable, roundingParams);
  }

  private GenericDraweeHierarchy testRoundingParams_createHierarchy(
      RoundingParams prevRoundingParams,
      RoundingParams roundingParams) {
    GenericDraweeHierarchy dh = mBuilder
        .setPlaceholderImage(mWrappedImage, ScaleType.CENTER)
        .setFailureImage(mFailureImage, ScaleType.CENTER)
        .setRetryImage(mRetryImage, null)
        .setRoundingParams(prevRoundingParams)
        .build();
    dh.setRoundingParams(roundingParams);
    return dh;
  }

  private void testRoundingParams_NoRoundedLeafs(
      RootDrawable rootDrawable,
      FadeDrawable fadeDrawable) {
    assertNotNull(fadeDrawable);
    assertScaleTypeAndDrawable(mWrappedImage, ScaleType.CENTER, fadeDrawable.getDrawable(1));
    assertScaleTypeAndDrawable(mFailureImage, ScaleType.CENTER, fadeDrawable.getDrawable(5));
    assertSame(mRetryImage, fadeDrawable.getDrawable(4));
    verifyCallback(rootDrawable, mWrappedLeaf);
    verifyCallback(rootDrawable, mFailureImage);
    verifyCallback(rootDrawable, mRetryImage);
  }

  private void testRoundingParams_RoundedLeafs(
      RootDrawable rootDrawable,
      FadeDrawable fadeDrawable,
      RoundingParams roundingParams) {
    assertNotNull(fadeDrawable);
    assertScaleTypeAndDrawable(mWrappedImage, ScaleType.CENTER, fadeDrawable.getDrawable(1));
    Rounded roundedPlaceholder = (Rounded) mWrappedImage.getCurrent().getCurrent();
    assertRoundingParams(roundingParams, roundedPlaceholder);
    Rounded roundedFailureImage = (Rounded) fadeDrawable.getDrawable(5).getCurrent();
    assertRoundingParams(roundingParams, roundedFailureImage);
    Rounded roundedRetryImage = (Rounded) fadeDrawable.getDrawable(4);
    assertRoundingParams(roundingParams, roundedRetryImage);
    verifyCallback(rootDrawable, (Drawable) roundedPlaceholder);
    verifyCallback(rootDrawable, (Drawable) roundedFailureImage);
    verifyCallback(rootDrawable, (Drawable) roundedRetryImage);
  }

  private void assertScaleTypeAndDrawable(
      Drawable expectedChild,
      ScaleType expectedScaleType,
      Drawable actualBranch) {
    assertNotNull(actualBranch);
    ScaleTypeDrawable scaleTypeDrawable = (ScaleTypeDrawable) actualBranch;
    assertSame(expectedChild, scaleTypeDrawable.getCurrent());
    assertSame(expectedScaleType, scaleTypeDrawable.getScaleType());
  }

  private void assertActualImageScaleType(
      ScaleType expectedScaleType,
      PointF expectedFocusPoint,
      Drawable actualBranch) {
    assertNotNull(actualBranch);
    ScaleTypeDrawable scaleTypeDrawable = (ScaleTypeDrawable) actualBranch;
    assertSame(expectedScaleType, scaleTypeDrawable.getScaleType());
    assertSame(ForwardingDrawable.class, scaleTypeDrawable.getCurrent().getClass());
    AndroidGraphicsTestUtils.assertEquals(expectedFocusPoint, scaleTypeDrawable.getFocusPoint(), 0);
  }

  private void assertRoundingParams(
      RoundingParams roundingParams,
      Rounded roundedDrawable) {
    assertNotNull(roundedDrawable);
    if (roundingParams == null) {
      // default rounding params, no rounding specified
      roundingParams = new RoundingParams();
    }
    if (roundingParams.getCornersRadii() == null) {
      // create a zero radii array if null
      roundingParams.setCornersRadius(0);
    }
    assertEquals(roundingParams.getRoundAsCircle(), roundedDrawable.isCircle());
    assertArrayEquals(roundingParams.getCornersRadii(), roundedDrawable.getRadii(), 0f);
    assertEquals(roundingParams.getBorderColor(), roundedDrawable.getBorderColor());
    assertEquals(roundingParams.getBorderWidth(), roundedDrawable.getBorderWidth(), 0f);
    assertEquals(roundingParams.getPadding(), roundedDrawable.getPadding(), 0f);
  }

  private void verifyCallback(Drawable parent, Drawable child) {
    Drawable.Callback callback = mock(Drawable.Callback.class);
    parent.setCallback(callback);
    child.invalidateSelf();
    verify(callback).invalidateDrawable(any(Drawable.class));
  }
}
