/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawable

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import com.facebook.drawee.drawable.DrawableUtils
import com.facebook.fresco.vito.options.BorderOptions
import kotlin.math.min

class CircularBorderBitmapDrawable(
    res: Resources?,
    bitmap: Bitmap?,
    private var borderOptions: BorderOptions? = null
) : BitmapDrawable(res, bitmap) {

  private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

  private var radius = 0
  private var _alpha = 255

  override fun draw(canvas: Canvas) {
    if (radius == 0) return
    val border = borderOptions
    if (border == null || border.padding < 0.0f || border.width < 0.0f) {
      super.draw(canvas)
      return
    }
    val widthReduction =
        if (border.scaleDownInsideBorders) border.width + border.padding else border.padding
    if (widthReduction > radius) return
    val centerX = bounds.exactCenterX()
    val centerY = bounds.exactCenterY()
    if (widthReduction > 0.0f) {
      val scale = (radius - widthReduction) / radius
      canvas.save()
      canvas.scale(scale, scale, centerX, centerY)
      super.draw(canvas)
      canvas.restore()
    } else {
      super.draw(canvas)
    }
    if (border.width > 0.0f) {
      canvas.drawCircle(centerX, centerY, radius - border.width / 2, borderPaint)
    }
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    radius = min(bounds.width(), bounds.height()) / 2
  }

  var border: BorderOptions?
    get() = borderOptions
    set(borderOptions) {
      if (this.borderOptions == null || this.borderOptions != borderOptions) {
        this.borderOptions = borderOptions
        ensureBorderPaint()
        invalidateSelf()
      }
    }

  init {
    borderPaint.style = Paint.Style.STROKE
    ensureBorderPaint()
  }

  override fun setAlpha(alpha: Int) {
    super.setAlpha(alpha)
    _alpha = alpha
    ensureBorderPaint()
  }

  private fun ensureBorderPaint() {
    borderOptions?.let {
      borderPaint.strokeWidth = it.width
      borderPaint.color = DrawableUtils.multiplyColorAlpha(it.color, _alpha)
    }
  }
}
