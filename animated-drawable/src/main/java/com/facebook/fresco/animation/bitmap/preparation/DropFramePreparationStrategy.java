/*
 * Copyright (c) 2017-present, Facebook, Inc.
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
public class DropFramePreparationStrategy
    implements BitmapFramePreparationStrategy {

  private static final Class<?> TAG = DropFramePreparationStrategy.class;

  @Override
  public void prepareFrames(
      BitmapFramePreparer bitmapFramePreparer,
      BitmapFrameCache bitmapFrameCache,
      AnimationBackend animationBackend,
      int lastDrawnFrameNumber) {
      int nextFrameNumber = lastDrawnFrameNumber % animationBackend.getFrameCount();
      if (FLog.isLoggable(FLog.VERBOSE)) {
        FLog.v(TAG, "Preparing frame %d, last drawn: %d", nextFrameNumber, lastDrawnFrameNumber);
      }
      bitmapFramePreparer.prepareFrame(
          bitmapFrameCache,
          animationBackend,
          nextFrameNumber);
  }
}
