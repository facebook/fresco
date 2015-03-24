/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
  public void getTransform(Matrix transform);

  /**
   * Called when the drawable needs to get its root bounds.
   *
   * @param bounds The root bounds of the drawable.
   */
  public void getRootBounds(RectF bounds);
}
