/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.interfaces;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Interface that represents a Drawee hierarchy.
 *
 * <p>A hierarchy assembles a tree of Drawables in order to form a dynamically changeable display.
 * This is much more lightweight than the traditional Android way of nesting View objects.
 *
 * <p>Hierarchy details are hidden for the outside world. All that's visible is the top level
 * drawable, which can be put into a view.
 *
 * <p>Example hierarchy:
 *
 * <pre>
 *   o FadeDrawable (top level drawable)
 *   |
 *   +--o ScaleTypeDrawable
 *   |  |
 *   |  +--o BitmapDrawable
 *   |
 *   +--o ScaleTypeDrawable
 *      |
 *      +--o BitmapDrawable
 * </pre>
 */
@ThreadSafe
public interface DraweeHierarchy {

  /**
   * Returns the top level drawable in the corresponding hierarchy. Hierarchy should always have the
   * same instance of its top level drawable.
   *
   * @return top level drawable
   */
  Drawable getTopLevelDrawable();

  /** @return bounds of the top drawable */
  Rect getBounds();
}
