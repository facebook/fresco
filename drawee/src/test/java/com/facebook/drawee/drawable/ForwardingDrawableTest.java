/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.mockito.Mockito.*;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link ForwardingDrawable}
 */
@RunWith(RobolectricTestRunner.class)
public class ForwardingDrawableTest {
  private Drawable mInnerDrawable;
  private ForwardingDrawable mDrawable;

  @Before
  public void setup() {
    mInnerDrawable = mock(Drawable.class);
    mDrawable = new ForwardingDrawable(mInnerDrawable);
    // ForwardingDrawable will call mInnerDrawable.setCallback
    reset(mInnerDrawable);
  }

  @Test
  public void testIntrinsicDimensions() {
    when(mInnerDrawable.getIntrinsicWidth()).thenReturn(100);
    when(mInnerDrawable.getIntrinsicHeight()).thenReturn(200);
    Drawable drawable1 = new ForwardingDrawable(mInnerDrawable);
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

  @Test
  public void testCopyProperties() {
    Rect rect = new Rect(10, 20, 30, 40);
    int config = 11;
    int level = 100;
    boolean visible = true;
    int[] stateSet = new int[]{1, 2};

    mDrawable.setBounds(rect);
    mDrawable.setChangingConfigurations(config);
    mDrawable.setLevel(level);
    mDrawable.setVisible(visible, false);
    mDrawable.setState(stateSet);

    Drawable newDrawable = mock(Drawable.class);
    mDrawable.setCurrent(newDrawable);

    verify(newDrawable).setBounds(rect);
    verify(newDrawable).setChangingConfigurations(config);
    verify(newDrawable).setLevel(level);
    verify(newDrawable).setVisible(visible, false);
    verify(newDrawable).setState(stateSet);
  }
}
