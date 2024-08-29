/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;

/** A drawable that is capable of cloning itself. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface CloneableDrawable {

  /**
   * Creates a copy of the drawable.
   *
   * @return the drawable copy
   */
  @Nullable
  Drawable cloneDrawable();
}
