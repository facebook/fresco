/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.backend;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests {@link AnimationBackendDelegate}
 */
@RunWith(RobolectricTestRunner.class)
public class AnimationBackendDelegateTest {

  private AnimationBackendDelegate<AnimationBackend> mAnimationBackendDelegate;

  private AnimationBackend mAnimationBackend;
  private Drawable mParent;
  private Canvas mCanvas;

  @Before
  public void setup() {
    mAnimationBackend = mock(AnimationBackend.class);
    mParent = mock(Drawable.class);
    mCanvas = mock(Canvas.class);

    mAnimationBackendDelegate = new AnimationBackendDelegate<>(mAnimationBackend);
  }

  @Test
  public void testForwardProperties() {
    ColorFilter colorFilter = mock(ColorFilter.class);
    Rect bounds = mock(Rect.class);
    int alphaValue = 123;

    verifyZeroInteractions(mAnimationBackend);

    // Set values to be persisted
    mAnimationBackendDelegate.setAlpha(alphaValue);
    mAnimationBackendDelegate.setColorFilter(colorFilter);
    mAnimationBackendDelegate.setBounds(bounds);

    // Verify that values have been restored
    verify(mAnimationBackend).setAlpha(alphaValue);
    verify(mAnimationBackend).setColorFilter(colorFilter);
    verify(mAnimationBackend).setBounds(bounds);
  }

  @Test
  public void testGetProperties() {
    int width = 123;
    int height = 234;
    int sizeInBytes = 2000;
    int frameCount = 20;
    int loopCount = 1000;
    int frameDurationMs = 200;

    when(mAnimationBackend.getIntrinsicWidth()).thenReturn(width);
    when(mAnimationBackend.getIntrinsicHeight()).thenReturn(height);
    when(mAnimationBackend.getSizeInBytes()).thenReturn(sizeInBytes);
    when(mAnimationBackend.getFrameCount()).thenReturn(frameCount);
    when(mAnimationBackend.getLoopCount()).thenReturn(loopCount);
    when(mAnimationBackend.getFrameDurationMs(anyInt())).thenReturn(frameDurationMs);

    assertThat(mAnimationBackendDelegate.getIntrinsicWidth()).isEqualTo(width);
    assertThat(mAnimationBackendDelegate.getIntrinsicHeight()).isEqualTo(height);
    assertThat(mAnimationBackendDelegate.getSizeInBytes()).isEqualTo(sizeInBytes);
    assertThat(mAnimationBackendDelegate.getFrameCount()).isEqualTo(frameCount);
    assertThat(mAnimationBackendDelegate.getLoopCount()).isEqualTo(loopCount);
    assertThat(mAnimationBackendDelegate.getFrameDurationMs(1)).isEqualTo(frameDurationMs);
  }

  @Test
  public void testGetDefaultProperties() {
    // We don't set an animation backend
    mAnimationBackendDelegate.setAnimationBackend(null);

    assertThat(mAnimationBackendDelegate.getIntrinsicWidth())
        .isEqualTo(AnimationBackend.INTRINSIC_DIMENSION_UNSET);
    assertThat(mAnimationBackendDelegate.getIntrinsicHeight())
        .isEqualTo(AnimationBackend.INTRINSIC_DIMENSION_UNSET);
    assertThat(mAnimationBackendDelegate.getSizeInBytes()).isEqualTo(0);
    assertThat(mAnimationBackendDelegate.getFrameCount()).isEqualTo(0);
    assertThat(mAnimationBackendDelegate.getLoopCount()).isEqualTo(0);
    assertThat(mAnimationBackendDelegate.getFrameDurationMs(1)).isEqualTo(0);
  }

  @Test
  public void testSetAnimationBackend() {
    AnimationBackend backend2 = mock(AnimationBackend.class);
    ColorFilter colorFilter = mock(ColorFilter.class);
    Rect bounds = mock(Rect.class);
    int alphaValue = 123;

    verifyZeroInteractions(backend2);

    // Set values to be persisted
    mAnimationBackendDelegate.setAlpha(alphaValue);
    mAnimationBackendDelegate.setColorFilter(colorFilter);
    mAnimationBackendDelegate.setBounds(bounds);

    mAnimationBackendDelegate.setAnimationBackend(backend2);

    // Verify that values have been restored
    verify(backend2).setAlpha(alphaValue);
    verify(backend2).setColorFilter(colorFilter);
    verify(backend2).setBounds(bounds);
  }

  @Test
  public void testDrawFrame() {
    mAnimationBackendDelegate.drawFrame(mParent, mCanvas, 1);

    verify(mAnimationBackend).drawFrame(mParent, mCanvas, 1);
  }

  @Test
  public void testClear() {
    mAnimationBackendDelegate.clear();

    verify(mAnimationBackend).clear();
  }
}
