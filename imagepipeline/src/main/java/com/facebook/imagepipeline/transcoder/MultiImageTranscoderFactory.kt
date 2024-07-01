/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.core.ImageTranscoderType
import com.facebook.imagepipeline.core.NativeCodeSetup
import com.facebook.imagepipeline.nativecode.NativeImageTranscoderFactory

/**
 * Class responsible of returning the correct [ImageTranscoder] given the [ImageFormat]. The custom
 * [ImageTranscoder], if any, will always be used first. If the image format is not supported, the
 * first fallback is NativeJpegTranscoder, otherwise [SimpleImageTranscoder] is used.
 */
class MultiImageTranscoderFactory(
    private val maxBitmapSize: Int,
    private val useDownSamplingRatio: Boolean,
    private val primaryImageTranscoderFactory: ImageTranscoderFactory?,
    @field:ImageTranscoderType @param:ImageTranscoderType private val imageTranscoderType: Int?,
    private val ensureTranscoderLibraryLoaded: Boolean
) : ImageTranscoderFactory {

  override fun createImageTranscoder(
      imageFormat: ImageFormat,
      isResizingEnabled: Boolean
  ): ImageTranscoder {
    // Use custom ImageTranscoder, if any
    var imageTranscoder = getCustomImageTranscoder(imageFormat, isResizingEnabled)
    // Use ImageTranscoder based on type passed, if any
    if (imageTranscoder == null) {
      imageTranscoder = getImageTranscoderWithType(imageFormat, isResizingEnabled)
    }
    // First fallback using native ImageTranscoder
    if (imageTranscoder == null && NativeCodeSetup.getUseNativeCode()) {
      imageTranscoder = getNativeImageTranscoder(imageFormat, isResizingEnabled)
    }

    // Fallback to SimpleImageTranscoder if the format is not supported by native ImageTranscoder
    return imageTranscoder ?: getSimpleImageTranscoder(imageFormat, isResizingEnabled)
  }

  private fun getCustomImageTranscoder(
      imageFormat: ImageFormat,
      isResizingEnabled: Boolean
  ): ImageTranscoder? =
      primaryImageTranscoderFactory?.createImageTranscoder(imageFormat, isResizingEnabled)

  private fun getNativeImageTranscoder(
      imageFormat: ImageFormat,
      isResizingEnabled: Boolean
  ): ImageTranscoder? =
      NativeImageTranscoderFactory.getNativeImageTranscoderFactory(
              maxBitmapSize, useDownSamplingRatio, ensureTranscoderLibraryLoaded)
          .createImageTranscoder(imageFormat, isResizingEnabled)

  private fun getSimpleImageTranscoder(
      imageFormat: ImageFormat,
      isResizingEnabled: Boolean
  ): ImageTranscoder =
      SimpleImageTranscoderFactory(maxBitmapSize)
          .createImageTranscoder(imageFormat, isResizingEnabled)

  private fun getImageTranscoderWithType(
      imageFormat: ImageFormat,
      isResizingEnabled: Boolean
  ): ImageTranscoder? =
      if (imageTranscoderType == null) {
        null
      } else {
        when (imageTranscoderType) {
          ImageTranscoderType.NATIVE_TRANSCODER ->
              getNativeImageTranscoder(imageFormat, isResizingEnabled)
          ImageTranscoderType.JAVA_TRANSCODER ->
              getSimpleImageTranscoder(imageFormat, isResizingEnabled)
          else -> throw IllegalArgumentException("Invalid ImageTranscoderType")
        }
      }
}
