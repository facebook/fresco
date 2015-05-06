/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ForwardingDrawable}
 */
@RunWith(RobolectricTestRunner.class)
public class DrawableUtilsTest {

  private final Rect mBounds = mock(Rect.class);
  private final int mChangingConfigurations = 0x12345678;
  private final int mLevel = 3;
  private final boolean mIsVisible = true;
  private final int[] mState = new int[5];

  private final Drawable.Callback mCallback = mock(Drawable.Callback.class);
  private final TransformCallback mTransformCallback = mock(TransformCallback.class);

  @Before
  public void setup() {
  }

  private void testCopyProperties(Drawable drawableFrom, Drawable drawableTo) {
    when(drawableFrom.getBounds()).thenReturn(mBounds);
    when(drawableFrom.getChangingConfigurations()).thenReturn(mChangingConfigurations);
    when(drawableFrom.getLevel()).thenReturn(mLevel);
    when(drawableFrom.isVisible()).thenReturn(mIsVisible);
    when(drawableFrom.getState()).thenReturn(mState);
    DrawableUtils.copyProperties(drawableTo, drawableFrom);
    verify(drawableTo).setBounds(mBounds);
    verify(drawableTo).setChangingConfigurations(mChangingConfigurations);
    verify(drawableTo).setLevel(mLevel);
    verify(drawableTo).setVisible(mIsVisible, false);
    verify(drawableTo).setState(mState);
  }

  @Test
  public void testCopyProperties() {
    testCopyProperties(mock(Drawable.class), mock(Drawable.class));
  }

  @Test
  public void testSetDrawableProperties() {
    DrawableProperties properties = mock(DrawableProperties.class);
    ColorFilter colorFilter = mock(ColorFilter.class);
    when(properties.getAlpha()).thenReturn(42);
    when(properties.getColorFilter()).thenReturn(colorFilter);
    when(properties.isDither()).thenReturn(true);
    when(properties.isFilterBitmap()).thenReturn(true);
    Drawable drawableTo = mock(Drawable.class);
    DrawableUtils.setDrawableProperties(drawableTo, properties);
    verify(drawableTo).setAlpha(42);
    verify(drawableTo).setColorFilter(colorFilter);
    verify(drawableTo).setDither(true);
    verify(drawableTo).setFilterBitmap(true);
  }

  @Test
  public void testCopyProperties_Null() {
    Drawable drawableFrom = mock(Drawable.class);
    Drawable drawableTo = mock(Drawable.class);
    DrawableUtils.copyProperties(null, drawableFrom);
    DrawableUtils.copyProperties(drawableTo, null);
    verifyNoMoreInteractions(drawableTo, drawableFrom);
  }

  @Test
  public void testCopyProperties_Same() {
    Drawable drawable = mock(Drawable.class);
    DrawableUtils.copyProperties(drawable, drawable);
    verifyNoMoreInteractions(drawable);
  }

  @Test
  public void testSetCallbacks() {
    Drawable drawable = mock(Drawable.class);
    DrawableUtils.setCallbacks(drawable, mCallback, mTransformCallback);
    verify(drawable).setCallback(mCallback);
  }

  @Test
  public void testSetCallbacks_TransformAwareDrawable() {
    ForwardingDrawable transformAwareDrawable = mock(ForwardingDrawable.class);
    DrawableUtils.setCallbacks(transformAwareDrawable, mCallback, mTransformCallback);
    verify(transformAwareDrawable).setCallback(mCallback);
    verify(transformAwareDrawable).setTransformCallback(mTransformCallback);
  }

  @Test
  public void testSetCallbacks_NullCallback() {
    Drawable drawable = mock(Drawable.class);
    DrawableUtils.setCallbacks(drawable, null, null);
    verify(drawable).setCallback(null);
  }

  @Test
  public void testSetCallbacks_TransformAwareDrawable_NullCallback() {
    ForwardingDrawable transformAwareDrawable = mock(ForwardingDrawable.class);
    DrawableUtils.setCallbacks(transformAwareDrawable, null, null);
    verify(transformAwareDrawable).setCallback(null);
    verify(transformAwareDrawable).setTransformCallback(null);
  }

  @Test
  public void testSetCallbacks_NullDrawable() {
    DrawableUtils.setCallbacks(null, mCallback, mTransformCallback);
  }

  @Test
  public void testMultiplyColorAlpha() {
    assertEquals(0x00123456, DrawableUtils.multiplyColorAlpha(0xC0123456, 0));
    assertEquals(0x07123456, DrawableUtils.multiplyColorAlpha(0xC0123456, 10));
    assertEquals(0x96123456, DrawableUtils.multiplyColorAlpha(0xC0123456, 200));
    assertEquals(0xC0123456, DrawableUtils.multiplyColorAlpha(0xC0123456, 255));
  }

  @Test
  public void testGetOpacityFromColor() {
    assertEquals(PixelFormat.TRANSPARENT, DrawableUtils.getOpacityFromColor(0x00000000));
    assertEquals(PixelFormat.TRANSPARENT, DrawableUtils.getOpacityFromColor(0x00123456));
    assertEquals(PixelFormat.TRANSPARENT, DrawableUtils.getOpacityFromColor(0x00FFFFFF));
    assertEquals(PixelFormat.TRANSLUCENT, DrawableUtils.getOpacityFromColor(0xC0000000));
    assertEquals(PixelFormat.TRANSLUCENT, DrawableUtils.getOpacityFromColor(0xC0123456));
    assertEquals(PixelFormat.TRANSLUCENT, DrawableUtils.getOpacityFromColor(0xC0FFFFFF));
    assertEquals(PixelFormat.OPAQUE, DrawableUtils.getOpacityFromColor(0xFF000000));
    assertEquals(PixelFormat.OPAQUE, DrawableUtils.getOpacityFromColor(0xFF123456));
    assertEquals(PixelFormat.OPAQUE, DrawableUtils.getOpacityFromColor(0xFFFFFFFF));
  }
}
