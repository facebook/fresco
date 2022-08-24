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

class CircleShape(val cx: Float, val cy: Float, val radius: Float) : Shape() {
  override fun draw(canvas: Canvas, paint: Paint) = canvas.drawCircle(cx, cy, radius, paint)
}

class RoundedRectShape(val rect: RectF, val rx: Float, val ry: Float) : Shape() {
  override fun draw(canvas: Canvas, paint: Paint) = canvas.drawRoundRect(rect, rx, ry, paint)
}

class PathShape(val path: Path) : Shape() {
  override fun draw(canvas: Canvas, paint: Paint) = canvas.drawPath(path, paint)
}
