/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.drawable.Drawable;

/**
 * A drawable parent that has a single child.
 */
public interface DrawableParent {

  /**
   * Sets the new child drawable.
   * @param newDrawable a new child drawable to set
   * @return the old child drawable
   */
  Drawable setDrawable(Drawable newDrawable);

  /**
   * Gets the child drawable.
   * @return the current child drawable
   */
  Drawable getDrawable();

}
