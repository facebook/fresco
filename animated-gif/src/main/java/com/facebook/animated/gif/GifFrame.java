/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.gif;

import android.graphics.Bitmap;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A single frame of a {@link GifImage}.
 */
@ThreadSafe
public class GifFrame implements AnimatedImageFrame {

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
  GifFrame(long nativeContext) {
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

  public boolean hasTransparency() {
    return nativeHasTransparency();
  }

  public int getTransparentPixelColor() {
    return nativeGetTransparentPixelColor();
  }

  public int getDisposalMode() {
    return nativeGetDisposalMode();
  }

  @DoNotStrip
  private native void nativeRenderFrame(int width, int height, Bitmap bitmap);

  @DoNotStrip
  private native int nativeGetDurationMs();

  @DoNotStrip
  private native int nativeGetWidth();

  @DoNotStrip
  private native int nativeGetHeight();

  @DoNotStrip
  private native int nativeGetXOffset();

  @DoNotStrip
  private native int nativeGetYOffset();

  @DoNotStrip
  private native int nativeGetDisposalMode();

  @DoNotStrip
  private native int nativeGetTransparentPixelColor();

  @DoNotStrip
  private native boolean nativeHasTransparency();

  @DoNotStrip
  private native void nativeDispose();

  @DoNotStrip
  private native void nativeFinalize();
}
