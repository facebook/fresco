/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.content.Context
import android.graphics.drawable.ColorDrawable
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawable2
import com.facebook.fresco.vito.core.VitoImagePipeline

class ImageSelector(
    val tracker: ImageTracker,
    private val imagePipeline: VitoImagePipeline,
    private val controller: FrescoController2,
    private val overlayColor: Int = 0x88ff0000.toInt(),
    private var currentIndex: Int = 0
) {

  var currentEditor: ImageLiveEditor? = null

  fun selectNext(context: Context) =
      highlightDrawable(context, tracker.getDrawable((++currentIndex) % tracker.drawableCount))

  fun selectPrevious(context: Context) {
    currentIndex--
    if (currentIndex < -1) {
      currentIndex = tracker.drawableCount - 1
    }
    if (currentIndex == -1) {
      removeHighlight(context)
      currentEditor = null
    } else {
      highlightDrawable(context, tracker.getDrawable(currentIndex % tracker.drawableCount))
    }
  }

  fun highlightDrawable(context: Context, drawable: FrescoDrawable2?) {
    removeHighlight(context)
    if (drawable == null) {
      return
    }
    val editor = ImageLiveEditor(drawable, imagePipeline, controller)
    currentEditor = editor
    editor.editOptions(context) { it.overlay(ColorDrawable(overlayColor)) }
  }

  fun removeHighlight(context: Context) = currentEditor?.editOptions(context) { it.overlay(null) }
}
