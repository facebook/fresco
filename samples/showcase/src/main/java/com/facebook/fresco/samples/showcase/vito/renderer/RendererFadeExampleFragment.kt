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
import com.facebook.fresco.vito.core.impl.ImageLayerDataModel
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import com.facebook.fresco.vito.renderer.ColorIntImageDataModel
import com.facebook.fresco.vito.renderer.DrawableImageDataModel

private const val FADE_DURATION = 1000

class RendererFadeExampleFragment : BaseShowcaseKotlinFragment() {

  class FadingDrawable(private val imageLayerDataModel: ImageLayerDataModel) : Drawable() {
    override fun draw(canvas: Canvas) {
      imageLayerDataModel.configure(bounds = bounds)
      imageLayerDataModel.draw(canvas)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity(): Int = PixelFormat.TRANSPARENT
  }

  override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
    super.onViewCreated(container, savedInstanceState)

    val w = 300.dpToPx()
    val h = 300.dpToPx()
    val bitmapImageDataModel =
        ImageLayerDataModel().also {
          it.configure(dataModel = BitmapImageDataModel(createSampleBitmap(w, h)))
        }

    val drawableImageDataModel =
        ImageLayerDataModel().also {
          it.configure(
              dataModel =
                  DrawableImageDataModel(
                      ContextCompat.getDrawable(requireContext(), R.drawable.logo)!!))
        }

    val colorImageDataModel =
        ImageLayerDataModel().also { it.configure(dataModel = ColorIntImageDataModel(Color.RED)) }

    container.findViewById<LinearLayout>(R.id.list).apply {
      addFadingExampleWithImageData(
          this, bitmapImageDataModel, "Bitmap: Click to Fade In&Out Bitmap", w, h)
      addFadingExampleWithImageData(
          this, drawableImageDataModel, "Drawable: Click to Fade In&Out Bitmap", w, h)
      addFadingExampleWithImageData(
          this, colorImageDataModel, "Color: Click to Fade In&Out Bitmap", w, h)
    }
  }

  private fun addFadingExampleWithImageData(
      linearLayout: LinearLayout,
      imageDataModel: ImageLayerDataModel,
      title: String,
      w: Int,
      h: Int
  ) {
    linearLayout
        .addImageViewWithText(title, w, h) {
          val drawable = FadingDrawable(imageDataModel)
          imageDataModel.invalidateLayerCallback = { drawable.invalidateSelf() }
          setImageDrawable(drawable)
        }
        .setOnClickListener {
          if (imageDataModel.getAlpha() == 0) {
            imageDataModel.fadeIn(FADE_DURATION)
          } else {
            imageDataModel.fadeOut(FADE_DURATION)
          }
        }
  }
}
