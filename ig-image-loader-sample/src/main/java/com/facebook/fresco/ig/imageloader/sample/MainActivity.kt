/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ig.imageloader.sample

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout

class MainActivity : Activity() {

  private lateinit var drawerLayout: DrawerLayout
  private lateinit var contentContainer: FrameLayout
  private lateinit var titleView: TextView
  private var selectedMenuItem: TextView? = null

  private val toggleStates: Map<String, Boolean>
    get() = (application as SampleApplication).toggleStates

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(buildRootLayout())
    showScreen(ViewType.IG_IMAGE_VIEW)
  }

  private fun buildRootLayout(): View {
    drawerLayout =
        DrawerLayout(this).apply {
          layoutParams =
              FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.MATCH_PARENT,
                  FrameLayout.LayoutParams.MATCH_PARENT,
              )
        }
    drawerLayout.addView(buildMainContent())
    drawerLayout.addView(buildDrawerMenu())
    return drawerLayout
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

    toolbar.addView(
        TextView(this).apply {
          text = "☰"
          textSize = 22f
          setPadding(dp(12), dp(8), dp(12), dp(8))
          setOnClickListener { drawerLayout.openDrawer(Gravity.START) }
        },
    )

    titleView =
        TextView(this).apply {
          text = "IG Image Loader Sample"
          textSize = 18f
          setTypeface(null, Typeface.BOLD)
          setTextColor(TEXT_PRIMARY)
        }
    toolbar.addView(titleView)

    return toolbar
  }

  // ── Drawer menu ────────────────────────────────────────────────────────────────

  private fun buildDrawerMenu(): View {
    val drawer =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          layoutParams =
              DrawerLayout.LayoutParams(dp(300), DrawerLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.START
              }
          setBackgroundColor(Color.WHITE)
        }

    drawer.addView(
        TextView(this).apply {
          text = "IG Image Loader Sample"
          textSize = 16f
          setTypeface(null, Typeface.BOLD)
          setTextColor(TEXT_PRIMARY)
          setPadding(dp(16), dp(16), dp(16), dp(4))
        },
    )

    // Active route label
    drawer.addView(
        TextView(this).apply {
          text = (application as SampleApplication).describeRoute()
          textSize = 12f
          setTextColor(ACCENT_COLOR)
          setPadding(dp(16), dp(0), dp(16), dp(8))
        },
    )
    drawer.addView(divider())

    val menuScroll =
        ScrollView(this).apply {
          layoutParams =
              LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.MATCH_PARENT,
              )
        }

    val menuList =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(0, 0, 0, dp(16))
        }

    // ── MC Config Overrides (toggles) ────────────────────────────────────
    menuList.addView(sectionHeader("MC CONFIG OVERRIDES"))
    menuList.addView(buildToggleSection())
    menuList.addView(divider())

    // ── View Types ───────────────────────────────────────────────────────
    menuList.addView(sectionHeader("VIEW TYPES"))

    val viewTypes =
        listOf(
            ViewType.IG_IMAGE_VIEW,
            ViewType.CIRCULAR_IMAGE_VIEW,
            ViewType.ROUNDED_CORNER_IMAGE_VIEW,
        )
    viewTypes.forEachIndexed { index, viewType ->
      val item = buildMenuItem(viewType)
      if (index == 0) highlightMenuItem(item)
      menuList.addView(item)
    }

    menuScroll.addView(menuList)
    drawer.addView(menuScroll)

    return drawer
  }

  @Suppress("UseSwitchCompatOrMaterialCode")
  private fun buildToggleSection(): View {
    val container =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(dp(16), dp(4), dp(16), dp(8))
        }

    var currentCategory: ToggleCategory? = null

    for (toggle in SampleToggle.ALL) {
      if (toggle.category != currentCategory) {
        currentCategory = toggle.category
        container.addView(
            TextView(this).apply {
              text = currentCategory.label
              textSize = 11f
              setTypeface(null, Typeface.BOLD)
              setTextColor(TEXT_HINT)
              setPadding(0, dp(if (container.childCount == 0) 0 else 8), 0, dp(4))
            },
        )
      }

      val row =
          LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(2))
          }

      val switchRow =
          LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
          }

      switchRow.addView(
          TextView(this).apply {
            text = toggle.label
            textSize = 13f
            setTextColor(TEXT_PRIMARY)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
          },
      )

      val currentState = toggleStates[toggle.key] ?: toggle.defaultValue
      val switch =
          Switch(this).apply {
            isChecked = currentState
            setOnCheckedChangeListener { _, isChecked ->
              SampleToggle.save(this@MainActivity, toggle.key, isChecked)
              showRestartDialog(toggle.label, isChecked)
            }
          }
      switchRow.addView(switch)
      row.addView(switchRow)

      row.addView(
          TextView(this).apply {
            text = toggle.description
            textSize = 10f
            setTextColor(TEXT_HINT)
            setPadding(0, dp(1), 0, 0)
          },
      )

      container.addView(row)
    }

    return container
  }

  private fun showRestartDialog(toggleLabel: String, enabled: Boolean) {
    val state = if (enabled) "ON" else "OFF"
    AlertDialog.Builder(this)
        .setTitle("Restart Required")
        .setMessage("$toggleLabel set to $state.\n\nThe app will restart to apply the change.")
        .setPositiveButton("Restart") { _, _ -> restartApp() }
        .setNegativeButton("Later", null)
        .show()
  }

  private fun restartApp() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
    Runtime.getRuntime().exit(0)
  }

  // ── Menu items ─────────────────────────────────────────────────────────────────

  private fun buildMenuItem(viewType: ViewType): TextView {
    return TextView(this).apply {
      text = viewType.label
      textSize = 15f
      setTextColor(TEXT_PRIMARY)
      setPadding(dp(24), dp(12), dp(16), dp(12))
      setOnClickListener { onMenuItemClicked(this, viewType) }
    }
  }

  private fun onMenuItemClicked(item: TextView, viewType: ViewType) {
    selectedMenuItem?.setBackgroundColor(Color.TRANSPARENT)
    highlightMenuItem(item)
    showScreen(viewType)
    drawerLayout.closeDrawer(Gravity.START)
  }

  private fun highlightMenuItem(item: TextView) {
    item.setBackgroundColor(MENU_SELECTED_BG)
    selectedMenuItem = item
  }

  private fun showScreen(viewType: ViewType) {
    titleView.text = viewType.label
    contentContainer.removeAllViews()

    val screen = ViewTypeScreen(this, viewType)
    contentContainer.addView(screen.buildView())
  }

  // ── UI helpers ─────────────────────────────────────────────────────────────────

  private fun sectionHeader(text: String): View =
      TextView(this).apply {
        this.text = text
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        setTextColor(TEXT_SECONDARY)
        letterSpacing = 0.08f
        setPadding(dp(16), dp(16), dp(16), dp(4))
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
    private const val MENU_SELECTED_BG = 0xFFE8E8E8.toInt()
    private const val TEXT_PRIMARY = 0xFF1A1A1A.toInt()
    private const val TEXT_SECONDARY = 0xFF666666.toInt()
    private const val TEXT_HINT = 0xFF999999.toInt()
    private const val ACCENT_COLOR = 0xFF0095F6.toInt()
  }
}
