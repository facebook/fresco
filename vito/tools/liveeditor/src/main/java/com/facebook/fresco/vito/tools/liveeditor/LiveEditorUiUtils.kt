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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.facebook.fresco.vito.core.impl.StringDebugDataProvider

class LiveEditorUiUtils(
    var liveEditor: ImageLiveEditor?,
    var debugDataProviders: List<StringDebugDataProvider>? = null
) {

  fun createView(
      context: Context,
      customEntries: List<ImageOptionsSampleValues.Entry<out Any>> = emptyList(),
      closeAction: ((View) -> Unit)? = null
  ): View =
      createScrollingList(context, closeAction) {
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
            createWithList(context, ImageOptionsSampleValues.localThumbnailPreviewsEnabledConfig))
        addView(createWithList(context, ImageOptionsSampleValues.progressiveRenderingEnabledConfig))
        addView(createWithList(context, ImageOptionsSampleValues.placeholderColors))

        customEntries.forEach { addView(createWithList(context, it)) }
      }

  private fun createScrollingList(
      context: Context,
      closeAction: ((View) -> Unit)? = null,
      block: LinearLayout.() -> Unit
  ): View =
      ScrollView(context).apply {
        layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(
            LinearLayout(context).apply {
              layoutParams =
                  LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
              orientation = LinearLayout.VERTICAL
              if (closeAction != null) {
                addView(createButton(context, "Close", closeAction))
              }
              block(this)
            })
      }

  fun createImageInfoView(context: Context, closeAction: (View) -> Unit): View =
      createScrollingList(context, closeAction) {
        var source: List<Pair<String, String>> =
            ImageSourceParser.convertSourceToKeyValue(liveEditor?.getSource().toString())
        liveEditor?.let { liveEditorNonNull ->
          debugDataProviders?.forEach {
            val debugData: Pair<String, String> =
                Pair(it.longName, it.extractData(liveEditorNonNull.drawable))
            source = source + debugData
          }
        }
        if (source.isEmpty()) {
          addView(TextView(context).apply { text = "Source is Empty" })
        }
        source.forEach {
          val view = ImageSourceUiUtil(context).createImageInfoView(it, this)
          addView(view)
        }
      }

  private fun createButton(context: Context, btnText: String, clickAction: (View) -> Unit): Button =
      Button(context).apply {
        text = btnText
        setPadding(paddingLeft, 8.dpToPx(context), paddingRight, 8.dpToPx(context))
        setOnClickListener(clickAction)
      }

  private fun <T> createWithList(
      context: Context,
      entry: ImageOptionsSampleValues.Entry<T>
  ): Spinner {
    return Spinner(context).apply {
      adapter =
          ArrayAdapter(
              context,
              android.R.layout.simple_spinner_dropdown_item,
              listOf(entry.name + ": original") + entry.data.map { entry.name + ": " + it.first })
      onItemSelectedListener =
          object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
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
      entry: ImageSourceSampleValues.Entry<T>
  ): Spinner {
    return Spinner(context).apply {
      adapter =
          ArrayAdapter(
              context,
              android.R.layout.simple_spinner_dropdown_item,
              listOf(entry.name + ": original") + entry.data.map { entry.name + ": " + it.first })
      onItemSelectedListener =
          object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
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

  companion object {
    internal fun Int.dpToPxF(context: Context): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics)

    internal fun Int.dpToPx(context: Context): Int = dpToPxF(context).toInt()
  }
}
