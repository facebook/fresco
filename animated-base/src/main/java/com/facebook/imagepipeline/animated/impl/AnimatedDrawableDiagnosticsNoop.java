/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.facebook.imagepipeline.animated.base.AnimatedDrawableCachingBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableDiagnostics;

/**
 * Implementation of {@link AnimatedDrawableDiagnostics} that does nothing.
 */
public class AnimatedDrawableDiagnosticsNoop implements AnimatedDrawableDiagnostics {

  private static AnimatedDrawableDiagnosticsNoop sInstance = new AnimatedDrawableDiagnosticsNoop();

  public static AnimatedDrawableDiagnosticsNoop getInstance() {
    return sInstance;
  }

  @Override
  public void setBackend(AnimatedDrawableCachingBackend animatedDrawableBackend) {
  }

  @Override
  public void onStartMethodBegin() {
  }

  @Override
  public void onStartMethodEnd() {
  }

  @Override
  public void onNextFrameMethodBegin() {
  }

  @Override
  public void onNextFrameMethodEnd() {
  }

  @Override
  public void incrementDroppedFrames(int droppedFrames) {
  }

  @Override
  public void incrementDrawnFrames(int drawnFrames) {
  }

  @Override
  public void onDrawMethodBegin() {
  }

  @Override
  public void onDrawMethodEnd() {
  }

  @Override
  public void drawDebugOverlay(Canvas canvas, Rect destRect) {
  }
}
