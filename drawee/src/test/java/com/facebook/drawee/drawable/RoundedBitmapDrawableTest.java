/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RoundedBitmapDrawableTest {
  private Resources mResources;
  private Bitmap mBitmap;
  private DisplayMetrics mDisplayMetrics;

  RoundedBitmapDrawable mRoundedBitmapDrawable;
  RoundedBitmapDrawable mRoundedBitmapDrawableWithNullBitmap;

  private final Drawable.Callback mCallback = mock(Drawable.Callback.class);

  @Before
  public void setUp() {
    mResources = mock(Resources.class);
    mBitmap = mock(Bitmap.class);
    mDisplayMetrics = mock(DisplayMetrics.class);
    when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);
    mRoundedBitmapDrawable = new RoundedBitmapDrawable(mResources, mBitmap);
    mRoundedBitmapDrawable.setCallback(mCallback);

    mRoundedBitmapDrawableWithNullBitmap = new RoundedBitmapDrawable(mResources, null);
    mRoundedBitmapDrawable.setCallback(mCallback);
  }

  @Test
  public void testSetCircle() {
    mRoundedBitmapDrawable.setCircle(true);
    verify(mCallback).invalidateDrawable(mRoundedBitmapDrawable);
    assertTrue(mRoundedBitmapDrawable.isCircle());
  }

  @Test
  public void testSetRadii() {
    mRoundedBitmapDrawable.setRadii(new float[]{1, 2, 3, 4, 5, 6, 7, 8});
    verify(mCallback).invalidateDrawable(mRoundedBitmapDrawable);
    assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6, 7, 8}, mRoundedBitmapDrawable.getRadii(), 0);
  }

  @Test
  public void testSetRadius() {
    mRoundedBitmapDrawable.setRadius(9);
    verify(mCallback).invalidateDrawable(mRoundedBitmapDrawable);
    assertArrayEquals(new float[]{9, 9, 9, 9, 9, 9, 9, 9}, mRoundedBitmapDrawable.getRadii(), 0);
  }

  @Test
  public void testSetBorder() {
    int color = 0x12345678;
    float width = 5;
    mRoundedBitmapDrawable.setBorder(color, width);
    verify(mCallback).invalidateDrawable(mRoundedBitmapDrawable);
    assertEquals(color, mRoundedBitmapDrawable.getBorderColor());
    assertEquals(width, mRoundedBitmapDrawable.getBorderWidth(), 0);
  }

  @Test
  public void testSetPadding() {
    float padding = 10;
    mRoundedBitmapDrawable.setPadding(padding);
    verify(mCallback).invalidateDrawable(mRoundedBitmapDrawable);
    assertEquals(padding, mRoundedBitmapDrawable.getPadding(), 0);
  }

  @Test
  public void testSetScaleDownInsideBorders() {
    mRoundedBitmapDrawable.setScaleDownInsideBorders(true);
    verify(mCallback).invalidateDrawable(mRoundedBitmapDrawable);
    assertTrue(mRoundedBitmapDrawable.getScaleDownInsideBorders());
  }

  @Test
  public void testSetPaintFilterBitmap() {
    mRoundedBitmapDrawable.setPaintFilterBitmap(true);
    verify(mCallback).invalidateDrawable(mRoundedBitmapDrawable);
    assertTrue(mRoundedBitmapDrawable.getPaintFilterBitmap());
  }

  @Test
  public void testShouldRoundDefault() {
    assertFalse(mRoundedBitmapDrawable.shouldRound());
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
  }

  @Test
  public void testShouldRoundRadius() {
    mRoundedBitmapDrawable.setRadius(5);
    assertTrue(mRoundedBitmapDrawable.shouldRound());
    mRoundedBitmapDrawable.setRadius(0);
    assertFalse(mRoundedBitmapDrawable.shouldRound());

    mRoundedBitmapDrawableWithNullBitmap.setRadius(5);
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
    mRoundedBitmapDrawableWithNullBitmap.setRadius(0);
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
  }

  @Test
  public void testShouldRoundRadii() {
    mRoundedBitmapDrawable.setRadii(new float[]{0, 0, 0, 0, 0, 0, 0, 1});
    assertTrue(mRoundedBitmapDrawable.shouldRound());
    mRoundedBitmapDrawable.setRadii(new float[]{0, 0, 0, 0, 0, 0, 0, 0});
    assertFalse(mRoundedBitmapDrawable.shouldRound());

    mRoundedBitmapDrawableWithNullBitmap.setRadii(new float[]{0, 0, 0, 0, 0, 0, 0, 1});
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
    mRoundedBitmapDrawableWithNullBitmap.setRadii(new float[]{0, 0, 0, 0, 0, 0, 0, 0});
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
  }

  @Test
  public void testShouldRoundCircle() {
    mRoundedBitmapDrawable.setCircle(true);
    assertTrue(mRoundedBitmapDrawable.shouldRound());
    mRoundedBitmapDrawable.setCircle(false);
    assertFalse(mRoundedBitmapDrawable.shouldRound());

    mRoundedBitmapDrawableWithNullBitmap.setCircle(true);
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
    mRoundedBitmapDrawableWithNullBitmap.setCircle(false);
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
  }

  @Test
  public void testShouldRoundBorder() {
    mRoundedBitmapDrawable.setBorder(0xFFFFFFFF, 1);
    assertTrue(mRoundedBitmapDrawable.shouldRound());
    mRoundedBitmapDrawable.setBorder(0x00000000, 0);
    assertFalse(mRoundedBitmapDrawable.shouldRound());

    mRoundedBitmapDrawableWithNullBitmap.setBorder(0xFFFFFFFF, 1);
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
    mRoundedBitmapDrawableWithNullBitmap.setBorder(0x00000000, 0);
    assertFalse(mRoundedBitmapDrawableWithNullBitmap.shouldRound());
  }

  @Test
  public void testPreservePaintOnDrawableCopy() {
    ColorFilter colorFilter = mock(ColorFilter.class);
    Paint originalPaint = mock(Paint.class);
    BitmapDrawable originalVersion = mock(BitmapDrawable.class);

    originalPaint.setColorFilter(colorFilter);
    when(originalVersion.getPaint()).thenReturn(originalPaint);

    RoundedBitmapDrawable roundedVersion = RoundedBitmapDrawable.fromBitmapDrawable(
        mResources,
        originalVersion);

    assertEquals(
        originalVersion.getPaint().getColorFilter(),
        roundedVersion.getPaint().getColorFilter());
  }
}
