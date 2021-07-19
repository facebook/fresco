/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.graphics.drawable.Drawable;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** A drawable parent that has a single child. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface DrawableParent {

  /**
   * Sets the new child drawable.
   *
   * @param newDrawable a new child drawable to set
   * @return the old child drawable
   */
  @Nullable
  Drawable setDrawable(@Nullable Drawable newDrawable);

  /**
   * Gets the child drawable.
   *
   * @return the current child drawable
   */
  @Nullable
  Drawable getDrawable();
}
