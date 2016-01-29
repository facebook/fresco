/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.Matrix;
import android.graphics.Rect;

import org.robolectric.RobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.facebook.drawee.drawable.ScalingUtils.ScaleType;

/**
 * Tests for scale type calculations.
 */
@RunWith(RobolectricTestRunner.class)
public class ScalingUtilsTest {
  private final Matrix mExpectedMatrix = new Matrix();
  private final Matrix mActualMatrix = new Matrix();
  private final Rect mParentBounds = new Rect(10, 15, 410, 315);

  @Before
  public void setUp() {
  }

  @Test
  public void testFitXY() {
    test(1.60f, 2.00f, 10, 15, 250, 150, ScaleType.FIT_XY);
    test(0.50f, 1.50f, 10, 15, 800, 200, ScaleType.FIT_XY);
    test(0.50f, 0.75f, 10, 15, 800, 400, ScaleType.FIT_XY);
    test(2.00f, 2.00f, 10, 15, 200, 150, ScaleType.FIT_XY);
    test(1.00f, 1.00f, 10, 15, 400, 300, ScaleType.FIT_XY);
    test(0.50f, 0.50f, 10, 15, 800, 600, ScaleType.FIT_XY);
    test(2.00f, 1.50f, 10, 15, 200, 200, ScaleType.FIT_XY);
    test(2.00f, 0.75f, 10, 15, 200, 400, ScaleType.FIT_XY);
    test(0.80f, 0.75f, 10, 15, 500, 400, ScaleType.FIT_XY);
  }

  @Test
  public void testFitStart() {
    test(1.60f, 1.60f, 10, 15, 250, 150, ScaleType.FIT_START);
    test(0.50f, 0.50f, 10, 15, 800, 200, ScaleType.FIT_START);
    test(0.50f, 0.50f, 10, 15, 800, 400, ScaleType.FIT_START);
    test(2.00f, 2.00f, 10, 15, 200, 150, ScaleType.FIT_START);
    test(1.00f, 1.00f, 10, 15, 400, 300, ScaleType.FIT_START);
    test(0.50f, 0.50f, 10, 15, 800, 600, ScaleType.FIT_START);
    test(1.50f, 1.50f, 10, 15, 200, 200, ScaleType.FIT_START);
    test(0.75f, 0.75f, 10, 15, 200, 400, ScaleType.FIT_START);
    test(0.75f, 0.75f, 10, 15, 500, 400, ScaleType.FIT_START);
  }

  @Test
  public void testFitCenter() {
    test(1.60f, 1.60f,  10,  45, 250, 150, ScaleType.FIT_CENTER);
    test(0.50f, 0.50f,  10, 115, 800, 200, ScaleType.FIT_CENTER);
    test(0.50f, 0.50f,  10,  65, 800, 400, ScaleType.FIT_CENTER);
    test(2.00f, 2.00f,  10,  15, 200, 150, ScaleType.FIT_CENTER);
    test(1.00f, 1.00f,  10,  15, 400, 300, ScaleType.FIT_CENTER);
    test(0.50f, 0.50f,  10,  15, 800, 600, ScaleType.FIT_CENTER);
    test(1.50f, 1.50f,  60,  15, 200, 200, ScaleType.FIT_CENTER);
    test(0.75f, 0.75f, 135,  15, 200, 400, ScaleType.FIT_CENTER);
    test(0.75f, 0.75f,  23,  15, 500, 400, ScaleType.FIT_CENTER);
  }

  @Test
  public void testFitEnd() {
    test(1.60f, 1.60f,  10,  75, 250, 150, ScaleType.FIT_END);
    test(0.50f, 0.50f,  10, 215, 800, 200, ScaleType.FIT_END);
    test(0.50f, 0.50f,  10, 115, 800, 400, ScaleType.FIT_END);
    test(2.00f, 2.00f,  10,  15, 200, 150, ScaleType.FIT_END);
    test(1.00f, 1.00f,  10,  15, 400, 300, ScaleType.FIT_END);
    test(0.50f, 0.50f,  10,  15, 800, 600, ScaleType.FIT_END);
    test(1.50f, 1.50f, 110,  15, 200, 200, ScaleType.FIT_END);
    test(0.75f, 0.75f, 260,  15, 200, 400, ScaleType.FIT_END);
    test(0.75f, 0.75f,  35,  15, 500, 400, ScaleType.FIT_END);
  }

