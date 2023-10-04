/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Not-in-place rounding image algorithm using [Canvas] that requires an Android version >= 3.1. If
 * image quality is preferred above performance, this algorithm performs anti-aliasing and will
 * generate better looking images, otherwise clients that look for better performances should use
 * NativeRoundingFilter.
 */
object XferRoundFilter {
  @JvmStatic
  fun xferRoundBitmap(output: Bitmap, source: Bitmap, enableAntiAliasing: Boolean) {
    checkNotNull(source)
    checkNotNull(output)
    output.setHasAlpha(true)
    val circlePaint: Paint
    val xfermodePaint: Paint
    if (enableAntiAliasing) {
      circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
      xfermodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    } else {
      circlePaint = Paint()
      xfermodePaint = Paint()
    }
    circlePaint.color = Color.BLACK
    xfermodePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    val xCenter = source.width / 2f
    val yCenter = source.height / 2f
    Canvas(output).apply {
      drawCircle(xCenter, yCenter, Math.min(xCenter, yCenter), circlePaint)
      drawBitmap(source, 0f, 0f, xfermodePaint)
    }
  }
}
