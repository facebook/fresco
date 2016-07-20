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
 * A drawable that is capable of cloning itself.
 */
public interface CloneableDrawable {

  /**
   * Creates a copy of the drawable.
   * @return the drawable copy
   */
  Drawable cloneDrawable();
}
