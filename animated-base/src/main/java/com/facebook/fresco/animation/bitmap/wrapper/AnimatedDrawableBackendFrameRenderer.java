/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap.wrapper;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor;
import javax.annotation.Nullable;

/**
 * {@link BitmapFrameRenderer} that wraps around an {@link AnimatedDrawableBackend}.
 */
public class AnimatedDrawableBackendFrameRenderer implements BitmapFrameRenderer {

  private static final Class<?> TAG = AnimatedDrawableBackendFrameRenderer.class;

  private final BitmapFrameCache mBitmapFrameCache;

  private AnimatedDrawableBackend mAnimatedDrawableBackend;
  private AnimatedImageCompositor mAnimatedImageCompositor;

  private final AnimatedImageCompositor.Callback mCallback =
      new AnimatedImageCompositor.Callback() {
        @Override
        public void onIntermediateResult(int frameNumber, Bitmap bitmap) {
          // We currently don't cache intermediate bitmaps here
        }

        @Nullable
        @Override
        public CloseableReference<Bitmap> getCachedBitmap(int frameNumber) {
          return mBitmapFrameCache.getCachedFrame(frameNumber);
        }
      };

  public AnimatedDrawableBackendFrameRenderer(
      BitmapFrameCache bitmapFrameCache,
      AnimatedDrawableBackend animatedDrawableBackend) {
    mBitmapFrameCache = bitmapFrameCache;
    mAnimatedDrawableBackend = animatedDrawableBackend;

    mAnimatedImageCompositor = new AnimatedImageCompositor(mAnimatedDrawableBackend, mCallback);
  }

  @Override
  public void setBounds(@Nullable Rect bounds) {
    AnimatedDrawableBackend newBackend = mAnimatedDrawableBackend.forNewBounds(bounds);
    if (newBackend != mAnimatedDrawableBackend) {
      mAnimatedDrawableBackend = newBackend;
      mAnimatedImageCompositor = new AnimatedImageCompositor(mAnimatedDrawableBackend, mCallback);
    }
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
    try {
      mAnimatedImageCompositor.renderFrame(frameNumber, targetBitmap);
    } catch (IllegalStateException exception) {
      FLog.e(TAG, exception, "Rendering of frame unsuccessful. Frame number: %d", frameNumber);
      return false;
    }
    return true;
  }
}