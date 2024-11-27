/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.annotation.SuppressLint
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable

/**
 * Set of properties for drawable. There are no default values and only gets applied if were set
 * explicitly.
 */
class DrawableProperties {

  private var alpha = UNSET
  private var isSetColorFilter = false
  private var colorFilter: ColorFilter? = null
  private var dither = UNSET
  private var filterBitmap = UNSET

  fun setAlpha(alpha: Int) {
    this.alpha = alpha
  }

  fun setColorFilter(colorFilter: ColorFilter?) {
    this.colorFilter = colorFilter
    isSetColorFilter = colorFilter != null
  }

  fun setDither(dither: Boolean) {
    this.dither = if (dither) 1 else 0
  }

  fun setFilterBitmap(filterBitmap: Boolean) {
    this.filterBitmap = if (filterBitmap) 1 else 0
  }

  @SuppressLint("Range")
  fun applyTo(drawable: Drawable?) {
    if (drawable == null) {
      return
    }
    if (alpha != UNSET) {
      drawable.alpha = alpha
    }
    if (isSetColorFilter) {
      drawable.colorFilter = colorFilter
    }
    if (dither != UNSET) {
      drawable.setDither(dither != 0)
    }
    if (filterBitmap != UNSET) {
      drawable.isFilterBitmap = filterBitmap != 0
    }
  }

  companion object {
    private const val UNSET = -1
  }
}
