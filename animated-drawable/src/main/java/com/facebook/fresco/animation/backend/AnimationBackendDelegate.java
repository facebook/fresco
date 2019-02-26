/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.backend;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.IntRange;
import javax.annotation.Nullable;

/**
 * Animation backend delegate that forwards all calls to a given {@link AnimationBackend}
 */
public class AnimationBackendDelegate<T extends AnimationBackend> implements AnimationBackend {

  private static final int ALPHA_UNSET = -1;

  /**
   * Current animation backend in use
   */
  @Nullable
  private T mAnimationBackend;

  // Animation backend parameters
  @IntRange(from = -1, to = 255)
  private int mAlpha = ALPHA_UNSET;
  @Nullable
  private ColorFilter mColorFilter;
  @Nullable
  private Rect mBounds;

  public AnimationBackendDelegate(@Nullable T animationBackend) {
    mAnimationBackend = animationBackend;
  }

  @Override
  public int getFrameCount() {
    return mAnimationBackend == null ? 0 : mAnimationBackend.getFrameCount();
  }

  @Override
  public int getFrameDurationMs(int frameNumber) {
    return mAnimationBackend == null ? 0 : mAnimationBackend.getFrameDurationMs(frameNumber);
  }

  @Override
  public int getLoopCount() {
    return mAnimationBackend == null ? LOOP_COUNT_INFINITE : mAnimationBackend.getLoopCount();
  }

  @Override
  public boolean drawFrame(Drawable parent, Canvas canvas, int frameNumber) {
    return mAnimationBackend != null && mAnimationBackend.drawFrame(parent, canvas, frameNumber);
  }

  @Override
  public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
    if (mAnimationBackend != null) {
      mAnimationBackend.setAlpha(alpha);
    }
    mAlpha = alpha;
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    if (mAnimationBackend != null) {
      mAnimationBackend.setColorFilter(colorFilter);
    }
    mColorFilter = colorFilter;
  }

  @Override
  public void setBounds(@Nullable Rect bounds) {
    if (mAnimationBackend != null) {
      mAnimationBackend.setBounds(bounds);
    }
    mBounds = bounds;
  }

  @Override
  public int getSizeInBytes() {
    return mAnimationBackend == null ? 0 : mAnimationBackend.getSizeInBytes();
  }

  @Override
  public void clear() {
    if (mAnimationBackend != null) {
      mAnimationBackend.clear();
    }
  }

  @Override
  public int getIntrinsicWidth() {
    return mAnimationBackend == null
        ? INTRINSIC_DIMENSION_UNSET
        : mAnimationBackend.getIntrinsicWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    return mAnimationBackend == null
        ? INTRINSIC_DIMENSION_UNSET
        : mAnimationBackend.getIntrinsicHeight();
  }

  /**
   * Set the animation backend to forward calls to.
   * If called with null, the current backend will be removed.
   *
   * @param animationBackend the backend to use or null to remove the current backend
   */
  public void setAnimationBackend(@Nullable T animationBackend) {
    mAnimationBackend = animationBackend;
    if (mAnimationBackend != null) {
      applyBackendProperties(mAnimationBackend);
    }
  }

  /**
   * Get the current animation backend.
   *
   * @return the current animation backend in use or null if not set
   */
  @Nullable
  public T getAnimationBackend() {
    return mAnimationBackend;
  }

  @SuppressLint("Range")
  private void applyBackendProperties(AnimationBackend backend) {
    if (mBounds != null) {
      backend.setBounds(mBounds);
    }
    if (mAlpha >= 0 && mAlpha <= 255) {
      backend.setAlpha(mAlpha);
    }
    if (mColorFilter != null) {
      backend.setColorFilter(mColorFilter);
    }
  }
}
