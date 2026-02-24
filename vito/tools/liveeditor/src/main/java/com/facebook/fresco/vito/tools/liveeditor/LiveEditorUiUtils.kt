/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.facebook.fresco.vito.core.impl.debug.StringDebugDataProvider
import com.facebook.fresco.vito.options.ImageOptions

class LiveEditorUiUtils(
    var liveEditor: ImageLiveEditor?,
    var debugDataProviders: List<StringDebugDataProvider>? = null,
) {

  fun createView(
      context: Context,
      customEntries: List<ImageOptionsSampleValues.Entry<out Any>> = emptyList(),
      title: String? = null,
  ): View =
      createScrollingList(context, title = title) {
        addView(createWithList(context, ImageSourceSampleValues.entries))
        addView(createWithList(context, ImageOptionsSampleValues.roundingOptions))
        addView(createWithList(context, ImageOptionsSampleValues.borderOptions))
        addView(createWithList(context, ImageOptionsSampleValues.scaleTypes))
        addView(createWithList(context, ImageOptionsSampleValues.colorFilters))
        addView(createWithList(context, ImageOptionsSampleValues.fadingOptions))
        addView(createWithList(context, ImageOptionsSampleValues.autoPlay))
        addView(createWithList(context, ImageOptionsSampleValues.bitmapConfig))
        addView(createWithList(context, ImageOptionsSampleValues.resizeToViewportConfig))
        addView(
            createWithList(context, ImageOptionsSampleValues.localThumbnailPreviewsEnabledConfig)
        )
        addView(createWithList(context, ImageOptionsSampleValues.progressiveRenderingEnabledConfig))
        addView(createWithList(context, ImageOptionsSampleValues.placeholderColors))

        customEntries.forEach { addView(createWithList(context, it)) }
      }

  private fun createScrollingList(
      context: Context,
      title: String? = null,
      block: LinearLayout.() -> Unit,
  ): View =
      ScrollView(context).apply {
        layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        isVerticalFadingEdgeEnabled = true
        setFadingEdgeLength(24.dpToPx(context))
        addView(
            LinearLayout(context).apply {
              layoutParams =
                  LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT,
                      ViewGroup.LayoutParams.WRAP_CONTENT,
                  )
              orientation = LinearLayout.VERTICAL
              val horizontalPad = 4.dpToPx(context)
              val topPad = 12.dpToPx(context)
              setPadding(horizontalPad, topPad, horizontalPad, 0)
              if (title != null) {
                addView(
                    TextView(context).apply {
                      text = title
                      textSize = 18f
                      setTypeface(typeface, android.graphics.Typeface.BOLD)
                      val titleHorizontalPad = 12.dpToPx(context)
                      val titleBottomPad = 6.dpToPx(context)
                      setPadding(titleHorizontalPad, 0, titleHorizontalPad, titleBottomPad)
                    }
                )
              }
              block(this)
            }
        )
      }

  fun createImageInfoView(context: Context, title: String? = null): View =
      createScrollingList(context, title = title) {
        // 1. ImageSource info
        var info: List<Pair<String, String>> =
            ImageSourceParser.convertSourceToKeyValue(liveEditor?.getSource().toString())

        // 2. ImageOptions info
        liveEditor?.let { editor -> info = info + extractImageOptionsInfo(editor.getOptions()) }

        // 3. Debug provider data
        liveEditor?.let { liveEditorNonNull ->
          debugDataProviders?.forEach { debugProvider ->
            val debugData: Pair<String, String> =
                Pair(debugProvider.longName, debugProvider.extractData(liveEditorNonNull.drawable))
            info = info + debugData
          }
        }

        // Render all info
        if (info.isEmpty()) {
          addView(TextView(context).apply { text = "Source is Empty" })
        }
        info.forEach { infoItem ->
          val view = ImageSourceUiUtil(context).createImageInfoView(infoItem, this)
          addView(view)
        }
      }

  private fun <T> createWithList(
      context: Context,
      entry: ImageOptionsSampleValues.Entry<T>,
  ): Spinner {
    return Spinner(context).apply {
      adapter =
          ArrayAdapter(
              context,
              android.R.layout.simple_spinner_dropdown_item,
              listOf(entry.name + ": original") + entry.data.map { entry.name + ": " + it.first },
          )
      onItemSelectedListener =
          object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
              if (position > 0) {
                liveEditor?.editOptions(context) {
                  entry.updateFunction(it, entry.data[position - 1].second)
                }
              }
            }
          }
    }
  }

  private fun <T> createWithList(
      context: Context,
      entry: ImageSourceSampleValues.Entry<T>,
  ): Spinner {
    return Spinner(context).apply {
      adapter =
          ArrayAdapter(
              context,
              android.R.layout.simple_spinner_dropdown_item,
              listOf(entry.name + ": original") + entry.data.map { entry.name + ": " + it.first },
          )
      onItemSelectedListener =
          object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
              if (position == 0) {
                liveEditor?.editSource(context) { liveEditor?.getOriginalSource() ?: it }
              } else {
                liveEditor?.editSource(context) {
                  entry.updateFunction(it, entry.data[position - 1].second)
                }
              }
            }
          }
    }
  }

  private fun extractImageOptionsInfo(options: ImageOptions): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()

    // Parse the toString() output which is maintained alongside ImageOptions class.
    // Format: "ImageOptions{ImageOptions{key=value, key2=value2, ...}}" — the outer wrapper
    // comes from ImageOptions.toString() and the inner one from Guava's ToStringHelper.
    var content = options.toString().removePrefix("ImageOptions{").removeSuffix("}")
    content = content.removePrefix("ImageOptions{").removeSuffix("}")

    // Parse key=value pairs. We split on ", " but need to handle nested objects
    // that may contain commas (e.g., "RoundingOptions{...}").
    parseKeyValuePairs(content).forEach { (key, value) -> result.add(key to value) }

    return result
  }

  private fun parseKeyValuePairs(content: String): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    var remaining = content.trim()

    while (remaining.isNotEmpty()) {
      // Find the key (everything before '=')
      val equalsIndex = remaining.indexOf('=')
      if (equalsIndex == -1) {
        break
      }

      val key = remaining.substring(0, equalsIndex).trim()
      remaining = remaining.substring(equalsIndex + 1)

      // Find the value - handle nested braces
      val (value, rest) = extractValue(remaining)
      result.add(key to value)
      remaining = rest.trimStart(',', ' ')
    }

    return result
  }

  private fun extractValue(s: String): Pair<String, String> {
    var braceDepth = 0
    var i = 0

    while (i < s.length) {
      val codePoint = s.codePointAt(i)
      when (codePoint) {
        '{'.code -> braceDepth++
        '}'.code -> braceDepth--
        ','.code ->
            if (braceDepth == 0) {
              return s.substring(0, i).trim() to s.substring(i)
            }
      }
      i += Character.charCount(codePoint)
    }

    return s.trim() to ""
  }

  companion object {
    internal fun Int.dpToPxF(context: Context): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics,
        )

    internal fun Int.dpToPx(context: Context): Int = dpToPxF(context).toInt()
  }
}
