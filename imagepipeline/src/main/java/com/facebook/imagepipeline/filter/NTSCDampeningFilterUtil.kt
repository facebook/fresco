/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter

import android.graphics.Color

object NTSCDampeningFilterUtil {

  @JvmStatic
  fun process(
      color: Int,
      reductionFactor: Float = DEFAULT_NTSC_REDUCTION_FACTOR,
  ): Int {
    var colorToReturn = color
    val red = Color.red(color) / 255.0
    val green = Color.green(color) / 255.0
    val blue = Color.blue(color) / 255.0

    // Calculate brightness using a perceptual NTSC formula
    val brightness = 0.299 * red + 0.587 * green + 0.114 * blue

    // Find the maximum channel value
    val maxChannel = maxOf(red, green, blue)

    // Threshold for what we consider "bright"
    val brightnessThreshold = 0.6
    val channelThreshold = 0.5

    // Consider a color bright if either the perceptual brightness is high
    // OR if any individual color channel is very high
    if (brightness > brightnessThreshold || maxChannel > channelThreshold) {
      val hsv = FloatArray(3)
      Color.colorToHSV(color, hsv)
      val hue = hsv[0]
      var saturation = hsv[1]
      var brightnessHSB = hsv[2]

      // Calculate adjustment factor based on both perceptual brightness and max channel
      val adjustmentFactor =
          maxOf(
                  (brightness - brightnessThreshold) / (1.0 - brightnessThreshold),
                  (maxChannel - channelThreshold) / (1.0 - channelThreshold),
              )
              .coerceIn(0.0, 1.0)

      // Apply a progressive brightness reduction based on how bright the color is
      val brightnessReduction = adjustmentFactor * reductionFactor
      brightnessHSB *= (1.0 - brightnessReduction).toFloat()

      // Apply a modest saturation reduction for very saturated colors
      if (saturation > 0.7) {
        val saturationReduction = adjustmentFactor * 0.2
        saturation *= (1.0 - saturationReduction).toFloat()
      }

      // Additional darkening for extremely bright colors
      if (brightnessHSB >= 0.8 || maxChannel >= 0.9) {
        brightnessHSB *= 0.95f
      }

      colorToReturn = Color.HSVToColor(floatArrayOf(hue, saturation, brightnessHSB))
    }

    return colorToReturn
  }

  private const val DEFAULT_NTSC_REDUCTION_FACTOR = 0.4f
}
