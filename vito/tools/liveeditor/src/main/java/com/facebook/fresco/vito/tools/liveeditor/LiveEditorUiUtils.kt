/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast

class LiveEditorUiUtils(var liveEditor: ImageLiveEditor?) {

  fun createView(context: Context): View {
    return LinearLayout(context).apply {
      layoutParams =
          LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
      orientation = LinearLayout.VERTICAL
      addView(createWithList(context, ImageSourceSampleValues.entries))
      addView(createWithList(context, ImageOptionsSampleValues.roundingOptions))
      addView(createWithList(context, ImageOptionsSampleValues.borderOptions))
      addView(createWithList(context, ImageOptionsSampleValues.scaleTypes))
      addView(createWithList(context, ImageOptionsSampleValues.colorFilters))
      addView(createWithList(context, ImageOptionsSampleValues.fadingOptions))
      addView(createWithList(context, ImageOptionsSampleValues.autoPlay))
      addView(createWithList(context, ImageOptionsSampleValues.bitmapConfig))
      addView(createImageInfoButton(context))
    }
  }

  fun createImageInfoButton(context: Context): Button =
      Button(context).apply {
        text = "Image Info"
        setOnClickListener {
          Toast.makeText(
                  context, "ImageSource: ${liveEditor?.getSource().toString()}", Toast.LENGTH_LONG)
              .show()
        }
      }

  fun <T> createWithList(context: Context, entry: ImageOptionsSampleValues.Entry<T>): Spinner {
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
  fun <T> createWithList(context: Context, entry: ImageSourceSampleValues.Entry<T>): Spinner {
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
                liveEditor?.editSource(context) {
                  entry.updateFunction(it, entry.data[position - 1].second)
                }
              }
            }
          }
    }
  }
}
