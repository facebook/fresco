/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.interfaces;

import android.graphics.drawable.Drawable;

/**
 * Interface that represents a Drawee hierarchy.
 * <p> A hierarchy assembles a tree of Drawables in order to form a dynamically changeable display.
 * This is much more lightweight than the traditional Android way of nesting View objects.
 * <p> Hierarchy details are hidden for the outside world. All that's visible is the top level
 * drawable, which can be put into a view.
 * <p> Example hierarchy:
 *
 *   o FadeDrawable (top level drawable)
 *   |
 *   +--o ScaleTypeDrawable
 *   |  |
 *   |  +--o BitmapDrawable
 *   |
 *   +--o ScaleTypeDrawable
 *      |
 *      +--o BitmapDrawable
 *
 */
public interface DraweeHierarchy {

  /**
   * Returns the top level drawable in the corresponding hierarchy. Hierarchy should always have
   * the same instance of its top level drawable.
   * @return top level drawable
   */
  public Drawable getTopLevelDrawable();
}
