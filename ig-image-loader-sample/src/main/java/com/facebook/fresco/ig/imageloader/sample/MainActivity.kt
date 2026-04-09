/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ig.imageloader.sample

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Minimal activity that verifies the pipeline initialized correctly. */
class MainActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(buildRootLayout())
  }

  private fun buildRootLayout(): View {
    val root =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setBackgroundColor(Color.WHITE)
        }

    // App title
    root.addView(
        TextView(this).apply {
          text = "IG Image Loader Sample"
          textSize = 20f
          setTypeface(null, Typeface.BOLD)
          setTextColor(TEXT_PRIMARY)
          setPadding(dp(16), dp(16), dp(16), dp(16))
        },
    )

    root.addView(divider())

    // Active pipeline route
    root.addView(
        TextView(this).apply {
          text = (application as SampleApplication).describeRoute()
          textSize = 14f
          setTextColor(ACCENT_COLOR)
          setPadding(dp(16), dp(12), dp(16), dp(12))
        },
    )

    root.addView(divider())

    // Placeholder — image loading UI added in upcoming diffs
    root.addView(
        TextView(this).apply {
          text = "Pipeline initialized successfully.\nImage loading screens coming next."
          textSize = 16f
          setTextColor(TEXT_SECONDARY)
          gravity = Gravity.CENTER
          setPadding(dp(32), dp(64), dp(32), dp(32))
          layoutParams =
              LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  0,
                  1f,
              )
        },
    )

    return root
  }

  private fun dp(value: Int): Int =
      TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP,
              value.toFloat(),
              resources.displayMetrics,
          )
          .toInt()

  private fun divider(): View =
      View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        setBackgroundColor(DIVIDER_COLOR)
      }

  companion object {
    private const val DIVIDER_COLOR = 0xFFE0E0E0.toInt()
    private const val TEXT_PRIMARY = 0xFF1A1A1A.toInt()
    private const val TEXT_SECONDARY = 0xFF666666.toInt()
    private const val ACCENT_COLOR = 0xFF0095F6.toInt()
  }
}
