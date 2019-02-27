/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.generic;

import static org.junit.Assert.*;

import android.graphics.Color;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RoundingParamsTest {

  private RoundingParams mRoundingParams;

  @Before
  public void setUp() {
    mRoundingParams = new RoundingParams();
  }

  @Test
  public void testDefaults() {
    assertEquals(RoundingParams.RoundingMethod.BITMAP_ONLY, mRoundingParams.getRoundingMethod());
    assertFalse(mRoundingParams.getRoundAsCircle());
    assertNull(mRoundingParams.getCornersRadii());
    assertEquals(0, mRoundingParams.getOverlayColor());
    assertFalse(mRoundingParams.getScaleDownInsideBorders());
    assertFalse(mRoundingParams.getPaintFilterBitmap());
  }

  @Test
  public void testSetCircle() {
    assertSame(mRoundingParams, mRoundingParams.setRoundAsCircle(true));
    assertTrue(mRoundingParams.getRoundAsCircle());
    assertSame(mRoundingParams, mRoundingParams.setRoundAsCircle(false));
    assertFalse(mRoundingParams.getRoundAsCircle());
  }

  @Test
  public void testSetRadii() {
    mRoundingParams.setCornersRadius(9);
    assertArrayEquals(new float[]{9, 9, 9, 9, 9, 9, 9, 9}, mRoundingParams.getCornersRadii(), 0f);

    mRoundingParams.setCornersRadii(8, 7, 2, 1);
    assertArrayEquals(new float[]{8, 8, 7, 7, 2, 2, 1, 1}, mRoundingParams.getCornersRadii(), 0f);

    mRoundingParams.setCornersRadii(new float[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6, 7, 8}, mRoundingParams.getCornersRadii(), 0f);
  }

  @Test
  public void testSetRoundingMethod() {
    mRoundingParams.setRoundingMethod(RoundingParams.RoundingMethod.OVERLAY_COLOR);
    assertEquals(RoundingParams.RoundingMethod.OVERLAY_COLOR, mRoundingParams.getRoundingMethod());
    mRoundingParams.setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
    assertEquals(RoundingParams.RoundingMethod.BITMAP_ONLY, mRoundingParams.getRoundingMethod());
  }

  @Test
  public void testSetOverlayColor() {
    mRoundingParams.setOverlayColor(0xC0123456);
    assertEquals(0xC0123456, mRoundingParams.getOverlayColor());
    assertEquals(RoundingParams.RoundingMethod.OVERLAY_COLOR, mRoundingParams.getRoundingMethod());
  }

  @Test
  public void testSetBorder() {
    int borderColor = Color.RED;
    float borderWidth = 0.8f;

    mRoundingParams.setBorder(borderColor, borderWidth);
    assertEquals(borderColor, mRoundingParams.getBorderColor());
    assertEquals(borderWidth, mRoundingParams.getBorderWidth(), 0);
  }

  @Test
  public void testSetScaleDownInsideBorders() {
    assertSame(mRoundingParams, mRoundingParams.setScaleDownInsideBorders(true));
    assertTrue(mRoundingParams.getScaleDownInsideBorders());
    assertSame(mRoundingParams, mRoundingParams.setScaleDownInsideBorders(false));
    assertFalse(mRoundingParams.getScaleDownInsideBorders());
  }

  @Test
  public void testSetPaintFilterBitmap() {
    assertSame(mRoundingParams, mRoundingParams.setPaintFilterBitmap(true));
    assertTrue(mRoundingParams.getPaintFilterBitmap());
    assertSame(mRoundingParams, mRoundingParams.setPaintFilterBitmap(false));
    assertFalse(mRoundingParams.getPaintFilterBitmap());
  }

  @Test
  public void testFactoryMethods() {
    RoundingParams params1 = RoundingParams.asCircle();
    assertTrue(params1.getRoundAsCircle());

    RoundingParams params2 = RoundingParams.fromCornersRadius(9);
    assertFalse(params2.getRoundAsCircle());
    assertArrayEquals(new float[]{9, 9, 9, 9, 9, 9, 9, 9}, params2.getCornersRadii(), 0f);

    RoundingParams params3 = RoundingParams.fromCornersRadii(8, 7, 2, 1);
    assertFalse(params3.getRoundAsCircle());
    assertArrayEquals(new float[]{8, 8, 7, 7, 2, 2, 1, 1}, params3.getCornersRadii(), 0f);

    RoundingParams params4 = RoundingParams.fromCornersRadii(new float[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertFalse(params4.getRoundAsCircle());
    assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6, 7, 8}, params4.getCornersRadii(), 0f);
  }
}