  @Test
  public void testCenter() {
    test(1.00f, 1.00f,   85,   90, 250, 150, ScaleType.CENTER);
    test(1.00f, 1.00f, -189,   65, 800, 200, ScaleType.CENTER);
    test(1.00f, 1.00f, -189,  -34, 800, 400, ScaleType.CENTER);
    test(1.00f, 1.00f,  110,   90, 200, 150, ScaleType.CENTER);
    test(1.00f, 1.00f,   10,   15, 400, 300, ScaleType.CENTER);
    test(1.00f, 1.00f, -189, -134, 800, 600, ScaleType.CENTER);
    test(1.00f, 1.00f,  110,   65, 200, 200, ScaleType.CENTER);
    test(1.00f, 1.00f,  110,  -34, 200, 400, ScaleType.CENTER);
    test(1.00f, 1.00f,  -39,  -34, 500, 400, ScaleType.CENTER);
  }

  @Test
  public void testCenterInside() {
    test(1.00f, 1.00f,  85,  90, 250, 150, ScaleType.CENTER_INSIDE);
    test(0.50f, 0.50f,  10, 115, 800, 200, ScaleType.CENTER_INSIDE);
    test(0.50f, 0.50f,  10,  65, 800, 400, ScaleType.CENTER_INSIDE);
    test(1.00f, 1.00f, 110,  90, 200, 150, ScaleType.CENTER_INSIDE);
    test(1.00f, 1.00f,  10,  15, 400, 300, ScaleType.CENTER_INSIDE);
    test(0.50f, 0.50f,  10,  15, 800, 600, ScaleType.CENTER_INSIDE);
    test(1.00f, 1.00f, 110,  65, 200, 200, ScaleType.CENTER_INSIDE);
    test(0.75f, 0.75f, 135,  15, 200, 400, ScaleType.CENTER_INSIDE);
    test(0.75f, 0.75f,  23,  15, 500, 400, ScaleType.CENTER_INSIDE);
  }

  @Test
  public void testCenterCrop() {
    test(2.00f, 2.00f,  -39,   15, 250, 150, ScaleType.CENTER_CROP);
    test(1.50f, 1.50f, -389,   15, 800, 200, ScaleType.CENTER_CROP);
    test(0.75f, 0.75f,  -89,   15, 800, 400, ScaleType.CENTER_CROP);
    test(2.00f, 2.00f,   10,   15, 200, 150, ScaleType.CENTER_CROP);
    test(1.00f, 1.00f,   10,   15, 400, 300, ScaleType.CENTER_CROP);
    test(0.50f, 0.50f,   10,   15, 800, 600, ScaleType.CENTER_CROP);
    test(2.00f, 2.00f,   10,  -34, 200, 200, ScaleType.CENTER_CROP);
    test(2.00f, 2.00f,   10, -234, 200, 400, ScaleType.CENTER_CROP);
    test(0.80f, 0.80f,   10,    5, 500, 400, ScaleType.CENTER_CROP);
  }

