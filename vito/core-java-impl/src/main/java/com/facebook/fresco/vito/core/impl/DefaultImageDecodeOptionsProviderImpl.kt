/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Bitmap
import android.os.Build
import com.facebook.common.logging.FLog
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl.CircularBitmapRounding
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl.ImageDecodeOptionsProvider
import com.facebook.fresco.vito.options.DecodedImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder

class DefaultImageDecodeOptionsProviderImpl(
    private val circularBitmapRounding: CircularBitmapRounding?
) : ImageDecodeOptionsProvider {

  override fun create(
      imageRequestBuilder: ImageRequestBuilder,
      imageOptions: DecodedImageOptions
  ): ImageDecodeOptions? =
      maybeCreateFromConfigAndCustomDecoder(imageOptions)
          ?: maybeSetupPipelineRounding(
              imageOptions.roundingOptions, imageOptions.bitmapConfig, circularBitmapRounding)

  companion object {
    private const val TAG = "DefaultImageOptionsProvider"

    @JvmStatic
    fun maybeCreateFromConfigAndCustomDecoder(
        imageOptions: DecodedImageOptions
    ): ImageDecodeOptions? {
      val bitmapConfig = imageOptions.bitmapConfig
      val imageDecodeOptions = imageOptions.imageDecodeOptions
      if (bitmapConfig != null) {
        if (imageOptions.roundingOptions != null || imageOptions.postprocessor != null) {
          FLog.wtf(TAG, "Trying to use bitmap config incompatible with rounding.")
        } else {
          return ImageDecodeOptions.newBuilder()
              .setBitmapConfig(bitmapConfig)
              .setCustomImageDecoder(imageOptions.imageDecodeOptions?.customImageDecoder)
              .build()
        }
      } else if (imageDecodeOptions?.customImageDecoder != null) {
        return ImageDecodeOptions.newBuilder()
            .setCustomImageDecoder(imageDecodeOptions.customImageDecoder)
            .build()
      }

      return null
    }

    @JvmStatic
    fun maybeSetupPipelineRounding(
        roundingOptions: RoundingOptions?,
        bitmapConfig: Bitmap.Config?,
        circularBitmapRounding: CircularBitmapRounding?
    ): ImageDecodeOptions? =
        if (roundingOptions == null ||
            roundingOptions.isForceRoundAtDecode ||
            !roundingOptions.isCircular ||
            circularBitmapRounding == null ||
            pipelineRoundingUnsupportedForBitmapConfig(bitmapConfig)) {
          null
        } else {
          circularBitmapRounding.getDecodeOptions(roundingOptions.isAntiAliased)
        }

    private fun pipelineRoundingUnsupportedForBitmapConfig(bitmapConfig: Bitmap.Config?): Boolean =
        bitmapConfig == Bitmap.Config.RGB_565 ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                bitmapConfig == Bitmap.Config.HARDWARE)
  }
}
