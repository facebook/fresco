/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RoundedCornersDrawableTest {

  private Drawable mUnderlyingDrawable;
  private RoundedCornersDrawable mRoundedCornersDrawable;
  private Drawable.Callback mCallback;

  @Before
  public void setup() {
    mUnderlyingDrawable = mock(Drawable.class);
    mCallback = mock(Drawable.Callback.class);
    mRoundedCornersDrawable = new RoundedCornersDrawable(mUnderlyingDrawable);
    mRoundedCornersDrawable.setCallback(mCallback);
  }

  @Test
  public void testInitialSetup() {
    assertEquals(RoundedCornersDrawable.Type.OVERLAY_COLOR, mRoundedCornersDrawable.mType);
    assertFalse(mRoundedCornersDrawable.mIsCircle);
    assertArrayEquals(new float[]{0, 0, 0, 0, 0, 0, 0, 0}, mRoundedCornersDrawable.mRadii, 0);
    assertEquals(0, mRoundedCornersDrawable.mPaint.getColor());
  }

  @Test
  public void testSetType() {
    RoundedCornersDrawable.Type type = RoundedCornersDrawable.Type.CLIPPING;
    mRoundedCornersDrawable.setType(type);
    verify(mCallback).invalidateDrawable(mRoundedCornersDrawable);
    assertEquals(type, mRoundedCornersDrawable.mType);
  }

  @Test
  public void testSetCircle() {
    mRoundedCornersDrawable.setCircle(true);
    verify(mCallback).invalidateDrawable(mRoundedCornersDrawable);
    assertTrue(mRoundedCornersDrawable.mIsCircle);
  }

  @Test
  public void testSetRadii() {
    mRoundedCornersDrawable.setRadii(new float[]{1, 2, 3, 4, 5, 6, 7, 8});
    verify(mCallback).invalidateDrawable(mRoundedCornersDrawable);
    assertArrayEquals(new float[] {1, 2, 3, 4, 5, 6, 7, 8}, mRoundedCornersDrawable.mRadii, 0);
  }

  @Test
  public void testSetRadius() {
    mRoundedCornersDrawable.setRadius(9);
    verify(mCallback).invalidateDrawable(mRoundedCornersDrawable);
    assertArrayEquals(new float[]{9, 9, 9, 9, 9, 9, 9, 9}, mRoundedCornersDrawable.mRadii, 0);
  }

  @Test
  public void testSetOverlayColor() {
    int overlayColor = 0xC0123456;
    mRoundedCornersDrawable.setOverlayColor(overlayColor);
    verify(mCallback).invalidateDrawable(mRoundedCornersDrawable);
    assertEquals(overlayColor, mRoundedCornersDrawable.mOverlayColor);
  }

  @Test
  public void testSetBorder() {
    float borderWidth = 0.7f;
    int borderColor = Color.CYAN;
    mRoundedCornersDrawable.setBorder(borderColor, borderWidth);
    verify(mCallback).invalidateDrawable(mRoundedCornersDrawable);
    assertEquals(borderColor, mRoundedCornersDrawable.mBorderColor);
    assertEquals(borderWidth, mRoundedCornersDrawable.mBorderWidth, 0);
  }
}