  @Test
  public void testFocusCrop_DefaultFocus() {
    test(2.00f, 2.00f,  -39,   15, 250, 150, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
    test(1.50f, 1.50f, -389,   15, 800, 200, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
    test(0.75f, 0.75f,  -89,   15, 800, 400, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10,   15, 200, 150, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
    test(1.00f, 1.00f,   10,   15, 400, 300, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
    test(0.50f, 0.50f,   10,   15, 800, 600, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10,  -34, 200, 200, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10, -234, 200, 400, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
    test(0.80f, 0.80f,   10,    5, 500, 400, 0.50f, 0.50f, ScaleType.FOCUS_CROP);
  }

  @Test
  public void testFocusCrop_FocusCentered() {
    test(2.00f, 2.00f,  -14,   15, 250, 150, 0.45f, 0.55f, ScaleType.FOCUS_CROP);
    test(1.50f, 1.50f, -329,   15, 800, 200, 0.45f, 0.55f, ScaleType.FOCUS_CROP);
    test(0.75f, 0.75f,  -59,   15, 800, 400, 0.45f, 0.55f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10,   15, 200, 150, 0.45f, 0.55f, ScaleType.FOCUS_CROP);
    test(1.00f, 1.00f,   10,   15, 400, 300, 0.45f, 0.55f, ScaleType.FOCUS_CROP);
    test(0.50f, 0.50f,   10,   15, 800, 600, 0.45f, 0.55f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10,  -54, 200, 200, 0.45f, 0.55f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10, -274, 200, 400, 0.45f, 0.55f, ScaleType.FOCUS_CROP);
    test(0.80f, 0.80f,   10,    2, 500, 400, 0.45f, 0.51f, ScaleType.FOCUS_CROP);
  }

  @Test
  public void testFocusCrop_FocusTopLeft() {
    test(2.00f, 2.00f, 10, 15, 250, 150, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
    test(1.50f, 1.50f, 10, 15, 800, 200, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
    test(0.75f, 0.75f, 10, 15, 800, 400, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f, 10, 15, 200, 150, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
    test(1.00f, 1.00f, 10, 15, 400, 300, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
    test(0.50f, 0.50f, 10, 15, 800, 600, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f, 10, 15, 200, 200, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f, 10, 15, 200, 400, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
    test(0.80f, 0.80f, 10, 15, 500, 400, 0.00f, 0.00f, ScaleType.FOCUS_CROP);
  }

  @Test
  public void testFocusCrop_FocusBottomRight() {
    test(2.00f, 2.00f,  -89,   15, 250, 150, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
    test(1.50f, 1.50f, -789,   15, 800, 200, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
    test(0.75f, 0.75f, -189,   15, 800, 400, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10,   15, 200, 150, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
    test(1.00f, 1.00f,   10,   15, 400, 300, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
    test(0.50f, 0.50f,   10,   15, 800, 600, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10,  -84, 200, 200, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
    test(2.00f, 2.00f,   10, -484, 200, 400, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
    test(0.80f, 0.80f,   10,   -4, 500, 400, 1.00f, 1.00f, ScaleType.FOCUS_CROP);
  }

  private void test(
      // expected
      float scaleX,
      float scaleY,
      float translateX,
      float translateY,
      // params
      int childWidth,
      int childHeight,
      ScaleType scaleType) {
    test(scaleX, scaleY, translateX, translateY, childWidth, childHeight, 0.5f, 0.5f, scaleType);
  }

  private void test(
      // expected
      float scaleX,
      float scaleY,
      float translateX,
      float translateY,
      // params
      int childWidth,
      int childHeight,
      float focusX,
      float focusY,
      ScaleType scaleType) {

    mExpectedMatrix.reset();
    if (scaleType == ScaleType.CENTER) {
      Assert.assertEquals(1.0f, scaleX, 0);
      Assert.assertEquals(1.0f, scaleY, 0);
      mExpectedMatrix.setTranslate(translateX, translateY);
    } else {
      createTransform(
          mExpectedMatrix,
          scaleX,
          scaleY,
          translateX,
          translateY);
    }

    mActualMatrix.reset();
    ScalingUtils.getTransform(
        mActualMatrix,
        mParentBounds,
        childWidth,
        childHeight,
        focusX,
        focusY,
        scaleType);

    AndroidGraphicsTestUtils.assertEquals(mExpectedMatrix, mActualMatrix);
  }

  private static Matrix createTransform(
      Matrix transform,
      float scaleX,
      float scaleY,
      float translateX,
      float translateY) {
    transform.setScale(scaleX, scaleY);
    transform.postTranslate(translateX, translateY);
    return transform;
  }
}
