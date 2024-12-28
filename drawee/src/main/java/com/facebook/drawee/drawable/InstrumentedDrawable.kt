/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable

/** Used to log image params at draw-time. */
class InstrumentedDrawable(drawable: Drawable, private val listener: Listener?) :
    ForwardingDrawable(drawable) {

  private val _scaleType: String = getScaleType(drawable)

  fun interface Listener {
    fun track(
        viewWidth: Int,
        viewHeight: Int,
        imageWidth: Int,
        imageHeight: Int,
        scaledWidth: Int,
        scaledHeight: Int,
        scaleType: String?
    )
  }

  private var isChecked = false

  private fun getScaleType(drawable: Drawable): String {
    if (drawable is ScaleTypeDrawable) {
      val type = drawable.scaleType
      return type.toString()
    }
    return "none"
  }

  override fun draw(canvas: Canvas) {
    if (!isChecked) {
      isChecked = true
      val bounds = RectF()
      getRootBounds(bounds)
      val viewWidth = bounds.width().toInt()
      val viewHeight = bounds.height().toInt()
      getTransformedBounds(bounds)
      val scaledWidth = bounds.width().toInt()
      val scaledHeight = bounds.height().toInt()
      val imageWidth = intrinsicWidth
      val imageHeight = intrinsicHeight
      listener?.track(
          viewWidth, viewHeight, imageWidth, imageHeight, scaledWidth, scaledHeight, _scaleType)
    }
    super.draw(canvas)
  }
}
