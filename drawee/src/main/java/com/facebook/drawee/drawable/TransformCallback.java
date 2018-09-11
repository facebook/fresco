/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.graphics.Matrix;
import android.graphics.RectF;

/**
 * Callback that is used to pass any transformation matrix and the root bounds from a parent
 * drawable to its child.
 */
public interface TransformCallback {

  /**
   * Called when the drawable needs to get all matrices applied to it.
   *
   * @param transform Matrix that is applied to the drawable by the parent drawables.
   */
  void getTransform(Matrix transform);

  /**
   * Called when the drawable needs to get its root bounds.
   *
   * @param bounds The root bounds of the drawable.
   */
  void getRootBounds(RectF bounds);
}
