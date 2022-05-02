/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Canvas
import android.graphics.Paint
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.renderer.Shape
import com.facebook.fresco.vito.renderer.util.ColorUtils

class BorderRenderer {

  companion object {
    fun renderBorder(canvas: Canvas, borderOptions: BorderOptions, shape: Shape, alpha: Int = 255) {
      shape.draw(canvas, createPaint(borderOptions, alpha))
    }

    fun createPaint(borderOptions: BorderOptions, alpha: Int = 255) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.multiplyColorAlpha(borderOptions.color, alpha)
          strokeWidth = borderOptions.width
          style = Paint.Style.STROKE
        }
  }
}
