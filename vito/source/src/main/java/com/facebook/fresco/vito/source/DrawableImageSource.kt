/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.graphics.drawable.Drawable

@Suppress("KtDataClass")
data class DrawableImageSource(val drawable: Drawable) : ImageSource {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    other as DrawableImageSource

    return drawable == other.drawable
  }

  override fun hashCode(): Int = drawable.hashCode()
}
