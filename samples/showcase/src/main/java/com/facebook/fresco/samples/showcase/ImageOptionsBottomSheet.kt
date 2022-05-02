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
import com.facebook.fresco.vito.tools.liveeditor.ImageSelector
import com.facebook.fresco.vito.tools.liveeditor.LiveEditorUiUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ImageOptionsBottomSheet(val imageSelector: ImageSelector) : BottomSheetDialogFragment() {

  private val uiUtils = LiveEditorUiUtils(imageSelector.currentEditor)

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    imageSelector.removeHighlight(requireContext())
    return uiUtils.createView(requireContext())
  }

  companion object {
    @JvmStatic
    fun newInstance(imageSelector: ImageSelector, bundle: Bundle): ImageOptionsBottomSheet {
      val fragment = ImageOptionsBottomSheet(imageSelector)
      fragment.arguments = bundle
      return fragment
    }
  }
}
