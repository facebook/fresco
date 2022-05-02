/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.renderer

import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.facebook.fresco.samples.showcase.BaseShowcaseKotlinFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.vito.VitoSpinners
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import com.facebook.fresco.vito.renderer.CircleShape
import com.facebook.fresco.vito.renderer.ColorIntImageDataModel
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.fresco.vito.renderer.PathShape
import com.facebook.fresco.vito.renderer.RectShape
import com.facebook.fresco.vito.renderer.RoundedRectShape
import java.lang.Math.min

class RendererColorFilterExampleFragment : BaseShowcaseKotlinFragment() {

  override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
    val w = 100.dpToPx()
    val h = 100.dpToPx()
    val exampleRect = Rect(0, 0, w, h)
    val exampleRectF = RectF(exampleRect)

    val shapes =
        listOf(
            RectShape(RectF(exampleRect)),
            CircleShape(
                exampleRect.exactCenterX(),
                exampleRect.exactCenterY(),
                min(exampleRect.width(), exampleRect.height()) / 2f),
            RoundedRectShape(exampleRectF, 20f.dpToPx(), 80f.dpToPx()),
            PathShape(
                Path().apply {
                  addRoundRect(
                      exampleRectF,
                      floatArrayOf(
                          10f.dpToPx(),
                          20f.dpToPx(),
                          30f.dpToPx(),
                          40f.dpToPx(),
                          50f.dpToPx(),
                          60f.dpToPx(),
                          70f.dpToPx(),
                          80f.dpToPx()),
                      Path.Direction.CW)
                }))

    container.findViewById<LinearLayout>(R.id.list).apply {
      addText("Example View dimensions: $w x $h px")
      for (colorFilter in VitoSpinners.colorFilters.first) {
        addText("Color filter ${colorFilter.first}")
        for (shape in shapes) {
          addRow {
            addExample(
                w,
                h,
                ColorIntImageDataModel(ContextCompat.getColor(context, R.color.primary)),
                shape,
                null,
                colorFilter.second)
            addExample(
                w,
                h,
                DrawableImageDataModel(
                    ContextCompat.getDrawable(requireContext(), R.mipmap.ic_launcher)!!),
                shape,
                null,
                colorFilter.second)
            addExample(
                w,
                h,
                BitmapImageDataModel(createSampleBitmap(w, h)),
                shape,
                null,
                colorFilter.second)
          }
        }
      }
    }
  }
}
