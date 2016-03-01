/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import javax.annotation.Nullable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.facebook.common.references.CloseableReference;

/**
 * Interface that {@link AnimatedDrawable} uses that abstracts out the image format.
 */
public interface AnimatedDrawableBackend {

  /**
   * Gets the original result of the decode.
   *
   * @return the original result of the code
   */
  AnimatedImageResult getAnimatedImageResult();

  /**
   * Gets the duration of the animation.
   *
   * @return the duration of the animation in milliseconds
   */
  int getDurationMs();

  /**
   * Gets the number of frames in the animation.
   *
   * @return the number of frames in the animation
   */
  int getFrameCount();

  /**
   * Gets the number of loops to run the animation for.
   *
   * @return the number of loops, or 0 to indicate infinite
   */
  int getLoopCount();

  /**
   * Gets the width of the image.
   *
   * @return the width of the image
   */
  int getWidth();

  /**
   * Gets the height of the image.
   *
   * @return the height of the image
   */
  int getHeight();

  /**
   * Gets the rendered width of the image. This may be smaller than the underlying image width
   * if the image is being rendered to a small bounds or to reduce memory requirements.
   *
   * @return the rendered width of the image
   */
  int getRenderedWidth();

  /**
   * Gets the rendered height of the image. This may be smaller than the underlying image height
   * if the image is being rendered to a small bounds or to reduce memory requirements.
   *
   * @return the rendered height of the image
   */
  int getRenderedHeight();

  /**
   * Gets info about the specified frame.
   *
   * @param frameNumber the frame number (0-based)
   * @return the frame info
   */
  AnimatedDrawableFrameInfo getFrameInfo(int frameNumber);

  /**
   * Renders the specified frame onto the canvas.
   *
   * @param frameNumber the frame number (0-based)
   * @param canvas the canvas to render onto
   */
  void renderFrame(int frameNumber, Canvas canvas);

  /**
   * Gets the frame index for specified timestamp.
   *
   * @param timestampMs the timestamp
   * @return the frame index for the timestamp or the last frame number if the timestamp is outside
   *    the duration of the entire animation
   */
  int getFrameForTimestampMs(int timestampMs);

  /**
   * Gets the timestamp relative to the first frame that this frame number starts at.
   *
   * @param frameNumber the frame number
   * @return the time in milliseconds
   */
  int getTimestampMsForFrame(int frameNumber);

  /**
   * Gets the duration of the specified frame.
   *
   * @param frameNumber the frame number
   * @return the time in milliseconds
   */
  int getDurationMsForFrame(int frameNumber);

  /**
   * Gets the frame number to use for the preview frame.
   *
   * @return the frame number to use for the preview frame
   */
  int getFrameForPreview();

  /**
   * Creates a new {@link AnimatedDrawableBackend} with the same parameters but with a new bounds.
   *
   * @param bounds the bounds
   * @return an {@link AnimatedDrawableBackend} with the new bounds (this may be the same instance
   *    if the bounds don't require a new backend)
   */
  AnimatedDrawableBackend forNewBounds(Rect bounds);

  /**
   * Gets the number of bytes currently used by the backend for caching (for debugging)
   *
   * @return the number of bytes currently used by the backend for caching
   */
  int getMemoryUsage();

  /**
   * Gets a pre-decoded frame. This will only return non-null if the {@code ImageDecodeOptions}
   * were configured to decode all frames at decode time.
   *
   * @param frameNumber the index of the frame to get
   * @return a reference to the preview bitmap which must be released by the caller when done or
   *     null if there is no preview bitmap set
   */
  @Nullable CloseableReference<Bitmap> getPreDecodedFrame(int frameNumber);

  /**
   * Gets whether it has the decoded frame. This will only return true if the
   * {@code ImageDecodeOptions} were configured to decode all frames at decode time.
   *
   * @param frameNumber the index of the frame to get
   * @return true if the result has the decoded frame
   */
  boolean hasPreDecodedFrame(int frameNumber);

    /**
     * Instructs the backend to drop its caches.
     */
  void dropCaches();
}
