/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.bitmap.wrapper;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link AnimatedDrawableBackendFrameRenderer}
 */
@RunWith(RobolectricTestRunner.class)
public class AnimatedDrawableBackendFrameRendererTest {

  private AnimatedDrawableBackendFrameRenderer mAnimatedDrawableBackendFrameRenderer;
  private AnimatedDrawableBackend mAnimatedDrawableBackend;
  private BitmapFrameCache mBitmapFrameCache;

  @Before
  public void setup() {
    mAnimatedDrawableBackend = mock(AnimatedDrawableBackend.class);
    mBitmapFrameCache = mock(BitmapFrameCache.class);
    mAnimatedDrawableBackendFrameRenderer = new AnimatedDrawableBackendFrameRenderer(
        mBitmapFrameCache,
        mAnimatedDrawableBackend);
  }

  @Test
  public void testSetBounds() {
    when(mAnimatedDrawableBackend.forNewBounds(any(Rect.class)))
        .thenReturn(mAnimatedDrawableBackend);

    Rect bounds = mock(Rect.class);
    mAnimatedDrawableBackendFrameRenderer.setBounds(bounds);

    verify(mAnimatedDrawableBackend).forNewBounds(bounds);
  }

  @Test
  public void testGetIntrinsicWidth() {
    when(mAnimatedDrawableBackend.getWidth())
        .thenReturn(123);

    assertThat(mAnimatedDrawableBackendFrameRenderer.getIntrinsicWidth()).isEqualTo(123);
    assertThat(mAnimatedDrawableBackendFrameRenderer.getIntrinsicHeight()).isNotEqualTo(123);
  }

  @Test
  public void testGetIntrinsicHeight() {
    when(mAnimatedDrawableBackend.getHeight())
        .thenReturn(1200);

    assertThat(mAnimatedDrawableBackendFrameRenderer.getIntrinsicHeight()).isEqualTo(1200);
    assertThat(mAnimatedDrawableBackendFrameRenderer.getIntrinsicWidth()).isNotEqualTo(1200);
  }

  @Test
  public void testRenderFrame() {
    when(mAnimatedDrawableBackend.getHeight())
        .thenReturn(1200);
    Bitmap bitmap = mock(Bitmap.class);
    AnimatedDrawableFrameInfo animatedDrawableFrameInfo = mock(AnimatedDrawableFrameInfo.class);
    when(mAnimatedDrawableBackend.getFrameInfo(anyInt())).thenReturn(animatedDrawableFrameInfo);

    boolean rendered = mAnimatedDrawableBackendFrameRenderer.renderFrame(0, bitmap);

    assertThat(rendered).isTrue();
  }
}
