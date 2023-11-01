/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

sealed class Shape {
  abstract fun draw(canvas: Canvas, paint: Paint)
}

class RectShape(val rect: RectF) : Shape() {
  override fun draw(canvas: Canvas, paint: Paint) = canvas.drawRect(rect, paint)
}

class CircleShape(
    private val cx: Float,
    private val cy: Float,
    private val radius: Float,
    private val antiAliased: Boolean? = null
) : Shape() {
  override fun draw(canvas: Canvas, paint: Paint) {
    if (antiAliased != null) {
      // Store the value for the anti-alias property this paint has
      val paintAntiAliasValue = paint.isAntiAlias
      // Temporarily apply anti-alias property for circle
      paint.isAntiAlias = antiAliased
      canvas.drawCircle(cx, cy, radius, paint)
      // Restore previous value on the paint object to avoid collateral unexpected behaviours
      paint.isAntiAlias = paintAntiAliasValue
    } else {
      canvas.drawCircle(cx, cy, radius, paint)
    }
  }
}

class RoundedRectShape(val rect: RectF, val rx: Float, val ry: Float) : Shape() {
  override fun draw(canvas: Canvas, paint: Paint) = canvas.drawRoundRect(rect, rx, ry, paint)
}

class PathShape(val path: Path) : Shape() {
  override fun draw(canvas: Canvas, paint: Paint) = canvas.drawPath(path, paint)
}
