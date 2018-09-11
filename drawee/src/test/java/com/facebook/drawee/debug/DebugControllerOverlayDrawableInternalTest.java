/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.debug;

import com.facebook.drawee.drawable.ScalingUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import robolectric3.shadows.ShadowMatrix;

/**
 * Test cases for {@link DebugControllerOverlayDrawable} that are not included in the gradle build
 * as they depend on a working `ShadowMatrix` implementation.
 */
@Config(shadows = {ShadowMatrix.class})
@RunWith(RobolectricTestRunner.class)
public class DebugControllerOverlayDrawableInternalTest {

  DebugControllerOverlayDrawableTestHelper helper;

  @Before
  public void setUp() {
    helper = new DebugControllerOverlayDrawableTestHelper();
  }

  @Test
  public void testOverlayWhenScaleTypeFitCenter() {
    helper.assertOverlayColorOk(100, 100, 100, 100, ScalingUtils.ScaleType.FIT_CENTER);
    helper.assertOverlayColorOk(100, 100, 1000, 100, ScalingUtils.ScaleType.FIT_CENTER);
    helper.assertOverlayColorOk(100, 100, 100, 1000, ScalingUtils.ScaleType.FIT_CENTER);

    helper.assertOverlayColorNotOk(100, 100, 1000, 1000, ScalingUtils.ScaleType.FIT_CENTER);
    helper.assertOverlayColorNotOk(100, 100, 10, 10, ScalingUtils.ScaleType.FIT_CENTER);
  }

  @Test
  public void testOverlayWhenScaleTypeFitXY() {
    helper.assertOverlayColorOk(100, 100, 100, 100, ScalingUtils.ScaleType.FIT_XY);

    helper.assertOverlayColorNotOk(100, 100, 1000, 100, ScalingUtils.ScaleType.FIT_XY);
    helper.assertOverlayColorNotOk(100, 100, 100, 1000, ScalingUtils.ScaleType.FIT_XY);
    helper.assertOverlayColorNotOk(100, 100, 1000, 1000, ScalingUtils.ScaleType.FIT_XY);
    helper.assertOverlayColorNotOk(100, 100, 10, 10, ScalingUtils.ScaleType.FIT_XY);
  }

  @Test
  public void testOverlayWhenScaleTypeCenter() {
    helper.assertOverlayColorOk(100, 100, 100, 100, ScalingUtils.ScaleType.CENTER);
    helper.assertOverlayColorOk(100, 100, 1000, 100, ScalingUtils.ScaleType.CENTER);
    helper.assertOverlayColorOk(100, 100, 100, 1000, ScalingUtils.ScaleType.CENTER);
    helper.assertOverlayColorOk(100, 100, 1000, 1000, ScalingUtils.ScaleType.CENTER);

    helper.assertOverlayColorNotOk(100, 100, 10, 10, ScalingUtils.ScaleType.CENTER);
  }

  @Test
  public void testOverlayWhenScaleTypeCenterCrop() {
    helper.assertOverlayColorOk(100, 100, 100, 100, ScalingUtils.ScaleType.CENTER_CROP);

    helper.assertOverlayColorNotOk(100, 100, 1000, 100, ScalingUtils.ScaleType.CENTER_CROP);
    helper.assertOverlayColorNotOk(100, 100, 100, 1000, ScalingUtils.ScaleType.CENTER_CROP);
    helper.assertOverlayColorNotOk(100, 100, 1000, 1000, ScalingUtils.ScaleType.CENTER_CROP);
    helper.assertOverlayColorNotOk(100, 100, 10, 10, ScalingUtils.ScaleType.CENTER_CROP);
  }
}
