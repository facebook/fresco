/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap.preparation;

import com.facebook.common.logging.FLog;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;

/**
 * Frame preparation strategy to prepare the next n frames
 */
public class FixedNumberBitmapFramePreparationStrategy
    implements BitmapFramePreparationStrategy {

  private static final Class<?> TAG = FixedNumberBitmapFramePreparationStrategy.class;
  private static final int DEFAULT_FRAMES_TO_PREPARE = 3;

  private final int mFramesToPrepare;

  public FixedNumberBitmapFramePreparationStrategy() {
    this(DEFAULT_FRAMES_TO_PREPARE);
  }

  public FixedNumberBitmapFramePreparationStrategy(int framesToPrepare) {
    mFramesToPrepare = framesToPrepare;
  }

  @Override
  public void prepareFrames(
      BitmapFramePreparer bitmapFramePreparer,
      BitmapFrameCache bitmapFrameCache,
      AnimationBackend animationBackend,
      int lastDrawnFrameNumber) {
    for (int i = 1; i <= mFramesToPrepare; i++) {
      int nextFrameNumber = (lastDrawnFrameNumber + i) % animationBackend.getFrameCount();
      if (FLog.isLoggable(FLog.VERBOSE)) {
        FLog.v(TAG, "Preparing frame %d, last drawn: %d", nextFrameNumber, lastDrawnFrameNumber);
      }
      if (!bitmapFramePreparer.prepareFrame(
          bitmapFrameCache,
          animationBackend,
          nextFrameNumber)) {
        // We cannot prepare more frames, so we return early
        return;
      }
    }
  }
}
