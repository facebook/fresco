/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.bitmap.wrapper;

import javax.annotation.Nullable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;

/**
 * {@link BitmapFrameRenderer} that wraps around an {@link AnimatedDrawableBackend}.
 */
public class AnimatedDrawableBackendFrameRenderer implements BitmapFrameRenderer {

  private AnimatedDrawableBackend mAnimatedDrawableBackend;

  public AnimatedDrawableBackendFrameRenderer(AnimatedDrawableBackend animatedDrawableBackend) {
    mAnimatedDrawableBackend = animatedDrawableBackend;
  }

  @Override
  public void setBounds(@Nullable Rect bounds) {
    mAnimatedDrawableBackend = mAnimatedDrawableBackend.forNewBounds(bounds);
  }

  @Override
  public int getIntrinsicWidth() {
    return mAnimatedDrawableBackend.getWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    return mAnimatedDrawableBackend.getHeight();
  }

  @Override
  public boolean renderFrame(int frameNumber, Bitmap targetBitmap) {
    Canvas canvas = new Canvas(targetBitmap);
    mAnimatedDrawableBackend.renderFrame(frameNumber, canvas);
    return true;
  }
}
