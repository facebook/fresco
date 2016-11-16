/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.debug;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link DebugControllerOverlayDrawable}
 */
@RunWith(RobolectricTestRunner.class)
public class DebugControllerOverlayDrawableTest {

  public DebugControllerOverlayDrawable mOverlayDrawable;

  @Before
  public void setUp() {
    mOverlayDrawable = new DebugControllerOverlayDrawable();
  }

  @Test
  public void testOverlayColorOkWhenSameSize() {
    assertOverlayColorOk(100, 100, 100, 100);
    assertOverlayColorOk(160, 90, 160, 90);
    assertOverlayColorOk(10, 400, 10, 400);
  }

  @Test
  public void testOverlayColorOkWhenDifferentSize() {
    assertOverlayColorOk(101, 50, 100, 50);
    assertOverlayColorOk(99, 50, 100, 50);
    assertOverlayColorOk(100, 52, 100, 50);
    assertOverlayColorOk(100, 48, 100, 50);

    assertOverlayColorOk(101, 49, 100, 50);
    assertOverlayColorOk(98, 52, 100, 50);

    assertOverlayColorOk(109, 50, 100, 50);
    assertOverlayColorOk(91, 50, 100, 50);
    assertOverlayColorOk(100, 54, 100, 50);
    assertOverlayColorOk(100, 46, 100, 50);

    assertOverlayColorOk(109, 54, 100, 50);
    assertOverlayColorOk(109, 46, 100, 50);
    assertOverlayColorOk(91, 54, 100, 50);
    assertOverlayColorOk(91, 46, 100, 50);
  }

  @Test
  public void testOverlayColorAlmostOk() {
    assertOverlayColorAlmostOk(110, 50, 100, 50);
    assertOverlayColorAlmostOk(90, 50, 100, 50);
    assertOverlayColorAlmostOk(100, 45, 100, 50);
    assertOverlayColorAlmostOk(100, 55, 100, 50);

    assertOverlayColorAlmostOk(110, 55, 100, 50);
    assertOverlayColorAlmostOk(110, 45, 100, 50);
    assertOverlayColorAlmostOk(90, 55, 100, 50);
    assertOverlayColorAlmostOk(90, 45, 100, 50);

    assertOverlayColorAlmostOk(149, 50, 100, 50);
    assertOverlayColorAlmostOk(149, 74, 100, 50);
    assertOverlayColorAlmostOk(51, 74, 100, 50);
    assertOverlayColorAlmostOk(51, 26, 100, 50);
  }

  @Test
  public void testOverlayColorNotOk() {
    assertOverlayColorNotOk(150, 50, 100, 50);
    assertOverlayColorNotOk(100, 75, 100, 50);
    assertOverlayColorNotOk(100, 100, 100, 50);
    assertOverlayColorNotOk(50, 50, 100, 50);
    assertOverlayColorNotOk(100, 25, 100, 50);

    assertOverlayColorNotOk(1000, 50, 100, 50);
    assertOverlayColorNotOk(500, 50, 100, 50);
    assertOverlayColorNotOk(50, 100, 100, 50);
    assertOverlayColorNotOk(150, 25, 100, 50);

    assertOverlayColorNotOk(1000, 500, 100, 50);
    assertOverlayColorNotOk(10, 5, 100, 50);
    assertOverlayColorNotOk(1, 1, 100, 50);
  }

  @Test
  public void testOverlayColorNotOkWhenZeroDimension() {
    assertOverlayColorNotOk(0, 0, 0, 0);
    assertOverlayColorNotOk(0, 100, 100, 100);
    assertOverlayColorNotOk(100, 0, 100, 100);
    assertOverlayColorNotOk(100, 100, 0, 100);
    assertOverlayColorNotOk(100, 100, 100, 0);
  }

  private void assertOverlayColorOk(
      int imageWidth,
      int imageHeight,
      int drawableWidth,
      int drawableHeight) {
    mOverlayDrawable.setBounds(0, 0, drawableWidth, drawableHeight);
    assertEquals(
        DebugControllerOverlayDrawable.OVERLAY_COLOR_IMAGE_OK,
        mOverlayDrawable.determineOverlayColor(
            imageWidth,
            imageHeight));
  }

  private void assertOverlayColorAlmostOk(
      int imageWidth,
      int imageHeight,
      int drawableWidth,
      int drawableHeight) {
    mOverlayDrawable.setBounds(0, 0, drawableWidth, drawableHeight);
    assertEquals(
        DebugControllerOverlayDrawable.OVERLAY_COLOR_IMAGE_ALMOST_OK,
        mOverlayDrawable.determineOverlayColor(
            imageWidth,
            imageHeight));
  }

  private void assertOverlayColorNotOk(
      int imageWidth,
      int imageHeight,
      int drawableWidth,
      int drawableHeight) {
    mOverlayDrawable.setBounds(0, 0, drawableWidth, drawableHeight);
    assertEquals(
        DebugControllerOverlayDrawable.OVERLAY_COLOR_IMAGE_NOT_OK,
        mOverlayDrawable.determineOverlayColor(
            imageWidth,
            imageHeight));
  }
}
