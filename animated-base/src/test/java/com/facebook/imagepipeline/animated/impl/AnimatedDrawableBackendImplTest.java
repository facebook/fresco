/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.impl;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({Bitmap.class, Canvas.class})
public class AnimatedDrawableBackendImplTest {

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  @Mock public AnimatedDrawableUtil mAnimatedDrawableUtil;
  @Mock public AnimatedImageResult mAnimatedImageResult;
  @Mock public Canvas mCanvas;
  @Mock public AnimatedImage mImage;
  @Mock public AnimatedImageFrame mFrame;
  @Mock public Bitmap mBitmap;
  @Mock public Rect mRect;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(mAnimatedImageResult.getImage()).thenReturn(mImage);
    when(mImage.doesRenderSupportScaling()).thenReturn(false);

    when(mImage.getFrame(anyInt())).thenReturn(mFrame);

    PowerMockito.mockStatic(Bitmap.class);
    when(Bitmap.createBitmap(anyInt(), anyInt(), isA(Bitmap.Config.class))).thenReturn(mBitmap);
  }

  private void testBasic(
      int canvasWidth,
      int canvasHeight,
      int frameOriginalWidth,
      int frameOriginalHeight,
      int frameExpectedRenderedWidth,
      int frameExpectedRenderedHeight) {
    when(mCanvas.getWidth()).thenReturn(canvasWidth);
    when(mCanvas.getHeight()).thenReturn(canvasHeight);
    when(mFrame.getWidth()).thenReturn(frameOriginalWidth);
    when(mFrame.getHeight()).thenReturn(frameOriginalHeight);

    final AnimatedDrawableBackendImpl animatedDrawableBackendImpl =
        new AnimatedDrawableBackendImpl(mAnimatedDrawableUtil, mAnimatedImageResult, mRect, true);

    animatedDrawableBackendImpl.renderFrame(0, mCanvas);

    verify(mFrame).renderFrame(frameExpectedRenderedWidth, frameExpectedRenderedHeight, mBitmap);
  }

  @Test
  public void testSimple() {
    testBasic(128, 128, 512, 512, 128, 128);
  }

  @Test
  public void testNoUpscaling() {
    testBasic(128, 128, 16, 16, 16, 16);
  }

  @Test
  public void testNarrow() {
    testBasic(64, 128, 256, 256, 64, 64);
  }

  @Test
  public void testOffsets() {
    final int frameSide = 1024;
    final int canvasSide = 256;
    final int scale = frameSide / canvasSide;

    final int frameOffset = 512;
    when(mFrame.getXOffset()).thenReturn(frameOffset);
    when(mFrame.getYOffset()).thenReturn(frameOffset);

    testBasic(canvasSide, canvasSide, frameSide, frameSide, frameSide / scale, frameSide / scale);
    verify(mCanvas).translate(frameOffset / scale, frameOffset / scale);
  }
}
