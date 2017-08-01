/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.debug;

import static org.junit.Assert.assertEquals;

import com.facebook.drawee.drawable.ScalingUtils;

public class DebugControllerOverlayDrawableTestHelper {

  public DebugControllerOverlayDrawable mOverlayDrawable;

  public DebugControllerOverlayDrawableTestHelper() {
    mOverlayDrawable = new DebugControllerOverlayDrawable();
  }

  public void assertOverlayColorOk(
      int imageWidth,
      int imageHeight,
      int drawableWidth,
      int drawableHeight,
      ScalingUtils.ScaleType scaleType) {
    mOverlayDrawable.setBounds(0, 0, drawableWidth, drawableHeight);
    assertEquals(
        DebugControllerOverlayDrawable.OVERLAY_COLOR_IMAGE_OK,
        mOverlayDrawable.determineOverlayColor(
            imageWidth,
            imageHeight,
            scaleType));
  }

  public void assertOverlayColorAlmostOk(
      int imageWidth,
      int imageHeight,
      int drawableWidth,
      int drawableHeight,
      ScalingUtils.ScaleType scaleType) {
    mOverlayDrawable.setBounds(0, 0, drawableWidth, drawableHeight);
    assertEquals(
        DebugControllerOverlayDrawable.OVERLAY_COLOR_IMAGE_ALMOST_OK,
        mOverlayDrawable.determineOverlayColor(
            imageWidth,
            imageHeight,
            scaleType));
  }

  public void assertOverlayColorNotOk(
      int imageWidth,
      int imageHeight,
      int drawableWidth,
      int drawableHeight,
      ScalingUtils.ScaleType scaleType) {
    mOverlayDrawable.setBounds(0, 0, drawableWidth, drawableHeight);
    assertEquals(
        DebugControllerOverlayDrawable.OVERLAY_COLOR_IMAGE_NOT_OK,
        mOverlayDrawable.determineOverlayColor(
            imageWidth,
            imageHeight,
            scaleType));
  }
}
