/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Canvas
import android.graphics.Paint
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.renderer.Shape

class BorderRenderer {

  companion object {
    fun renderBorder(canvas: Canvas, borderOptions: BorderOptions, shape: Shape, alpha: Int = 255) {
      shape.draw(canvas, createPaint(borderOptions, alpha))
    }

    fun createPaint(borderOptions: BorderOptions, alpha: Int = 255) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = multiplyColorAlpha(borderOptions.color, alpha)
          strokeWidth = borderOptions.width
          style = Paint.Style.STROKE
        }

    private fun multiplyColorAlpha(color: Int, alpha: Int): Int {
      return when (alpha) {
        255 -> color
        0 -> color and 0x00FFFFFF
        else -> {
          val cappedAlpha = alpha + (alpha shr 7) // make it 0..256
          val colorAlpha = color ushr 24
          val multipliedAlpha = colorAlpha * cappedAlpha shr 8
          multipliedAlpha shl 24 or (color and 0x00FFFFFF)
        }
      }
    }
  }
}
