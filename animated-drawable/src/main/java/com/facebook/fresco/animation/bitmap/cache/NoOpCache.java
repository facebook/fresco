/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.bitmap.cache;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import javax.annotation.Nullable;

/**
 * No-op bitmap cache that doesn't do anything.
 */
public class NoOpCache implements BitmapFrameCache {

  @Nullable
  @Override
  public CloseableReference<Bitmap> getCachedFrame(int frameNumber) {
    return null;
  }

  @Nullable
  @Override
  public CloseableReference<Bitmap> getFallbackFrame(int frameNumber) {
    return null;
  }

  @Nullable
  @Override
  public CloseableReference<Bitmap> getBitmapToReuseForFrame(
      int frameNumber,
      int width,
      int height) {
    return null;
  }

  @Override
  public boolean contains(int frameNumber) {
    return false;
  }

  @Override
  public int getSizeInBytes() {
    return 0;
  }

  @Override
  public void clear() {
    // no-op
  }

  @Override
  public void onFrameRendered(
      int frameNumber,
      CloseableReference<Bitmap> bitmapReference,
      @BitmapAnimationBackend.FrameType int frameType) {
    // no-op
  }

  @Override
  public void onFramePrepared(
      int frameNumber,
      CloseableReference<Bitmap> bitmapReference,
      @BitmapAnimationBackend.FrameType int frameType) {
    // Does not cache anything
  }

  @Override
  public void setFrameCacheListener(FrameCacheListener frameCacheListener) {
    // Does not cache anything
  }
}
