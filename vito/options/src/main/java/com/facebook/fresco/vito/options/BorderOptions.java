/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options;

import androidx.annotation.ColorInt;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class BorderOptions {

  public static BorderOptions create(@ColorInt int color, float width) {
    return new BorderOptions(color, width);
  }

  public final @ColorInt int color;
  public final float width;

  public BorderOptions(@ColorInt int color, float width) {
    this.color = color;
    this.width = width;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    BorderOptions that = (BorderOptions) obj;
    return color == that.color && width == that.width;
  }

  @Override
  public int hashCode() {
    int result = color;
    result = 31 * result + Float.floatToIntBits(width);
    return result;
  }
}
