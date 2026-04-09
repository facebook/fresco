/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ig.imageloader.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Semi-transparent debug overlay displayed on top of loaded images. Shows pipeline path, load
 * timing, source, and bitmap info.
 *
 * **Compact mode** (default): Single line — `Path A • 287ms • network • 1080×1080`
 *
 * **Expanded mode** (tap to toggle): Full details including bitmap config, memory, view dimensions,
 * and (for Path B) the CacheRequest→FrescoImageRequestFactory translation details showing decode
 * parity gaps.
 */
class DebugOverlay(context: Context) {

  private val textView =
      TextView(context).apply {
        setBackgroundColor(OVERLAY_BG)
        setTextColor(Color.WHITE)
        textSize = 10f
        typeface = Typeface.MONOSPACE
        setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4))
        layoutParams =
            FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                )
                .apply { gravity = Gravity.TOP or Gravity.START }
        visibility = View.GONE
      }

  private var expanded = false
  private var compactText = ""
  private var expandedText = ""

  val view: View = textView

  init {
    textView.setOnClickListener {
      expanded = !expanded
      textView.text = if (expanded) expandedText else compactText
    }
  }

  fun update(data: OverlayData) {
    val sourceLabel = data.loadSource ?: "?"
    val dimsLabel = "${data.bitmapWidth}×${data.bitmapHeight}"
    val timeLabel = "${"%.0f".format(data.loadTimeMs)}ms"
    val pathLabel = data.pathLabel

    compactText = "$pathLabel  •  $timeLabel  •  $sourceLabel  •  $dimsLabel"

    expandedText = buildString {
      appendLine("$pathLabel    $timeLabel")
      appendLine()
      appendLine("Source          $sourceLabel")
      val configName = data.bitmapConfig?.name ?: "?"
      val memoryKb = (data.bitmapByteCount / 1024.0)
      appendLine(
          "Bitmap          $dimsLabel  $configName  ${"%.1f".format(memoryKb)}KB",
      )
      appendLine("View            ${data.viewWidth}×${data.viewHeight}")
      if (data.encodedSize > 0) {
        appendLine("Encoded         ${"%.1f".format(data.encodedSize / 1024.0)}KB")
      }

      if (data.isCacheRequestPath) {
        appendLine()
        appendLine("CacheRequest")
        appendLine("  maxSampleSize     ${data.maxSampleSize}")
        appendLine("  viewWidthPx       ${data.crViewWidth}")
        appendLine("  viewHeightPx      ${data.crViewHeight}")
        appendLine("  lowFidelity       ${data.lowFidelity}")
        appendLine("  progressive       ${data.progressive}")
      }
    }

    textView.text = compactText
    textView.visibility = View.VISIBLE
    expanded = false
  }

  fun hide() {
    textView.visibility = View.GONE
  }

  data class OverlayData(
      val pathLabel: String,
      val loadTimeMs: Double,
      val loadSource: String?,
      val bitmapWidth: Int = 0,
      val bitmapHeight: Int = 0,
      val bitmapConfig: Bitmap.Config? = null,
      val bitmapByteCount: Int = 0,
      val viewWidth: Int = 0,
      val viewHeight: Int = 0,
      val encodedSize: Int = 0,
      val isCacheRequestPath: Boolean = false,
      val maxSampleSize: Int = 1,
      val crViewWidth: Int = 0,
      val crViewHeight: Int = 0,
      val lowFidelity: Boolean = false,
      val progressive: Boolean = false,
  )

  companion object {
    private const val OVERLAY_BG = 0xAA000000.toInt()

    private fun dp(context: Context, value: Int): Int =
        TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value.toFloat(),
                context.resources.displayMetrics,
            )
            .toInt()
  }
}
