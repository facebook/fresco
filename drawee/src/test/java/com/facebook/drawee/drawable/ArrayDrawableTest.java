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
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.robolectric.RobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ArrayDrawableTest {
  private Drawable mUnderlyingDrawable0;
  private Drawable mUnderlyingDrawable1;
  private Drawable mUnderlyingDrawable2;
  private ArrayDrawable mArrayDrawable;

  @Before
  public void setUp() {
    mUnderlyingDrawable0 = mock(Drawable.class);
    mUnderlyingDrawable1 = mock(Drawable.class);
    mUnderlyingDrawable2 = mock(Drawable.class);
    when(mUnderlyingDrawable0.mutate()).thenReturn(mUnderlyingDrawable0);
    when(mUnderlyingDrawable1.mutate()).thenReturn(mUnderlyingDrawable1);
    when(mUnderlyingDrawable2.mutate()).thenReturn(mUnderlyingDrawable2);
    mArrayDrawable = new ArrayDrawable(new Drawable[] {
        mUnderlyingDrawable0,
        mUnderlyingDrawable1,
        mUnderlyingDrawable2
    });
  }

  @Test
  public void testIntrinsicDimensions() {
    when(mUnderlyingDrawable0.getIntrinsicWidth()).thenReturn(100);
    when(mUnderlyingDrawable1.getIntrinsicWidth()).thenReturn(200);
    when(mUnderlyingDrawable2.getIntrinsicWidth()).thenReturn(150);
    when(mUnderlyingDrawable0.getIntrinsicHeight()).thenReturn(500);
    when(mUnderlyingDrawable1.getIntrinsicHeight()).thenReturn(300);
    when(mUnderlyingDrawable2.getIntrinsicHeight()).thenReturn(400);
    Assert.assertEquals(200, mArrayDrawable.getIntrinsicWidth());
    Assert.assertEquals(500, mArrayDrawable.getIntrinsicHeight());
  }

  @Test
  public void testGetDrawable() {
    Assert.assertEquals(mUnderlyingDrawable0, mArrayDrawable.getDrawable(0));
    Assert.assertEquals(mUnderlyingDrawable1, mArrayDrawable.getDrawable(1));
    Assert.assertEquals(mUnderlyingDrawable2, mArrayDrawable.getDrawable(2));
  }

  @Test
  public void testDraw() {
    Canvas mockCanvas = mock(Canvas.class);
    mArrayDrawable.draw(mockCanvas);
    verify(mUnderlyingDrawable0).draw(mockCanvas);
    verify(mUnderlyingDrawable1).draw(mockCanvas);
    verify(mUnderlyingDrawable2).draw(mockCanvas);
  }

  @Test
  public void testOnBoundsChange() {
    Rect rectMock = mock(Rect.class);
    mArrayDrawable.onBoundsChange(rectMock);
    verify(mUnderlyingDrawable0).setBounds(rectMock);
    verify(mUnderlyingDrawable1).setBounds(rectMock);
    verify(mUnderlyingDrawable2).setBounds(rectMock);
  }

  @Test
  public void testSetAlpha() {
    mArrayDrawable.setAlpha(11);
    verify(mUnderlyingDrawable0).setAlpha(11);
    verify(mUnderlyingDrawable1).setAlpha(11);
    verify(mUnderlyingDrawable2).setAlpha(11);
  }

  @Test
  public void testSetColorFilter() {
    ColorFilter colorFilter = mock(ColorFilter.class);
    mArrayDrawable.setColorFilter(colorFilter);
    verify(mUnderlyingDrawable0).setColorFilter(colorFilter);
    verify(mUnderlyingDrawable1).setColorFilter(colorFilter);
    verify(mUnderlyingDrawable2).setColorFilter(colorFilter);
  }

  @Test
  public void testSetDither() {
    testSetDither(true);
    testSetDither(false);
  }

  private void testSetDither(boolean dither) {
    reset(mUnderlyingDrawable0, mUnderlyingDrawable1, mUnderlyingDrawable2);
    mArrayDrawable.setDither(dither);
    verify(mUnderlyingDrawable0).setDither(dither);
    verify(mUnderlyingDrawable1).setDither(dither);
    verify(mUnderlyingDrawable2).setDither(dither);
  }

  @Test
  public void testSetFilterBitmap() {
    testSetFilterBitmap(true);
    testSetFilterBitmap(false);
  }

  private void testSetFilterBitmap(boolean filterBitmap) {
    mArrayDrawable.setFilterBitmap(filterBitmap);
    verify(mUnderlyingDrawable0).setFilterBitmap(filterBitmap);
    verify(mUnderlyingDrawable1).setFilterBitmap(filterBitmap);
    verify(mUnderlyingDrawable2).setFilterBitmap(filterBitmap);
  }

  @Test
  public void testSetVisible() {
    testSetVisible(true, true);
    testSetVisible(true, false);
    testSetVisible(false, true);
    testSetVisible(false, false);
  }

  private void testSetVisible(boolean visible, boolean restart) {
    mArrayDrawable.setVisible(visible, restart);
    verify(mUnderlyingDrawable0).setVisible(visible, restart);
    verify(mUnderlyingDrawable1).setVisible(visible, restart);
    verify(mUnderlyingDrawable2).setVisible(visible, restart);
  }

  @Test
  public void testSetDrawableNonMutated() {
    Drawable newDrawable = mock(Drawable.class);
    mArrayDrawable.setDrawable(1, newDrawable);
    assertSame(newDrawable, mArrayDrawable.getDrawable(1));
    verify(mUnderlyingDrawable1).setCallback(isNull(Drawable.Callback.class));
    verify(newDrawable).setCallback(eq(mArrayDrawable));
    verify(newDrawable, never()).mutate();
  }

  @Test
  public void testSetDrawableMutated() {
    BitmapDrawable newDrawable = mock(BitmapDrawable.class);
    when(newDrawable.mutate()).thenReturn(newDrawable);
    Rect rect = new Rect(1, 2, 3, 4);
    when(mUnderlyingDrawable1.getBounds()).thenReturn(rect);
    mArrayDrawable = (ArrayDrawable) mArrayDrawable.mutate();
    mArrayDrawable.setDrawable(1, newDrawable);
    verify(newDrawable).setBounds(eq(rect));
    verify(mUnderlyingDrawable1).setCallback(isNull(Drawable.Callback.class));
    verify(newDrawable).setCallback(eq(mArrayDrawable));
    verify(newDrawable).mutate();
  }
}
