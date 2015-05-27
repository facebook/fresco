/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.webp;

import javax.annotation.concurrent.ThreadSafe;

import android.graphics.Bitmap;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;

/**
 * A single frame of a {@link WebPImage}.
 */
@ThreadSafe
public class WebPFrame implements AnimatedImageFrame {

  // Accessed by native methods
  @SuppressWarnings("unused")
  @DoNotStrip
  private long mNativeContext;

  /**
   * Constructs the frame with the native pointer. This is called by native code.
   *
   * @param nativeContext the native pointer
   */
  @DoNotStrip
  WebPFrame(long nativeContext) {
    mNativeContext = nativeContext;
  }

  @Override
  protected void finalize() {
    nativeFinalize();
  }

  @Override
  public void dispose() {
    nativeDispose();
  }

  @Override
  public void renderFrame(int width, int height, Bitmap bitmap) {
    nativeRenderFrame(width, height, bitmap);
  }

  @Override
  public int getDurationMs() {
    return nativeGetDurationMs();
  }

  @Override
  public int getWidth() {
    return nativeGetWidth();
  }

  @Override
  public int getHeight() {
    return nativeGetHeight();
  }

  @Override
  public int getXOffset() {
    return nativeGetXOffset();
  }

  @Override
  public int getYOffset() {
    return nativeGetYOffset();
  }

  public boolean shouldDisposeToBackgroundColor() {
    return nativeShouldDisposeToBackgroundColor();
  }

  public boolean shouldBlendWithPreviousFrame() {
    return nativeShouldBlendWithPreviousFrame();
  }

  private native void nativeRenderFrame(int width, int height, Bitmap bitmap);
  private native int nativeGetDurationMs();
  private native int nativeGetWidth();
  private native int nativeGetHeight();
  private native int nativeGetXOffset();
  private native int nativeGetYOffset();
  private native boolean nativeShouldDisposeToBackgroundColor();
  private native boolean nativeShouldBlendWithPreviousFrame();
  private native void nativeDispose();
  private native void nativeFinalize();
}
