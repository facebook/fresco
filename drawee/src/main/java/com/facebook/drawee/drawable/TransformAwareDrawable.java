/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;

/** Interface that enables setting a transform callback. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface TransformAwareDrawable {

  /**
   * Sets a transform callback.
   *
   * @param transformCallback the transform callback to be set
   */
  void setTransformCallback(@Nullable TransformCallback transformCallback);
}
