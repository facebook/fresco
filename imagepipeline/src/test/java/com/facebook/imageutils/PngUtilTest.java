/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imageutils;

import android.graphics.Rect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Tests {@link PngUtil}
 */
@RunWith(RobolectricTestRunner.class)
public class PngUtilTest {

  @Test
  public void testGetDimensions() {
    Rect rect = PngUtil.getDimensions(PngUtilTest.class.getResourceAsStream("pngs/1.png"));
    assertEquals(240, rect.width());
    assertEquals(181, rect.height());
    rect = PngUtil.getDimensions(PngUtilTest.class.getResourceAsStream("pngs/2.png"));
    assertEquals(240, rect.width());
    assertEquals(246, rect.height());
    rect = PngUtil.getDimensions(PngUtilTest.class.getResourceAsStream("pngs/3.png"));
    assertEquals(240, rect.width());
    assertEquals(180, rect.height());
    rect = PngUtil.getDimensions(PngUtilTest.class.getResourceAsStream("jpegs/1.jpeg"));
    assertNull(rect);
  }

}
