/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView

class LithoSampleHostFragment(
    private val lithoSample: LithoSample,
    private val helpText: String? = null,
) : BaseShowcaseFragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    return inflater.inflate(R.layout.fragment_litho_host, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    view.findViewById<TextView>(R.id.text_help).text = helpText
    val c = ComponentContext(requireContext())
    val lithoView =
        LithoView.create(c, lithoSample.createLithoComponent(c, sampleUris(), "Example"))
    view.findViewById<FrameLayout>(R.id.container).addView(lithoView)
  }
}
