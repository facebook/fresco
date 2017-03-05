/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.bitmap.cache;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import android.graphics.Bitmap;

import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.imageutils.BitmapUtil;

/**
 * Simple bitmap cache that keeps the last frame and reuses it if possible.
 */
public class KeepLastFrameCache implements BitmapFrameCache {

  private static final int FRAME_NUMBER_UNSET = -1;

  private int mLastFrameNumber = FRAME_NUMBER_UNSET;

  @Nullable
  private FrameCacheListener mFrameCacheListener;

  @GuardedBy("this")
  @Nullable
  private CloseableReference<Bitmap> mLastBitmapReference;

  @Nullable
  @Override
  public synchronized CloseableReference<Bitmap> getCachedFrame(int frameNumber) {
    if (mLastFrameNumber == frameNumber) {
      return CloseableReference.cloneOrNull(mLastBitmapReference);
    }
    return null;
  }

  @Nullable
  @Override
  public synchronized CloseableReference<Bitmap> getFallbackFrame(int frameNumber) {
    return CloseableReference.cloneOrNull(mLastBitmapReference);
  }

  @Override
  public synchronized CloseableReference<Bitmap> getBitmapToReuseForFrame(
      int frameNumber,
      int width,
      int height) {
    try {
      return CloseableReference.cloneOrNull(mLastBitmapReference);
    } finally {
      closeAndResetLastBitmapReference();
    }
  }

  @Override
  public synchronized int getSizeInBytes() {
    return mLastBitmapReference == null
        ? 0
        : BitmapUtil.getSizeInBytes(mLastBitmapReference.get());
  }

  @Override
  public synchronized void clear() {
    closeAndResetLastBitmapReference();
  }

  @Override
  public synchronized void onFrameRendered(
      int frameNumber,
      CloseableReference<Bitmap> bitmap,
      @BitmapAnimationBackend.FrameType int frameType) {
    if (bitmap != null
        && mLastBitmapReference != null
        && bitmap.get().equals(mLastBitmapReference.get())) {
      return;
    }
    CloseableReference.closeSafely(mLastBitmapReference);
    if (mFrameCacheListener != null && mLastFrameNumber != FRAME_NUMBER_UNSET) {
      mFrameCacheListener.onFrameEvicted(this, mLastFrameNumber);
    }
    mLastBitmapReference = CloseableReference.cloneOrNull(bitmap);
    if (mFrameCacheListener != null) {
      mFrameCacheListener.onFrameCached(this, frameNumber);
    }
    mLastFrameNumber = frameNumber;
  }

  @Override
  public void setFrameCacheListener(FrameCacheListener frameCacheListener) {
    mFrameCacheListener = frameCacheListener;
  }

  private synchronized void closeAndResetLastBitmapReference() {
    if (mFrameCacheListener != null && mLastFrameNumber != FRAME_NUMBER_UNSET) {
      mFrameCacheListener.onFrameEvicted(this, mLastFrameNumber);
    }
    CloseableReference.closeSafely(mLastBitmapReference);
    mLastBitmapReference = null;
    mLastFrameNumber = FRAME_NUMBER_UNSET;
  }
}
