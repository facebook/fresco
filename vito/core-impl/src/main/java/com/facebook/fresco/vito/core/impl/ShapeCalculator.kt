/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Path
import android.graphics.RectF
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.fresco.vito.renderer.CircleShape
import com.facebook.fresco.vito.renderer.PathShape
import com.facebook.fresco.vito.renderer.RectShape
import com.facebook.fresco.vito.renderer.RoundedRectShape
import com.facebook.fresco.vito.renderer.Shape
import kotlin.math.min

class ShapeCalculator {
  companion object {
    fun getShape(
        bounds: RectF,
        roundingOptions: RoundingOptions?,
        cornerRadiusAdjustment: Float = 0f
    ): Shape {
      return when {
        roundingOptions == null -> RectShape(bounds)
        roundingOptions.isCircular -> {
          CircleShape(
              bounds.centerX(),
              bounds.centerY(),
              min(bounds.width(), bounds.height()) / 2f,
              roundingOptions.isAntiAliased)
        }
        roundingOptions.hasRoundedCorners() -> {
          when {
            roundingOptions.cornerRadius != RoundingOptions.CORNER_RADIUS_UNSET -> {
              val radius = roundingOptions.cornerRadius + cornerRadiusAdjustment
              RoundedRectShape(bounds, radius, radius)
            }
            else -> {
              var radii = roundingOptions.cornerRadii
              if (radii != null && cornerRadiusAdjustment != 0f) {
                val insideBorderRadii = FloatArray(8)
                for (i in radii.indices) {
                  insideBorderRadii[i] = radii[i] + cornerRadiusAdjustment
                }
                radii = insideBorderRadii
              }
              if (radii == null) {
                throw IllegalArgumentException("Malformed rounding options $roundingOptions")
              }
              PathShape(
                  Path().apply {
                    addRoundRect(bounds, radii, Path.Direction.CW)
                    fillType = Path.FillType.WINDING
                  })
            }
          }
        }
        else -> RectShape(bounds)
      }
    }
  }
}
