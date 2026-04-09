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
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import com.instagram.common.analytics.intf.ConstAnalyticsModule
import com.instagram.common.typedurl.SimpleImageUrl
import com.instagram.common.ui.widget.imageview.CircularImageView
import com.instagram.common.ui.widget.imageview.IgImageView
import com.instagram.common.ui.widget.imageview.RoundedCornerImageView
import com.instagram.common.ui.widget.imageview.listeners.LoadedImageInfo
import com.instagram.common.ui.widget.imageview.listeners.OnLoadListener
import com.instagram.fresco.vitoimagetype.ImageType

/**
 * Builds a screen for a specific [ViewType] with URL input, presets, configurable options, a load
 * button, image display with [DebugOverlay], and a metadata panel.
 *
 * Each screen is a [View] hierarchy that can be swapped into the main content area of
 * [MainActivity]. The screen is self-contained — it manages its own state and image loading.
 */
class ViewTypeScreen(
    private val activity: Activity,
    private val viewType: ViewType,
) {

  private lateinit var urlInput: EditText
  private lateinit var presetSpinner: Spinner
  private lateinit var imageTypeSpinner: Spinner
  private lateinit var sampleSizeSpinner: Spinner
  private lateinit var lowFidelitySwitch: Switch
  private lateinit var progressiveSwitch: Switch
  private lateinit var imageContainer: FrameLayout
  private lateinit var metadataView: TextView
  private val debugOverlay = DebugOverlay(activity)

  // CircularImageView specific
  private var strokeWidthInput: EditText? = null
  private var strokeColorInput: EditText? = null

  // RoundedCornerImageView specific
  private var cornerRadiusInput: EditText? = null

  private val presets = ImageDisplayConfig.presetsForViewType(viewType)
  private val imageTypes =
      arrayOf(
          ImageType.UNIDENTIFIED,
          ImageType.GRID,
          ImageType.PROFILE_CIRCULAR,
          ImageType.VIDEO_COVER,
          ImageType.ADS_NON_9_16,
          ImageType.ADS_IAB_SCREENSHOT,
      )
  private val sampleSizes = arrayOf(1, 2, 4, 8)

  private var loadStartTimeNs = 0L

  fun buildView(): View {
    val root =
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL
          layoutParams =
              ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT,
              )
        }

    root.addView(buildImageSection())
    root.addView(divider())
    root.addView(buildConfigSection())
    root.addView(divider())
    root.addView(buildMetadataSection())

    return root
  }

  // ── Config section (scrollable) ──────────────────────────────────────────────

  @Suppress("UseSwitchCompatOrMaterialCode")
  private fun buildConfigSection(): View {
    val scroll =
        ScrollView(activity).apply {
          layoutParams =
              LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT,
              )
        }

    val container =
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(dp(16), dp(12), dp(16), dp(12))
        }

    // Preset dropdown
    container.addView(label("Preset"))
    presetSpinner =
        Spinner(activity).apply {
          adapter =
              ArrayAdapter(
                  activity,
                  android.R.layout.simple_spinner_dropdown_item,
                  listOf("Custom") + presets.map { it.label },
              )
          onItemSelectedListener =
              object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                  if (position > 0) applyPreset(presets[position - 1])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
              }
        }
    container.addView(presetSpinner)
    container.addView(spacer(8))

    // URL input
    container.addView(label("URL"))
    urlInput =
        EditText(activity).apply {
          setText("https://www.facebook.com/images/fb_icon_325x325.png")
          textSize = 12f
          inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
          setTextColor(TEXT_PRIMARY)
          isSingleLine = true
        }
    container.addView(urlInput)
    container.addView(spacer(8))

    // ImageType dropdown
    container.addView(label("ImageType"))
    imageTypeSpinner =
        Spinner(activity).apply {
          adapter =
              ArrayAdapter(
                  activity,
                  android.R.layout.simple_spinner_dropdown_item,
                  imageTypes.map { it.name },
              )
        }
    container.addView(imageTypeSpinner)
    container.addView(spacer(4))

    // maxSampleSize dropdown
    container.addView(label("maxSampleSize"))
    sampleSizeSpinner =
        Spinner(activity).apply {
          adapter =
              ArrayAdapter(
                  activity,
                  android.R.layout.simple_spinner_dropdown_item,
                  sampleSizes.map { it.toString() },
              )
        }
    container.addView(sampleSizeSpinner)
    container.addView(spacer(4))

    // lowFidelity toggle
    val lowFidelityRow = toggleRow("lowFidelity (RGB_565)")
    lowFidelitySwitch = lowFidelityRow.second
    container.addView(lowFidelityRow.first)

    // progressive toggle
    val progressiveRow = toggleRow("progressive")
    progressiveSwitch = progressiveRow.second
    container.addView(progressiveRow.first)

    // View-type specific controls
    when (viewType) {
      ViewType.CIRCULAR_IMAGE_VIEW -> {
        container.addView(spacer(8))
        container.addView(label("strokeWidth (dp)"))
        strokeWidthInput =
            EditText(activity).apply {
              setText("0")
              inputType = InputType.TYPE_CLASS_NUMBER
              textSize = 13f
              setTextColor(TEXT_PRIMARY)
            }
        container.addView(strokeWidthInput)

        container.addView(label("strokeColor (hex)"))
        strokeColorInput =
            EditText(activity).apply {
              setText("#E1306C")
              textSize = 13f
              setTextColor(TEXT_PRIMARY)
            }
        container.addView(strokeColorInput)
      }
      ViewType.ROUNDED_CORNER_IMAGE_VIEW -> {
        container.addView(spacer(8))
        container.addView(label("cornerRadius (dp)"))
        cornerRadiusInput =
            EditText(activity).apply {
              setText("8")
              inputType = InputType.TYPE_CLASS_NUMBER
              textSize = 13f
              setTextColor(TEXT_PRIMARY)
            }
        container.addView(cornerRadiusInput)
      }
      ViewType.IG_IMAGE_VIEW -> {}
    }

    container.addView(spacer(12))

    // Load button
    container.addView(
        Button(activity).apply {
          text = "Load Image"
          setOnClickListener { loadImage() }
        }
    )

    scroll.addView(container)
    return scroll
  }

  // ── Image section ────────────────────────────────────────────────────────────

  private fun buildImageSection(): View {
    imageContainer =
        FrameLayout(activity).apply {
          layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(300))
          setBackgroundColor(SURFACE_BG)
        }
    return imageContainer
  }

  // ── Metadata section ─────────────────────────────────────────────────────────

  private fun buildMetadataSection(): View {
    val scroll =
        ScrollView(activity).apply {
          layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

    metadataView =
        TextView(activity).apply {
          setPadding(dp(16), dp(12), dp(16), dp(16))
          textSize = 11f
          typeface = Typeface.MONOSPACE
          setTextColor(TEXT_SECONDARY)
          setBackgroundColor(Color.WHITE)
        }
    scroll.addView(metadataView)
    return scroll
  }

  // ── Image loading ────────────────────────────────────────────────────────────

  private fun loadImage() {
    imageContainer.removeAllViews()
    debugOverlay.hide()

    val url = urlInput.text.toString().trim()
    if (url.isEmpty()) return

    val imageView = createImageView()
    val size =
        if (viewType == ViewType.CIRCULAR_IMAGE_VIEW) dp(200)
        else FrameLayout.LayoutParams.MATCH_PARENT
    imageView.layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
    imageContainer.addView(imageView)
    imageContainer.addView(debugOverlay.view)

    val imageUrl = SimpleImageUrl(url)
    val selectedImageType = imageTypes[imageTypeSpinner.selectedItemPosition]
    val selectedSampleSize = sampleSizes[sampleSizeSpinner.selectedItemPosition]
    val lowFidelity = lowFidelitySwitch.isChecked
    val progressive = progressiveSwitch.isChecked
    val module = ConstAnalyticsModule(viewType.label)

    loadStartTimeNs = System.nanoTime()

    imageView.setOnLoadListener(
        object : OnLoadListener {
          override fun onLoad(info: LoadedImageInfo) {
            val durationMs = (System.nanoTime() - loadStartTimeNs) / 1_000_000.0
            val loadSource = imageView.loadSource ?: info.loadSource ?: "?"
            val route = (activity.application as SampleApplication).describeRoute()
            Log.d(
                TAG,
                "⏱ ${viewType.label} loaded in ${"%.1f".format(durationMs)}ms via $route source=$loadSource",
            )
            activity.runOnUiThread {
              val bitmap = imageView.getBitmap()
              val app = activity.application as SampleApplication
              val isCacheRequestPath = !(app.toggleStates["use_vito"] ?: true)
              debugOverlay.update(
                  DebugOverlay.OverlayData(
                      pathLabel = app.describeRoute(),
                      loadTimeMs = durationMs,
                      loadSource = loadSource,
                      bitmapWidth = bitmap?.width ?: info.bitmapInfo.bitmapWidth,
                      bitmapHeight = bitmap?.height ?: info.bitmapInfo.bitmapHeight,
                      bitmapConfig = bitmap?.config,
                      bitmapByteCount = bitmap?.byteCount ?: 0,
                      viewWidth = imageView.width,
                      viewHeight = imageView.height,
                      isCacheRequestPath = isCacheRequestPath,
                      maxSampleSize = selectedSampleSize,
                      crViewWidth = imageView.width,
                      crViewHeight = imageView.height,
                      lowFidelity = lowFidelity,
                      progressive = progressive,
                  )
              )
              showMetadata(
                  durationMs,
                  selectedImageType,
                  selectedSampleSize,
                  lowFidelity,
                  progressive,
              )
            }
          }

          override fun onFailToLoad(errorMessage: String?, statusCode: Int?) {
            val durationMs = (System.nanoTime() - loadStartTimeNs) / 1_000_000.0
            Log.w(
                TAG,
                "✗ ${viewType.label} FAILED in ${"%.1f".format(durationMs)}ms: $errorMessage",
            )
            activity.runOnUiThread {
              android.widget.Toast.makeText(
                      activity,
                      "Image load failed: ${errorMessage ?: "Unknown error"}",
                      android.widget.Toast.LENGTH_LONG,
                  )
                  .show()
              showMetadata(
                  durationMs,
                  selectedImageType,
                  selectedSampleSize,
                  lowFidelity,
                  progressive,
                  error = errorMessage,
              )
            }
          }
        }
    )

    // Set progressive image config when progressive switch is checked
    if (progressive) {
      imageView.setProgressiveImageConfig(
          com.instagram.common.cache.image.utils.ProgressiveImageConfig()
      )
    }

    when {
      lowFidelity -> imageView.setUrl(imageUrl, module, false, true, selectedImageType)
      selectedSampleSize > 1 -> imageView.setUrl(imageUrl, module, selectedSampleSize)
      else -> imageView.setUrl(imageUrl, module, selectedImageType)
    }
  }

  private fun createImageView(): IgImageView =
      when (viewType) {
        ViewType.CIRCULAR_IMAGE_VIEW ->
            CircularImageView(activity).apply {
              scaleType = ImageView.ScaleType.CENTER_CROP
              val strokeW = strokeWidthInput?.text?.toString()?.toIntOrNull() ?: 0
              if (strokeW > 0) {
                val colorStr = strokeColorInput?.text?.toString() ?: "#E1306C"
                val color =
                    try {
                      Color.parseColor(colorStr)
                    } catch (e: Exception) {
                      0xFFE1306C.toInt()
                    }
                setStroke(dp(strokeW), color)
              }
            }
        ViewType.ROUNDED_CORNER_IMAGE_VIEW ->
            RoundedCornerImageView(activity).apply {
              val radius = cornerRadiusInput?.text?.toString()?.toIntOrNull() ?: 8
              setRadius(dp(radius).toFloat())
              scaleType = ImageView.ScaleType.CENTER_CROP
            }
        ViewType.IG_IMAGE_VIEW ->
            IgImageView(activity).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
      }

  // ── Presets ──────────────────────────────────────────────────────────────────

  private fun applyPreset(config: ImageDisplayConfig) {
    urlInput.setText(config.imageUrl)
    imageTypeSpinner.setSelection(imageTypes.indexOf(config.imageType).coerceAtLeast(0))
    sampleSizeSpinner.setSelection(sampleSizes.indexOf(config.maxSampleSize).coerceAtLeast(0))
    lowFidelitySwitch.isChecked = config.lowFidelity
    progressiveSwitch.isChecked = config.progressive

    when (viewType) {
      ViewType.CIRCULAR_IMAGE_VIEW -> {
        strokeWidthInput?.setText(config.strokeWidthDp.toString())
        if (config.strokeColor != 0) {
          strokeColorInput?.setText(String.format("#%06X", 0xFFFFFF and config.strokeColor))
        }
      }
      ViewType.ROUNDED_CORNER_IMAGE_VIEW -> {
        cornerRadiusInput?.setText(config.cornerRadiusDp.toString())
      }
      ViewType.IG_IMAGE_VIEW -> {}
    }
  }

  // ── Metadata panel ───────────────────────────────────────────────────────────

  private fun showMetadata(
      durationMs: Double,
      imageType: ImageType,
      sampleSize: Int,
      lowFidelity: Boolean,
      progressive: Boolean,
      error: String? = null,
  ) {
    metadataView.text = buildString {
      val route = (activity.application as SampleApplication).describeRoute()
      appendLine("Route           $route")
      appendLine()
      appendLine("ViewType        ${viewType.label}")
      appendLine("ImageType       $imageType")
      appendLine("maxSampleSize   $sampleSize")
      appendLine("lowFidelity     $lowFidelity")
      appendLine("progressive     $progressive")
      appendLine()
      if (error != null) {
        appendLine("Load FAILED     ${"%.1f".format(durationMs)}ms")
        append("Error           $error")
      } else {
        append("Load time       ${"%.1f".format(durationMs)}ms")
      }
    }
  }

  // ── UI helpers ───────────────────────────────────────────────────────────────

  private fun label(text: String): View =
      TextView(activity).apply {
        this.text = text
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        setTextColor(TEXT_SECONDARY)
        setPadding(0, dp(4), 0, dp(2))
      }

  @Suppress("UseSwitchCompatOrMaterialCode")
  private fun toggleRow(text: String): Pair<View, Switch> {
    val switch = Switch(activity)
    val row =
        LinearLayout(activity).apply {
          orientation = LinearLayout.HORIZONTAL
          gravity = Gravity.CENTER_VERTICAL
          setPadding(0, dp(4), 0, dp(4))
          addView(
              TextView(activity).apply {
                this.text = text
                textSize = 13f
                setTextColor(TEXT_PRIMARY)
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
              }
          )
          addView(switch)
        }
    return Pair(row, switch)
  }

  private fun spacer(heightDp: Int): View =
      View(activity).apply {
        layoutParams =
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp))
      }

  private fun divider(): View =
      View(activity).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        setBackgroundColor(DIVIDER_COLOR)
      }

  private fun dp(value: Int): Int =
      TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP,
              value.toFloat(),
              activity.resources.displayMetrics,
          )
          .toInt()

  companion object {
    private const val TAG = "IgImageLoaderSample"
    private const val SURFACE_BG = 0xFFF5F5F5.toInt()
    private const val DIVIDER_COLOR = 0xFFE0E0E0.toInt()
    private const val TEXT_PRIMARY = 0xFF1A1A1A.toInt()
    private const val TEXT_SECONDARY = 0xFF666666.toInt()
  }
}
