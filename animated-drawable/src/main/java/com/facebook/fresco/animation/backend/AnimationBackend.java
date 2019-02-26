/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.backend;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.IntRange;
import javax.annotation.Nullable;

/**
 * Animation backend interface that is used to draw frames.
 */
public interface AnimationBackend extends AnimationInformation {

  /**
   * Default value if the intrinsic dimensions are not set.
   *
   * @see #getIntrinsicWidth()
   * @see #getIntrinsicHeight()
   */
  int INTRINSIC_DIMENSION_UNSET = -1;

  /**
   * Draw the frame for the given frame number on the canvas.
   *
   * @param parent the parent that draws the frame
   * @param canvas the canvas to draw an
   * @param frameNumber the frame number of the frame to draw
   * @return true if successful, false if the frame could not be rendered
   */
  boolean drawFrame(Drawable parent, Canvas canvas, int frameNumber);

  /**
   * Set the alpha value to be used for drawing frames in {@link #drawFrame(Drawable, Canvas, int)}
   * if supported.
   *
   * @param alpha the alpha value between 0 and 255
   */
  void setAlpha(@IntRange(from = 0, to = 255) int alpha);

  /**
   * The color filter to be used for drawing frames in {@link #drawFrame(Drawable, Canvas, int)}
   * if supported.
   *
   * @param colorFilter the color filter to use
   */
  void setColorFilter(@Nullable ColorFilter colorFilter);

  /**
   * Called when the bounds of the parent drawable are updated.
   * This can be used to perform some ahead-of-time computations if needed.
   *
   * The supplied bounds do not have to be stored. It is possible to just use
   * {@link Drawable#getBounds()} of the parent drawable of
   * {@link #drawFrame(Drawable, Canvas, int)} instead.
   *
   * @param bounds the bounds to be used for drawing frames
   */
  void setBounds(Rect bounds);

  /**
   * Get the intrinsic width of the underlying animation or
   * {@link #INTRINSIC_DIMENSION_UNSET} if not available.
   *
   * This value is used by the underlying drawable for aspect ratio computations,
   * similar to {@link Drawable#getIntrinsicWidth()}.
   *
   * @return the width or {@link #INTRINSIC_DIMENSION_UNSET} if unset
   */
  int getIntrinsicWidth();

  /**
   * Get the intrinsic height of the underlying animation or
   * {@link #INTRINSIC_DIMENSION_UNSET} if not available.
   *
   * This value is used by the underlying drawable for aspect ratio computations,
   * similar to {@link Drawable#getIntrinsicHeight()}.
   *
   * @return the height or {@link #INTRINSIC_DIMENSION_UNSET} if unset
   */
  int getIntrinsicHeight();

  /**
   * Get the size of the animation backend.
   *
   * @return the size in bytes
   */
  int getSizeInBytes();

  /**
   * Clean up animation data. This will be called when the backing drawable is cleared as well.
   * For example, drop all cached frames.
   */
  void clear();
}
