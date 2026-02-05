/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

/**
 * A lightweight debug overlay that shows a small color-coded bar at the bottom of the image.
 *
 * The bar color indicates the image origin:
 * - Green: memory_bitmap or local (fastest)
 * - Yellow: disk or memory_encoded (medium)
 * - Red: network (slowest)
 * - Gray: unknown
 *
 * The bar displays essential information like dimensions and origin.
 */
@SuppressLint("ColorConstantUsageIssue")
class LightweightDebugOverlayDrawable : Drawable() {

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

  @ColorInt private var barColor: Int = Color.GRAY
  private var debugText: String = ""

  var onBoundsChangedCallback: ((Rect) -> Unit)? = null

  init {
    textPaint.color = Color.WHITE
    textPaint.textSize = TEXT_SIZE_PX.toFloat()
    textPaint.isFakeBoldText = true
    textPaint.setShadowLayer(3f, 1f, 1f, Color.BLACK)
  }

  fun setOriginColor(@ColorInt color: Int) {
    barColor = color
    invalidateSelf()
  }

  fun setDebugText(text: String) {
    debugText = text
    invalidateSelf()
  }

  fun reset() {
    barColor = Color.GRAY
    debugText = ""
    onBoundsChangedCallback = null
    invalidateSelf()
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    onBoundsChangedCallback?.invoke(bounds)
  }

  override fun draw(canvas: Canvas) {
    val bounds = bounds
    if (bounds.isEmpty) {
      return
    }

    // Calculate bar dimensions
    val barHeight = BAR_HEIGHT_PX.coerceAtMost(bounds.height() / 4)
    val barTop = bounds.bottom - barHeight

    // Draw color-coded bar at the bottom
    paint.style = Paint.Style.FILL
    paint.color = barColor
    paint.alpha = BAR_ALPHA
    canvas.drawRect(
        bounds.left.toFloat(),
        barTop.toFloat(),
        bounds.right.toFloat(),
        bounds.bottom.toFloat(),
        paint,
    )

    // Draw text on the bar
    if (debugText.isNotEmpty()) {
      // Center text vertically in the bar
      val textY = barTop + (barHeight + TEXT_SIZE_PX) / 2f - 4f
      val textX = bounds.left + TEXT_PADDING_PX.toFloat()

      // Truncate text if it's too long
      val maxTextWidth = bounds.width() - 2 * TEXT_PADDING_PX
      val displayText =
          if (textPaint.measureText(debugText) > maxTextWidth) {
            truncateText(debugText, maxTextWidth)
          } else {
            debugText
          }

      canvas.drawText(displayText, textX, textY, textPaint)
    }
  }

  private fun truncateText(text: String, maxWidth: Int): String {
    var truncated = text
    while (truncated.length > 3 && textPaint.measureText("$truncated...") > maxWidth) {
      truncated = truncated.dropLast(1)
    }
    return "$truncated..."
  }

  override fun setAlpha(alpha: Int) = Unit

  override fun setColorFilter(colorFilter: ColorFilter?) = Unit

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  companion object {
    private const val BAR_HEIGHT_PX = 36
    private const val BAR_ALPHA = 180
    private const val TEXT_SIZE_PX = 24
    private const val TEXT_PADDING_PX = 10
  }
}
