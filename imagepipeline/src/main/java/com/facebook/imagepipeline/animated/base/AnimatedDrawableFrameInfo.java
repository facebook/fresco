/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

/**
 * Info per frame returned by {@link AnimatedDrawableBackend}.
 */
public class AnimatedDrawableFrameInfo {

  /**
   * How to dispose of the current frame before rendering the next frame.
   */
  public enum DisposalMethod {

    /** Do not dipose the frame. Leave as-is. */
    DISPOSE_DO_NOT,

    /** Dispose to the background color */
    DISPOSE_TO_BACKGROUND,

    /** Dispose to the previous frame */
    DISPOSE_TO_PREVIOUS
  }

  public final int frameNumber;
  public final int xOffset;
  public final int yOffset;
  public final int width;
  public final int height;
  public final boolean shouldBlendWithPreviousFrame;
  public final DisposalMethod disposalMethod;

  public AnimatedDrawableFrameInfo(
      int frameNumber,
      int xOffset,
      int yOffset,
      int width,
      int height,
      boolean shouldBlendWithPreviousFrame,
      DisposalMethod disposalMethod) {
    this.frameNumber = frameNumber;
    this.xOffset = xOffset;
    this.yOffset = yOffset;
    this.width = width;
    this.height = height;
    this.shouldBlendWithPreviousFrame = shouldBlendWithPreviousFrame;
    this.disposalMethod = disposalMethod;
  }
}
