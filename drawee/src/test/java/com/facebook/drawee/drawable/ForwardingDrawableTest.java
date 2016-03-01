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
import android.graphics.drawable.Drawable;

import org.robolectric.RobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link ForwardingDrawable}
 */
@RunWith(RobolectricTestRunner.class)
public class ForwardingDrawableTest {
  private Drawable mInnerDrawable;
  private FakeForwardingDrawable mDrawable;

  @Before
  public void setup() {
    mInnerDrawable = mock(Drawable.class);
    mDrawable = new FakeForwardingDrawable(mInnerDrawable);
    // ForwardingDrawable will call mInnerDrawable.setCallback
    reset(mInnerDrawable);
  }

  @Test
  public void testIntrinsicDimensions() {
    when(mInnerDrawable.getIntrinsicWidth()).thenReturn(100);
    when(mInnerDrawable.getIntrinsicHeight()).thenReturn(200);
    Drawable drawable1 = new FakeForwardingDrawable(mInnerDrawable);
    Assert.assertEquals(100, drawable1.getIntrinsicWidth());
    Assert.assertEquals(200, drawable1.getIntrinsicHeight());
  }

  @Test
  public void testGetCurrent() {
    Assert.assertEquals(mInnerDrawable, mDrawable.getCurrent());
  }

  @Test
  public void testSettersAndGetters() {
    ColorFilter colorFilterMock = mock(ColorFilter.class);
    Rect rectMock = mock(Rect.class);
    int alpha = 77;
    boolean dither = true;
    boolean filterBitmap = false;
    boolean visible = false;
    boolean restart = false;

    //when(mInnerDrawable.setVisible(visible, restart)).thenReturn(true);
    when(mInnerDrawable.getOpacity()).thenReturn(11);
    Assert.assertEquals(11, mDrawable.getOpacity());

    mDrawable.getPadding(rectMock);
    mDrawable.setAlpha(alpha);
    mDrawable.setDither(dither);
    mDrawable.setFilterBitmap(filterBitmap);
    mDrawable.setColorFilter(colorFilterMock);
    mDrawable.onBoundsChange(rectMock);
    mDrawable.setVisible(visible, restart);

    verify(mInnerDrawable).getPadding(rectMock);
    verify(mInnerDrawable).setAlpha(alpha);
    verify(mInnerDrawable).setDither(dither);
    verify(mInnerDrawable).setFilterBitmap(filterBitmap);
    verify(mInnerDrawable).setColorFilter(colorFilterMock);
    verify(mInnerDrawable).setBounds(rectMock);
    verify(mInnerDrawable).setVisible(visible, restart);
  }

  @Test
  public void testDraw() {
    Canvas mockCanvas = mock(Canvas.class);
    mDrawable.draw(mockCanvas);
    verify(mInnerDrawable).draw(mockCanvas);
  }

  static class FakeForwardingDrawable extends ForwardingDrawable {
    public FakeForwardingDrawable(Drawable drawable) {
      super(drawable);
    }
  }

}
