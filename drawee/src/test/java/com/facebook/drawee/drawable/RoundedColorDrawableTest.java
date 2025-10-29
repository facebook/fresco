/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

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
    assertThat(mRoundedColorDrawable.getColor()).isEqualTo(Color.GREEN);
    assertThat(mRoundedColorDrawable.isCircle()).isFalse();
    assertThat(mRoundedColorDrawable.getRadii())
        .containsExactly(new float[] {0, 0, 0, 0, 0, 0, 0, 0});
  }

  @Test
  public void testSetCircle() {
    mRoundedColorDrawable.setCircle(true);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.isCircle()).isTrue();
  }

  @Test
  public void testSetRadii() {
    float[] radii = {8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f};
    float[] expectedRadii = {8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f};
    mRoundedColorDrawable.setRadii(radii);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.getRadii()).containsExactly(expectedRadii);
  }

  @Test
  public void testSetRadius() {
    float radius = 8f;
    float[] expectedRadii = {8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f};
    mRoundedColorDrawable.setRadius(radius);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.getRadii()).containsExactly(expectedRadii);
  }

  @Test
  public void testSetColor() {
    int color = 0xC0223456;
    mRoundedColorDrawable.setColor(color);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.getColor()).isEqualTo(color);
  }

  @Test
  public void testSetAlpha() {
    int alpha = 10;
    mRoundedColorDrawable.setAlpha(alpha);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.getAlpha()).isEqualTo(alpha);
  }

  @Test
  public void testSetBorder() {
    int color = 0xC0223456;
    float width = 5;
    mRoundedColorDrawable.setBorder(color, width);
    verify(mCallback, times(2)).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.getBorderColor()).isEqualTo(color);
    assertThat(mRoundedColorDrawable.getBorderWidth()).isEqualTo(width);
  }

  @Test
  public void testSetPadding() {
    float padding = 10;
    mRoundedColorDrawable.setPadding(padding);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.getPadding()).isEqualTo(padding);
  }

  @Test
  public void testSetScaleDownInsideBorders() {
    mRoundedColorDrawable.setScaleDownInsideBorders(true);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.getScaleDownInsideBorders()).isTrue();
  }

  @Test
  public void testSetPaintFilterBitmap() {
    mRoundedColorDrawable.setPaintFilterBitmap(true);
    verify(mCallback).invalidateDrawable(mRoundedColorDrawable);
    assertThat(mRoundedColorDrawable.getPaintFilterBitmap()).isTrue();
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
    assertThat(internalPaint.getColor()).isEqualTo(expectedInternalPaintColor);
    assertThat(internalPaint.getStyle()).isEqualTo(Paint.Style.FILL);
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
    assertThat(argumentCaptor.getAllValues().size()).isEqualTo(2);

    Paint borderPaint = argumentCaptor.getAllValues().get(1);
    assertThat(borderPaint.getColor()).isEqualTo(expectedBorderPaintColor);
    assertThat(borderPaint.getStyle()).isEqualTo(Paint.Style.STROKE);
    assertThat(borderPaint.getStrokeWidth()).isEqualTo(borderWidth);
  }

  @Test
  public void testGetOpacity() {
    mRoundedColorDrawable.setColor(0x8FFFFFFF);
    mRoundedColorDrawable.setAlpha(255);
    assertThat(mRoundedColorDrawable.getOpacity()).isEqualTo(PixelFormat.TRANSLUCENT);

    mRoundedColorDrawable.setColor(0x00000000);
    mRoundedColorDrawable.setAlpha(255);
    assertThat(mRoundedColorDrawable.getOpacity()).isEqualTo(PixelFormat.TRANSPARENT);

    mRoundedColorDrawable.setColor(0xFFFFFFFF);
    mRoundedColorDrawable.setAlpha(255);
    assertThat(mRoundedColorDrawable.getOpacity()).isEqualTo(PixelFormat.OPAQUE);

    mRoundedColorDrawable.setAlpha(100);
    assertThat(mRoundedColorDrawable.getOpacity()).isEqualTo(PixelFormat.TRANSLUCENT);

    mRoundedColorDrawable.setAlpha(0);
    assertThat(mRoundedColorDrawable.getOpacity()).isEqualTo(PixelFormat.TRANSPARENT);
  }
}
