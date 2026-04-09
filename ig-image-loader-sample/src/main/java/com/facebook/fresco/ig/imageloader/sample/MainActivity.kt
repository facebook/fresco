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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Activity with image loading screen. Navigation drawer added in the next diff. */
class MainActivity : Activity() {

  private lateinit var contentContainer: FrameLayout
  private lateinit var titleView: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(buildMainContent())
    showScreen(ViewType.IG_IMAGE_VIEW)
  }

  private fun buildMainContent(): View {
    val root =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          layoutParams =
              FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.MATCH_PARENT,
                  FrameLayout.LayoutParams.MATCH_PARENT,
              )
          setBackgroundColor(Color.WHITE)
        }

    root.addView(buildToolbar())
    root.addView(divider())

    contentContainer =
        FrameLayout(this).apply {
          layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
    root.addView(contentContainer)

    return root
  }

  private fun buildToolbar(): View {
    val toolbar =
        LinearLayout(this).apply {
          orientation = LinearLayout.HORIZONTAL
          setPadding(dp(4), dp(8), dp(16), dp(8))
          gravity = Gravity.CENTER_VERTICAL
        }

    titleView =
        TextView(this).apply {
          text = "IG Image Loader Sample"
          textSize = 18f
          setTypeface(null, Typeface.BOLD)
          setTextColor(TEXT_PRIMARY)
          setPadding(dp(12), dp(8), dp(12), dp(8))
        }
    toolbar.addView(titleView)

    return toolbar
  }

  private fun showScreen(viewType: ViewType) {
    titleView.text = viewType.label
    contentContainer.removeAllViews()

    val screen = ViewTypeScreen(this, viewType)
    contentContainer.addView(screen.buildView())
  }

  // ── UI helpers ─────────────────────────────────────────────────────────────────

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
  }
}
