/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.debug;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests {@link DebugControllerOverlayDrawable}
 */
@RunWith(RobolectricTestRunner.class)
public class DebugControllerOverlayDrawableTest {

  DebugControllerOverlayDrawableTestHelper helper;

  @Before
  public void setUp() {
    helper = new DebugControllerOverlayDrawableTestHelper();
  }

  @Test
  public void testOverlayColorOkWhenSameSize() {
    helper.assertOverlayColorOk(100, 100, 100, 100, null);
    helper.assertOverlayColorOk(160, 90, 160, 90, null);
    helper.assertOverlayColorOk(10, 400, 10, 400, null);
  }

  @Test
  public void testOverlayColorOkWhenDifferentSize() {
    helper.assertOverlayColorOk(101, 50, 100, 50, null);
    helper.assertOverlayColorOk(99, 50, 100, 50, null);
    helper.assertOverlayColorOk(100, 52, 100, 50, null);
    helper.assertOverlayColorOk(100, 48, 100, 50, null);

    helper.assertOverlayColorOk(101, 49, 100, 50, null);
    helper.assertOverlayColorOk(98, 52, 100, 50, null);

    helper.assertOverlayColorOk(109, 50, 100, 50, null);
    helper.assertOverlayColorOk(91, 50, 100, 50, null);
    helper.assertOverlayColorOk(100, 54, 100, 50, null);
    helper.assertOverlayColorOk(100, 46, 100, 50, null);

    helper.assertOverlayColorOk(109, 54, 100, 50, null);
    helper.assertOverlayColorOk(109, 46, 100, 50, null);
    helper.assertOverlayColorOk(91, 54, 100, 50, null);
    helper.assertOverlayColorOk(91, 46, 100, 50, null);
  }

  @Test
  public void testOverlayColorAlmostOk() {
    helper.assertOverlayColorAlmostOk(110, 50, 100, 50, null);
    helper.assertOverlayColorAlmostOk(90, 50, 100, 50, null);
    helper.assertOverlayColorAlmostOk(100, 45, 100, 50, null);
    helper.assertOverlayColorAlmostOk(100, 55, 100, 50, null);

    helper.assertOverlayColorAlmostOk(110, 55, 100, 50, null);
    helper.assertOverlayColorAlmostOk(110, 45, 100, 50, null);
    helper.assertOverlayColorAlmostOk(90, 55, 100, 50, null);
    helper.assertOverlayColorAlmostOk(90, 45, 100, 50, null);

    helper.assertOverlayColorAlmostOk(149, 50, 100, 50, null);
    helper.assertOverlayColorAlmostOk(149, 74, 100, 50, null);
    helper.assertOverlayColorAlmostOk(51, 74, 100, 50, null);
    helper.assertOverlayColorAlmostOk(51, 26, 100, 50, null);
  }

  @Test
  public void testOverlayColorNotOk() {
    helper.assertOverlayColorNotOk(150, 50, 100, 50, null);
    helper.assertOverlayColorNotOk(100, 75, 100, 50, null);
    helper.assertOverlayColorNotOk(100, 100, 100, 50, null);
    helper.assertOverlayColorNotOk(50, 50, 100, 50, null);
    helper.assertOverlayColorNotOk(100, 25, 100, 50, null);

    helper.assertOverlayColorNotOk(1000, 50, 100, 50, null);
    helper.assertOverlayColorNotOk(500, 50, 100, 50, null);
    helper.assertOverlayColorNotOk(50, 100, 100, 50, null);
    helper.assertOverlayColorNotOk(150, 25, 100, 50, null);

    helper.assertOverlayColorNotOk(1000, 500, 100, 50, null);
    helper.assertOverlayColorNotOk(10, 5, 100, 50, null);
    helper.assertOverlayColorNotOk(1, 1, 100, 50, null);
  }

  @Test
  public void testOverlayColorNotOkWhenZeroDimension() {
    helper.assertOverlayColorNotOk(0, 0, 0, 0, null);
    helper.assertOverlayColorNotOk(0, 100, 100, 100, null);
    helper.assertOverlayColorNotOk(100, 0, 100, 100, null);
    helper.assertOverlayColorNotOk(100, 100, 0, 100, null);
    helper.assertOverlayColorNotOk(100, 100, 100, 0, null);
  }
}
