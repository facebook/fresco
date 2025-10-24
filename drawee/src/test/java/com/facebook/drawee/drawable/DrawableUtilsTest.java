/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ForwardingDrawable} */
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
  public void setup() {}

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
    DrawableProperties properties = new DrawableProperties();
    ColorFilter colorFilter = mock(ColorFilter.class);
    properties.setAlpha(42);
    properties.setColorFilter(colorFilter);
    properties.setDither(true);
    properties.setFilterBitmap(true);
    Drawable drawableTo = mock(Drawable.class);
    DrawableUtils.setDrawableProperties(drawableTo, properties);
    verify(drawableTo).setAlpha(42);
    verify(drawableTo).setColorFilter(colorFilter);
    verify(drawableTo).setDither(true);
    verify(drawableTo).setFilterBitmap(true);
  }

  @Test
  public void testSetDrawablePropertiesDefault() {
    DrawableProperties properties = new DrawableProperties();
    Drawable drawableTo = mock(Drawable.class);
    DrawableUtils.setDrawableProperties(drawableTo, properties);
    verify(drawableTo, never()).setAlpha(anyInt());
    verify(drawableTo, never()).setColorFilter(any(ColorFilter.class));
    verify(drawableTo, never()).setDither(anyBoolean());
    verify(drawableTo, never()).setFilterBitmap(anyBoolean());
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
    assertThat(DrawableUtils.multiplyColorAlpha(0xC0123456, 0)).isEqualTo(0x00123456);
    assertThat(DrawableUtils.multiplyColorAlpha(0xC0123456, 10)).isEqualTo(0x07123456);
    assertThat(DrawableUtils.multiplyColorAlpha(0xC0123456, 200)).isEqualTo(0x96123456);
    assertThat(DrawableUtils.multiplyColorAlpha(0xC0123456, 255)).isEqualTo(0xC0123456);
  }

  @Test
  public void testGetOpacityFromColor() {
    assertThat(DrawableUtils.getOpacityFromColor(0x00000000)).isEqualTo(PixelFormat.TRANSPARENT);
    assertThat(DrawableUtils.getOpacityFromColor(0x00123456)).isEqualTo(PixelFormat.TRANSPARENT);
    assertThat(DrawableUtils.getOpacityFromColor(0x00FFFFFF)).isEqualTo(PixelFormat.TRANSPARENT);
    assertThat(DrawableUtils.getOpacityFromColor(0xC0000000)).isEqualTo(PixelFormat.TRANSLUCENT);
    assertThat(DrawableUtils.getOpacityFromColor(0xC0123456)).isEqualTo(PixelFormat.TRANSLUCENT);
    assertThat(DrawableUtils.getOpacityFromColor(0xC0FFFFFF)).isEqualTo(PixelFormat.TRANSLUCENT);
    assertThat(DrawableUtils.getOpacityFromColor(0xFF000000)).isEqualTo(PixelFormat.OPAQUE);
    assertThat(DrawableUtils.getOpacityFromColor(0xFF123456)).isEqualTo(PixelFormat.OPAQUE);
    assertThat(DrawableUtils.getOpacityFromColor(0xFFFFFFFF)).isEqualTo(PixelFormat.OPAQUE);
  }
}
