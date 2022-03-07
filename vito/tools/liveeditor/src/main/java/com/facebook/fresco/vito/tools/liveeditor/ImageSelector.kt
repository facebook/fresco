/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.content.Context
import android.graphics.drawable.ColorDrawable
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImagePipeline

class ImageSelector(
    val tracker: ImageTracker,
    private val imagePipeline: VitoImagePipeline,
    private val controller: FrescoController2,
    private val overlayColor: Int = 0x88ff0000.toInt(),
    private var currentIndex: Int = -1
) {

  var currentEditor: ImageLiveEditor? = null

  fun selectNext(context: Context) = highlightDrawable(context, incrementIndexBy(1))

  fun selectPrevious(context: Context) = highlightDrawable(context, incrementIndexBy(-1))

  fun highlightDrawable(context: Context, drawable: FrescoDrawableInterface?) {
    removeHighlight(context)
    if (drawable == null) {
      return
    }
    val editor = ImageLiveEditor(drawable, imagePipeline, controller)
    currentEditor = editor
    editor.editOptions(context) { it.overlay(ColorDrawable(overlayColor)) }
  }

  fun removeHighlight(context: Context) = currentEditor?.editOptions(context) { it.overlay(null) }

  private fun highlightDrawable(context: Context, index: Int?) {
    if (index != null) {
      highlightDrawable(context, tracker.getDrawableOrNull(index))
    }
  }

  private fun incrementIndexBy(increment: Int): Int? {
    val size = tracker.drawableCount
    if (size <= 0) {
      return null
    }
    currentIndex = (currentIndex + increment).mod(size)
    return currentIndex
  }
}
