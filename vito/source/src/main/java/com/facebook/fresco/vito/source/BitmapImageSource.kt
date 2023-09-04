/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.graphics.Bitmap

@Suppress("KtDataClass")
data class BitmapImageSource(val bitmap: Bitmap) : ImageSource {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    return bitmap == (other as BitmapImageSource).bitmap
  }

  override fun hashCode(): Int = bitmap.hashCode()
}
