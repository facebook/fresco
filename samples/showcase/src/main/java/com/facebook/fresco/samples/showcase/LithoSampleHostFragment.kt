/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import kotlinx.android.synthetic.main.fragment_litho_host.*

class LithoSampleHostFragment(
    private val lithoSample: LithoSample,
    private val helpText: String? = null
) : BaseShowcaseFragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_litho_host, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    text_help.setText(helpText)
    val c = ComponentContext(context)
    val lithoView =
        LithoView.create(c, lithoSample.createLithoComponent(c, sampleUris(), "Example"))
    container.addView(lithoView)
  }
}
