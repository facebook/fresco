/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.facebook.fresco.samples.showcase.BaseShowcaseKotlinFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.vito.VitoSpinners
import com.facebook.fresco.vito.core.impl.ImageLayerDataModel
import com.facebook.fresco.vito.core.impl.getCanvasTransformation
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import com.facebook.fresco.vito.renderer.ColorIntImageDataModel
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.fresco.vito.renderer.ImageDataModel

class VitoLayerExample : BaseShowcaseKotlinFragment() {

  class ExampleImageLayerDrawable(private val imageLayerDataModel: ImageLayerDataModel) :
      Drawable() {
    override fun draw(canvas: Canvas) {
      imageLayerDataModel.configure(bounds = bounds)
      imageLayerDataModel.draw(canvas)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity(): Int = PixelFormat.TRANSPARENT
  }

  override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
    val w = 100.dpToPx()
    val h = 100.dpToPx()
    container.findViewById<LinearLayout>(R.id.list).apply {
      addText("Example View dimensions: $w x $h px")
      addScaleTypeExamples(
          w,
          h,
          "Scale Types, bitmap 160x90",
          BitmapImageDataModel(createSampleBitmap(160, 90, 127)))
      addScaleTypeExamples(
          w,
          h,
          "Scale Types, bitmap 320x90",
          BitmapImageDataModel(createSampleBitmap(320, 90, 127)))
      addScaleTypeExamples(
          w,
          h,
          "Scale Types, bitmap 90x160",
          BitmapImageDataModel(createSampleBitmap(90, 160, 127)))
      addScaleTypeExamples(
          w,
          h,
          "Scale Types, bitmap 90x320",
          BitmapImageDataModel(createSampleBitmap(90, 320, 127)))
      addScaleTypeExamples(
          w,
          h,
          "Scale Types, bitmap 800x600",
          BitmapImageDataModel(createSampleBitmap(800, 600, 127)))
      addScaleTypeExamples(w, h, "Scale Types, color int", ColorIntImageDataModel(Color.RED))
      addScaleTypeExamples(
          w,
          h,
          "Scale Types, drawable",
          DrawableImageDataModel(ContextCompat.getDrawable(context, R.drawable.logo)!!))
    }
  }

  fun LinearLayout.addScaleTypeExamples(
      w: Int,
      h: Int,
      title: String,
      imageDataModel: ImageDataModel
  ) {
    addRow(title, true) {
      for (entry in VitoSpinners.scaleTypes.first) {
        addLayerExample(
            entry.first,
            w,
            h,
            ImageLayerDataModel().apply {
              configure(
                  dataModel = imageDataModel,
                  canvasTransformation =
                      entry.second.first?.getCanvasTransformation(entry.second.second),
                  roundingOptions = RoundingOptions.asCircle())
            })
      }
    }
  }

  fun LinearLayout.addLayerExample(
      title: String,
      w: Int,
      h: Int,
      imageLayerDataModel: ImageLayerDataModel
  ) {
    addImageViewWithText(title, w, h) {
      setImageDrawable(ExampleImageLayerDrawable(imageLayerDataModel))
    }
  }
}
