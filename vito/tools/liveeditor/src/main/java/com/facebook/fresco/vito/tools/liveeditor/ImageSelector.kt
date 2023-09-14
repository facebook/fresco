/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.tools.liveeditor.LiveEditorUiUtils.Companion.dpToPx
import kotlin.Unit

class ImageSelector(
    val tracker: ImageTracker,
    private val imagePipeline: VitoImagePipeline,
    private val controller: FrescoController2,
    private val overlayColor: Int = Color.GREEN,
    private var currentIndex: Int = -1
) {

  var currentEditor: ImageLiveEditor? = null

  fun selectNext(context: Context) {
    highlightDrawable(context, incrementIndexBy(1))
  }

  fun selectPrevious(context: Context) {
    highlightDrawable(context, incrementIndexBy(-1))
  }

  fun highlightDrawable(context: Context, drawable: FrescoDrawableInterface?) {

    removeHighlight(context)

    if (drawable == null) {
      return
    }

    val editor = ImageLiveEditor(drawable, imagePipeline, controller)

    currentEditor = editor

    val overlayColorDrawable =
        ShapeDrawable().apply {
          paint?.style = Paint.Style.STROKE
          paint?.color = overlayColor
          paint?.strokeWidth = 10.dpToPx(context).toFloat()
        }

    editor.editOptions(context) { it.overlay(overlayColorDrawable) }
  }

  fun removeHighlight(context: Context): Unit? =
      currentEditor?.editOptions(context) { it.overlay(null) }

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
