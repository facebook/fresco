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
import com.facebook.fresco.samples.showcase.databinding.FragmentLithoHostBinding
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView

class LithoSampleHostFragment(
    private val lithoSample: LithoSample,
    private val helpText: String? = null
) : BaseShowcaseFragment() {

  private var _binding: FragmentLithoHostBinding? = null
  private val binding
    get() = _binding!!

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentLithoHostBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    binding.textHelp.text = helpText
    val c = ComponentContext(requireContext())
    val lithoView =
        LithoView.create(c, lithoSample.createLithoComponent(c, sampleUris(), "Example"))
    binding.container.addView(lithoView)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
