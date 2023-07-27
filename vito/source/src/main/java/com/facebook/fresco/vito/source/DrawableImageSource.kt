/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.graphics.drawable.Drawable

data class DrawableImageSource(val drawable: Drawable) : ImageSource {

  override fun hashCode(): Int {
    return drawable.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) return false
    return drawable == (other as DrawableImageSource).drawable
  }
}
