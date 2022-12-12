/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer

import android.graphics.Matrix
import android.graphics.Rect

/** Canvas transformation */
interface CanvasTransformation {

  /**
   * Calculate a transformation based on the given parent bounds and child dimensions. The given
   * outTransform should be used and returned. If no transformation should be applied, null can be
   * returned.
   *
   * @param outTransform the Matrix to re-use
   * @param parentBounds the bounds to fill
   * @param childWidth the width of the child to draw
   * @param childHeight the height of the child to draw
   * @return a Matrix (usually outTransform) or null if no transformation required
   */
  fun calculateTransformation(
      outTransform: Matrix,
      parentBounds: Rect,
      childWidth: Int,
      childHeight: Int
  ): Matrix?
}
