/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.base;

import android.graphics.Bitmap;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Common interface for an animated image. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface AnimatedImage {

  int LOOP_COUNT_INFINITE = 0;

  /**
   * Disposes the instance. This will free native resources held by this instance. Once called,
   * other methods on this instance may throw. Note, the underlying native resources may not
   * actually be freed until all associated instances of {@link AnimatedImageFrame} are disposed or
   * finalized as well.
   */
  void dispose();

  /**
   * Gets the width of the image (also known as the canvas in WebP nomenclature).
   *
   * @return the width of the image
   */
  int getWidth();

  /**
   * Gets the height of the image (also known as the canvas in WebP nomenclature).
   *
   * @return the height of the image
   */
  int getHeight();

  /**
   * Gets the number of frames in the image.
   *
   * @return the number of frames in the image
   */
  int getFrameCount();

  /**
   * Gets the duration of the animated image.
   *
   * @return the duration of the animated image in milliseconds
   */
  int getDuration();

  /**
   * Gets the duration of each frame of the animated image.
   *
   * @return an array that is the size of the number of frames containing the duration of each frame
   *     in milliseconds
   */
  int[] getFrameDurations();

  /**
   * Gets the number of loops to run the animation for.
   *
   * @return the number of loops, or 0 to indicate infinite
   */
  int getLoopCount();

  /**
   * Creates an {@link AnimatedImageFrame} at the specified index.
   *
   * @param frameNumber the index of the frame
   * @return a newly created {@link AnimatedImageFrame}
   */
  AnimatedImageFrame getFrame(int frameNumber);

  /**
   * Returns whether {@link AnimatedImageFrame#renderFrame} supports scaling to arbitrary sizes or
   * whether scaling must be done externally.
   *
   * @return whether rendering supports scaling
   */
  boolean doesRenderSupportScaling();

  /**
   * Gets the size of bytes of the encoded image data (which is the data kept in memory for the
   * image).
   *
   * @return the size in bytes of the encoded image data
   */
  int getSizeInBytes();

  /**
   * Gets the frame info for the specified frame.
   *
   * @param frameNumber the frame to get the info for
   * @return the frame info
   */
  AnimatedDrawableFrameInfo getFrameInfo(int frameNumber);

  /**
   * Gets the Bitmap.Config to decode the Bitmap of Animated Frames.
   *
   * @return Bitmap.Config for Animated Image
   */
  @Nullable
  Bitmap.Config getAnimatedBitmapConfig();
}
