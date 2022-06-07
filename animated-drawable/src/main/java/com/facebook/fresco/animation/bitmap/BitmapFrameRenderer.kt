/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Bitmap frame renderer used by [BitmapAnimationBackend] to render animated images (e.g. GIFs or
 * animated WebPs).
 */
interface BitmapFrameRenderer {

  /**
   * Render the frame for the given frame number to the target bitmap.
   *
   * @param frameNumber the frame number to render
   * @param targetBitmap the bitmap to render the frame in
   * @return true if successful
   */
  fun renderFrame(frameNumber: Int, targetBitmap: Bitmap): Boolean

  /**
   * Set the parent drawable bounds to be used for frame rendering.
   *
   * @param bounds the bounds to use
   */
  fun setBounds(bounds: Rect?)

  /**
   * Return the intrinsic width of bitmap frames. Return
   * [AnimationBackend#INTRINSIC_DIMENSION_UNSET] if no specific width is set.
   *
   * @return the intrinsic width
   */
  val intrinsicWidth: Int

  /**
   * Return the intrinsic height of bitmap frames. Return
   * [AnimationBackend#INTRINSIC_DIMENSION_UNSET] if no specific height is set.
   *
   * @return the intrinsic height
   */
  val intrinsicHeight: Int
}
