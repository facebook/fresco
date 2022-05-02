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
import androidx.annotation.LayoutRes
import com.facebook.fresco.samples.showcase.common.dpToPx

/** A base Kotlin class for ShowcaseFragment */
abstract class BaseShowcaseKotlinFragment(
    @LayoutRes private val layoutResId: Int = R.layout.fragment_scrolling_linear_layout
) : BaseShowcaseFragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? = inflater.inflate(layoutResId, container, false)

  fun Int.dpToPx(): Int = this.dpToPx(requireContext())

  fun Float.dpToPx(): Float = this.dpToPx(requireContext())
}
