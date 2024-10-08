/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.drawable.Drawable

/** A drawable parent that has a single child. */
interface DrawableParent {

  /**
   * Sets the new child drawable.
   *
   * @param newDrawable a new child drawable to set
   * @return the old child drawable
   */
  fun setDrawable(newDrawable: Drawable?): Drawable?

  /**
   * Gets the child drawable.
   *
   * @return the current child drawable
   */
  val drawable: Drawable?
}
