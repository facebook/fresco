/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.renderer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.facebook.fresco.samples.showcase.common.dpToPx
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.fresco.vito.renderer.Shape

fun createSampleBitmap(w: Int, h: Int, blue: Int = 0) =
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
      for (x in 0 until w) {
        for (y in 0 until h) {
          setPixel(x, y, Color.rgb(255 * x / w, 255 * y / h, blue))
        }
      }
    }

fun LinearLayout.createLinearLayout(
    isHorizontal: Boolean = false,
    block: LinearLayout.() -> Unit
): LinearLayout {
  return LinearLayout(context).apply {
    this.orientation = if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
    layoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    block()
  }
}

fun LinearLayout.addImageViewWithText(
    title: String,
    w: Int,
    h: Int,
    padding: Int = 0,
    block: ImageView.() -> Unit
): LinearLayout {
  val ll = createLinearLayout {
    addText(title)
    addImageView(w, h, padding).block()
  }
  addView(ll)
  return ll
}

fun LinearLayout.addImageView(w: Int, h: Int, padding: Int = 0): ImageView {
  val margin = 8.dpToPx(context)
  val view =
      ImageView(context).apply {
        layoutParams =
            LinearLayout.LayoutParams(w, h).apply {
              setMargins(margin, margin, margin, margin)
              setPadding(padding, padding, padding, padding)
              setBackgroundDrawable(CheckerBoardDrawable(resources))
            }
      }
  addView(view)
  return view
}

fun LinearLayout.addRow(
    text: String? = null,
    scrollHorizontally: Boolean = false,
    block: LinearLayout.() -> Unit
) {
  addText(text)
  addView(
      if (scrollHorizontally) {
        HorizontalScrollView(context).apply {
          layoutParams =
              LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
          addView(createLinearLayout(true, block))
        }
      } else {
        createLinearLayout(true, block)
      })
}

fun LinearLayout.addExample(
    w: Int,
    h: Int,
    imageDataModel: ImageDataModel,
    shape: Shape,
    transformationMatrix: Matrix? = null,
    colorFilter: ColorFilter? = null,
) {
  addImageView(w, h)
      .setImageDrawable(
          RendererExampleDrawable(imageDataModel, shape, transformationMatrix, colorFilter))
}

fun LinearLayout.addText(text: String?) {
  val padding = 8.dpToPx(context)
  if (text != null) {
    addView(
        TextView(context).apply {
          this.text = text
          layoutParams =
              LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
          setPadding(padding, padding, padding, padding)
        })
  }
}
