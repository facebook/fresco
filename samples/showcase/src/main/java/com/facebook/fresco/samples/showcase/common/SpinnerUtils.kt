/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.common

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

object SpinnerUtils {
    fun <T> Spinner.setupWithList(data: Pair<List<Pair<String, T>>, String>, clickListener: (T) -> Unit) {
        adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                data.first.map { data.second + ": " + it.first })
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                clickListener(data.first[position].second)
            }
        }
    }

    fun Spinner.setupWithCallbacks(data: List<Pair<String, () -> Unit>>) {
        adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                data.map { it.first })
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                data[position].second()
            }
        }
    }
}
