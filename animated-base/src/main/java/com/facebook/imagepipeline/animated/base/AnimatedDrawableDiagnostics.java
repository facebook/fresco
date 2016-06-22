/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Diagnostics interface for {@link AnimatedDrawable}.
 */
public interface AnimatedDrawableDiagnostics {

  /**
   * Sets the backend that the {@link AnimatedDrawable} is using.
   *
   * @param animatedDrawableBackend the backend
   */
  void setBackend(AnimatedDrawableCachingBackend animatedDrawableBackend);

  /**
   * Called when the {@link AnimatedDrawable#onStart} method begins, which is the method that
   * resets and starts the animation.
   */
  void onStartMethodBegin();

  /**
   * Called when the {@link AnimatedDrawable#onStart} method ends.
   */
  void onStartMethodEnd();

  /**
   * Called when the {@link AnimatedDrawable#onNextFrame} method begins, which is the method that
   * determines the next frame to render and configures itself to do so.
   */
  void onNextFrameMethodBegin();

  /**
   * Called when the {@link AnimatedDrawable#onNextFrame} method ends.
   */
  void onNextFrameMethodEnd();

  /**
   * Increments the number of dropped frames for stats purposes.
   *
   * @param droppedFrames the number of dropped frames
   */
  void incrementDroppedFrames(int droppedFrames);

  /**
   * Increments the number of drawn frames for stats purposes.
   *
   * @param drawnFrames the number of drawn frames
   */
  void incrementDrawnFrames(int drawnFrames);

  /**
   * Called when the {@link AnimatedDrawable#draw} method begins.
   */
  void onDrawMethodBegin();

  /**
   * Called when the {@link AnimatedDrawable#draw} method emds.
   */
  void onDrawMethodEnd();

  /**
   * Allows the diagnostics code to draw an overlay that may be useful for debugging.
   *
   * @param canvas the canvas to draw to
   * @param destRect the rectangle bounds to draw to
   */
  void drawDebugOverlay(Canvas canvas, Rect destRect);
}
