/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** A Drawable that draws nothing on Canvas */
object NopDrawable : Drawable() {

  override fun draw(p0: Canvas) {
    // nop
  }

  override fun setAlpha(p0: Int) {
    // nop
  }

  override fun setColorFilter(p0: ColorFilter?) {
    // nop
  }

  override fun getOpacity(): Int = PixelFormat.OPAQUE
}
