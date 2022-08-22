/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.databinding.FragmentVitoViewKtxBinding
import com.facebook.fresco.vito.ktx.ViewExtensions.show
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions

class VitoViewKtxFragment : BaseShowcaseFragment() {

  private var _binding: FragmentVitoViewKtxBinding? = null
  private val binding
    get() = _binding!!

  private val imageOptions =
      ImageOptions.create()
          .round(RoundingOptions.asCircle())
          .placeholderRes(R.color.placeholder_color)
          .build()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentVitoViewKtxBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
    val uri = sampleUris().createSampleUri()
    binding.image.show(uri, imageOptions)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
