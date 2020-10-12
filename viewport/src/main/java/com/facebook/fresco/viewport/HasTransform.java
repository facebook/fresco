// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.viewport;

import android.graphics.Matrix;
import android.graphics.Rect;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public interface HasTransform {

  /**
   * Gets transformation matrix based on the scale type.
   *
   * @param outTransform out matrix to store result
   * @param parentBounds parent bounds
   * @param childWidth child width
   * @param childHeight child height
   * @param focusX focus point x coordinate, relative [0...1]
   * @param focusY focus point y coordinate, relative [0...1]
   * @return same reference to the out matrix for convenience
   */
  Matrix getTransform(
      Matrix outTransform,
      Rect parentBounds,
      int childWidth,
      int childHeight,
      float focusX,
      float focusY);
}
