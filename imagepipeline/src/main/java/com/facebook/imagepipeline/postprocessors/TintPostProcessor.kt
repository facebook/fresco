/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.postprocessors

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.imagepipeline.request.BasePostprocessor

/**
 * Postprocessor that adds a semi-transparent tint to a bitmap.
 *
 * By default it will add a tint over every pixel in the bitmap. A PorterDuff.Mode may be provided
 * for custom draw behavior. PorterDuff.Mode.SRC_ATOP will avoid drawing over transparent pixels.
 *
 * For example: Providing color: Color.WHITE and alpha: 0.5f will brighten the image.
 *
 * @param color The color to tint the image with. Ex: Color.WHITE, Color.BLACK
 * @param alphaPercent Sets the alpha bits of the color param for tinting, if provided. 0.0 - 1.0
 * @param porterDuffMode The PorterDuff.Mode to use for drawing, or null to draw over every pixel.
 */
class TintPostProcessor(
    @ColorInt color: Int,
    alphaPercent: Float? = null,
    private val porterDuffMode: PorterDuff.Mode? = null
) : BasePostprocessor() {
  @ColorInt
  private val tintColor: Int =
      if (alphaPercent != null) {
        ColorUtils.setAlphaComponent(color, (alphaPercent * 255).toInt().coerceIn(0, 255))
      } else {
        color
      }

  private val cacheKey: CacheKey =
      SimpleCacheKey("Tint. tintColor=$tintColor, mode=$porterDuffMode")

  override fun process(
      sourceBitmap: Bitmap,
  ) {
    if (porterDuffMode == null) {
      Canvas(sourceBitmap).drawColor(tintColor)
    } else {
      Canvas(sourceBitmap).drawColor(tintColor, porterDuffMode)
    }
  }

  override fun getName(): String = TintPostProcessor::class.toString()

  override fun getPostprocessorCacheKey(): CacheKey = cacheKey
}
