/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Pair
import android.view.Gravity
import androidx.annotation.ColorInt
import java.util.LinkedHashMap
import kotlin.math.max
import kotlin.math.min

open class DebugOverlayDrawable
@JvmOverloads
constructor(
    val identifier: String = "",
    private val identifierColor: Int = 0xFF00FF00.toInt(),
) : Drawable() {

  @ColorInt var backgroundColor: Int = Color.TRANSPARENT
  var textGravity: Int = Gravity.TOP

  var drawIdentifier: Boolean = true

  var onBoundsChangedCallback: ((Rect) -> Unit)? = null

  // Internal helpers
  private val debugData = LinkedHashMap<String, Pair<String, Int>>()
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private var maxLineLength = INITIAL_MAX_LINE_LENGTH
  private var startTextXPx = 0
  private var startTextYPx = 0
  private var lineIncrementPx = 0
  private var currentTextXPx = 0
  private var currentTextYPx = 0

  init {
    reset()
  }

  @JvmOverloads
  fun addDebugData(key: String, value: String, color: Int = TEXT_COLOR) {
    debugData[key] = Pair(value, color)
    maxLineLength = max(value.length, maxLineLength)
    prepareDebugTextParameters(bounds)
  }

  fun reset() {
    debugData.clear()
    maxLineLength = INITIAL_MAX_LINE_LENGTH
    onBoundsChangedCallback = null
    invalidateSelf()
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    prepareDebugTextParameters(bounds)
    onBoundsChangedCallback?.invoke(bounds)
  }

  override fun draw(canvas: Canvas) {
    // Draw bounding box
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = OUTLINE_STROKE_WIDTH_PX.toFloat()
    paint.color = OUTLINE_COLOR
    canvas.drawRect(
        bounds.left.toFloat(),
        bounds.top.toFloat(),
        bounds.right.toFloat(),
        bounds.bottom.toFloat(),
        paint)

    // Draw overlay
    paint.style = Paint.Style.FILL
    paint.color = backgroundColor
    canvas.drawRect(
        bounds.left.toFloat(),
        bounds.top.toFloat(),
        bounds.right.toFloat(),
        bounds.bottom.toFloat(),
        paint)

    // Draw text
    paint.style = Paint.Style.FILL
    paint.strokeWidth = 0f
    paint.color = TEXT_COLOR

    // Reset the text position
    currentTextXPx = startTextXPx
    currentTextYPx = startTextYPx
    if (drawIdentifier) {
      addDebugText(canvas, "Vito", identifier, identifierColor)
    }
    for ((key, value) in debugData) {
      addDebugText(canvas, key, value.first, value.second)
    }
  }

  override fun setAlpha(alpha: Int) = Unit

  override fun setColorFilter(cf: ColorFilter?) = Unit

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  private fun prepareDebugTextParameters(bounds: Rect) {
    if (debugData.isEmpty() || maxLineLength <= 0) {
      return
    }
    var textSizePx = min(bounds.width() / maxLineLength, bounds.height() / debugData.size)
    textSizePx = min(MAX_TEXT_SIZE_PX, max(MIN_TEXT_SIZE_PX, textSizePx))
    paint.textSize = textSizePx.toFloat()
    lineIncrementPx = textSizePx + TEXT_LINE_SPACING_PX
    startTextXPx = bounds.left + TEXT_PADDING_PX
    startTextYPx =
        if (textGravity == Gravity.BOTTOM) bounds.bottom - TEXT_PADDING_PX
        else bounds.top + TEXT_PADDING_PX + textSizePx
  }

  protected fun addDebugText(canvas: Canvas, label: String, value: String, color: Int) {
    val labelColon = "$label: "
    val labelColonWidth = paint.measureText(labelColon)
    val valueWidth = paint.measureText(value)
    paint.color = TEXT_BACKGROUND_COLOR
    canvas.drawRect(
        (currentTextXPx - MARGIN).toFloat(),
        (currentTextYPx + TEXT_LINE_SPACING_PX).toFloat(),
        currentTextXPx + labelColonWidth + valueWidth + MARGIN,
        (currentTextYPx - lineIncrementPx + TEXT_LINE_SPACING_PX).toFloat(),
        paint)
    paint.color = TEXT_COLOR
    canvas.drawText(labelColon, currentTextXPx.toFloat(), currentTextYPx.toFloat(), paint)
    paint.color = color
    canvas.drawText(value, currentTextXPx + labelColonWidth, currentTextYPx.toFloat(), paint)
    if (textGravity == Gravity.BOTTOM) {
      currentTextYPx -= lineIncrementPx
    } else {
      currentTextYPx += lineIncrementPx
    }
  }

  protected fun addDebugText(
      canvas: Canvas,
      x: Int,
      y: Int,
      label: String,
      value: String,
      color: Int
  ) {
    val labelColon = "$label: "
    val labelColonWidth = paint.measureText(labelColon)
    val valueWidth = paint.measureText(value)
    paint.color = TEXT_BACKGROUND_COLOR
    canvas.drawRect(
        (x - MARGIN).toFloat(),
        (y + TEXT_LINE_SPACING_PX).toFloat(),
        x + labelColonWidth + valueWidth + MARGIN,
        (y + lineIncrementPx + TEXT_LINE_SPACING_PX).toFloat(),
        paint)
    paint.color = TEXT_COLOR
    canvas.drawText(labelColon, currentTextXPx.toFloat(), currentTextYPx.toFloat(), paint)
    paint.color = color
    canvas.drawText(value, currentTextXPx + labelColonWidth, currentTextYPx.toFloat(), paint)
    currentTextYPx += lineIncrementPx
  }

  companion object {
    private const val OUTLINE_COLOR = 0xFFFF9800.toInt()
    private const val TEXT_BACKGROUND_COLOR = 0x66000000
    private const val TEXT_COLOR = Color.WHITE
    private const val OUTLINE_STROKE_WIDTH_PX = 2
    private const val MAX_TEXT_SIZE_PX = 72
    private const val MIN_TEXT_SIZE_PX = 16
    private const val TEXT_LINE_SPACING_PX = 8
    private const val TEXT_PADDING_PX = 10
    private const val INITIAL_MAX_LINE_LENGTH = 4
    private const val MARGIN = TEXT_LINE_SPACING_PX / 2
  }
}
