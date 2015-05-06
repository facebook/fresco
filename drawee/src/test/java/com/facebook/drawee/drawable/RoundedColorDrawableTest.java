/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Matchers.any;

@RunWith(RobolectricTestRunner.class)
public class RoundedColorDrawableTest {

  private Canvas mCanvas;
  private Drawable.Callback mCallback;

  private RoundedColorDrawable mRoundedColorDrawable;

  @Before
  public void setup() {
    mCanvas = mock(Canvas.class);
    mCallback = mock(Drawable.Callback.class);
    mRoundedColorDrawable = new RoundedColorDrawable(Color.GREEN);
    mRoundedColorDrawable.setCallback(mCallback);
  }

  @Test
  public void testInitialSetup() {
    assertEquals(Color.GREEN, mRoundedColorDrawable.getColor());
    assertFalse(mRoundedColorDrawable.mIsCircle);
    assertArrayEquals(new float[] {0, 0, 0, 0, 0, 0, 0, 0}, mRoundedColorDrawable.mRadii, 0f);
  }

  @Test
  public void testSetCircle() {
    mRoundedColorDrawable.setCircle(true);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertTrue(mRoundedColorDrawable.mIsCircle);
  }

  @Test
  public void testSetRadii() {
    float[] radii = {8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f};
    float[] expectedRadii = {8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f};
    mRoundedColorDrawable.setRadii(radii);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertArrayEquals(expectedRadii, mRoundedColorDrawable.mRadii, 0f);
  }

  @Test
  public void testSetRadius() {
    float radius = 8f;
    float[] expectedRadii = {8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f};
    mRoundedColorDrawable.setRadius(radius);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertArrayEquals(expectedRadii, mRoundedColorDrawable.mRadii, 0f);
  }

  @Test
  public void testSetColor() {
    int color = 0xC0223456;
    mRoundedColorDrawable.setColor(color);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertEquals(color, mRoundedColorDrawable.getColor());
  }

  @Test
  public void testSetAlpha() {
    int alpha = 10;
    mRoundedColorDrawable.setAlpha(alpha);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertEquals(alpha, mRoundedColorDrawable.getAlpha());
  }

  @Test
  public void testSetBorder() {
    int color = 0xC0223456;
    float width = 5;
    mRoundedColorDrawable.setBorder(color, width);
    verify(mCallback, times(2)).invalidateDrawable(mRoundedColorDrawable);
    assertEquals(color, mRoundedColorDrawable.mBorderColor);
    assertEquals(width, mRoundedColorDrawable.mBorderWidth, 0);
  }

  @Test
  public void testDrawWithoutBorder() {
    int internalColor = 0xC0223456;
    int alpha = 10;
    int expectedInternalPaintColor = 0x07223456;

    mRoundedColorDrawable.setAlpha(alpha);
    mRoundedColorDrawable.setColor(internalColor);
    mRoundedColorDrawable.draw(mCanvas);

    ArgumentCaptor<Paint> argumentCaptor = ArgumentCaptor.forClass(Paint.class);
    verify(mCanvas).drawPath(any(Path.class), argumentCaptor.capture());

    Paint internalPaint = argumentCaptor.getValue();
    assertEquals(expectedInternalPaintColor, internalPaint.getColor());
    assertEquals(Paint.Style.FILL, internalPaint.getStyle());
  }

  @Test
  public void testDrawWithBorder() {
    int internalColor = 0xC0223456;
    int alpha = 10;
    int borderColor = 0xC0123456;
    int expectedBorderPaintColor = 0x07123456;
    float borderWidth = 5;

    mRoundedColorDrawable.setAlpha(alpha);
    mRoundedColorDrawable.setColor(internalColor);
    mRoundedColorDrawable.setBorder(borderColor, borderWidth);
    mRoundedColorDrawable.draw(mCanvas);

    ArgumentCaptor<Paint> argumentCaptor = ArgumentCaptor.forClass(Paint.class);
    verify(mCanvas, times(2)).drawPath(any(Path.class), argumentCaptor.capture());
    assertEquals(2, argumentCaptor.getAllValues().size());

    Paint borderPaint = argumentCaptor.getAllValues().get(1);
    assertEquals(expectedBorderPaintColor, borderPaint.getColor());
    assertEquals(Paint.Style.STROKE, borderPaint.getStyle());
    assertEquals(borderWidth, borderPaint.getStrokeWidth(), 0);
  }

  @Test
  public void testGetOpacity() {
    mRoundedColorDrawable.setColor(0x8FFFFFFF);
    mRoundedColorDrawable.setAlpha(255);
    assertEquals(PixelFormat.TRANSLUCENT, mRoundedColorDrawable.getOpacity());

    mRoundedColorDrawable.setColor(0x00000000);
    mRoundedColorDrawable.setAlpha(255);
    assertEquals(PixelFormat.TRANSPARENT, mRoundedColorDrawable.getOpacity());

    mRoundedColorDrawable.setColor(0xFFFFFFFF);
    mRoundedColorDrawable.setAlpha(255);
    assertEquals(PixelFormat.OPAQUE, mRoundedColorDrawable.getOpacity());

    mRoundedColorDrawable.setAlpha(100);
    assertEquals(PixelFormat.TRANSLUCENT, mRoundedColorDrawable.getOpacity());

    mRoundedColorDrawable.setAlpha(0);
    assertEquals(PixelFormat.TRANSPARENT, mRoundedColorDrawable.getOpacity());
  }
}
