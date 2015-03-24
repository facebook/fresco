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

import com.facebook.common.internal.Preconditions;

/**
 * Settable drawable that forwards to the underlying drawable. Drawable can be set multiple times.
 */
public class SettableDrawable extends ForwardingDrawable {

  /**
   * Creates a new settable drawable.
   * @param drawable underlying drawable
   */
  public SettableDrawable(Drawable drawable) {
    super(Preconditions.checkNotNull(drawable));
  }

  /**
   * Sets the new drawable. It is allowed to set drawable multiple times.
   * @param newDrawable a new drawable to set
   */
  public void setDrawable(Drawable newDrawable) {
    Preconditions.checkNotNull(newDrawable);
    setCurrent(newDrawable);
  }

  /**
   * Gets the current drawable.
   * @return the current drawable
   */
  public Drawable getDrawable() {
    return getCurrent();
  }

}
