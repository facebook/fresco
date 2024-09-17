/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.net.Uri
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.options.DecodedImageOptions
import com.facebook.fresco.vito.options.EncodedImageOptions
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder

/**
 * Utility methods to create [ImageRequest]s for [com.facebook.fresco.vito.options.ImageOptions].
 */
class ImagePipelineUtilsImpl(private val imageDecodeOptionsProvider: ImageDecodeOptionsProvider) :
    ImagePipelineUtils {

  fun interface CircularBitmapRounding {
    fun getDecodeOptions(antiAliased: Boolean): ImageDecodeOptions?
  }

  fun interface ImageDecodeOptionsProvider {
    fun create(
        imageRequestBuilder: ImageRequestBuilder,
        imageOptions: DecodedImageOptions
    ): ImageDecodeOptions?
  }

  override fun buildImageRequest(uri: Uri?, imageOptions: DecodedImageOptions): ImageRequest? {
    if (uri == null) {
      return null
    }
    return createDecodedImageRequestBuilder(
            createEncodedImageRequestBuilder(uri, imageOptions), imageOptions)
        ?.build()
  }

  override fun wrapDecodedImageRequest(
      originalRequest: ImageRequest,
      imageOptions: DecodedImageOptions
  ): ImageRequest? =
      createDecodedImageRequestBuilder(
              createEncodedImageRequestBuilder(originalRequest, imageOptions), imageOptions)
          ?.build()

  override fun buildEncodedImageRequest(
      uri: Uri?,
      imageOptions: EncodedImageOptions
  ): ImageRequest? = createEncodedImageRequestBuilder(uri, imageOptions)?.build()

  protected fun createDecodedImageRequestBuilder(
      imageRequestBuilder: ImageRequestBuilder?,
      imageOptions: DecodedImageOptions
  ): ImageRequestBuilder? =
      imageRequestBuilder?.apply {
        imageOptions.resizeOptions?.let { resizeOptions = it }
        imageOptions.downsampleOverride?.let { downsampleOverride = it }
        imageOptions.rotationOptions?.let { rotationOptions = it }
        imageDecodeOptionsProvider.create(imageRequestBuilder, imageOptions)?.let {
          imageDecodeOptions = it
        }
        isLocalThumbnailPreviewsEnabled = imageOptions.areLocalThumbnailPreviewsEnabled()
        loadThumbnailOnly = imageOptions.loadThumbnailOnly
        imageOptions.postprocessor?.let { postprocessor = it }
        imageOptions.isProgressiveDecodingEnabled?.let { isProgressiveRenderingEnabled = it }
      }

  protected fun createEncodedImageRequestBuilder(
      uri: Uri?,
      imageOptions: EncodedImageOptions
  ): ImageRequestBuilder? {
    if (uri == null) {
      return null
    }
    val builder = ImageRequestBuilder.newBuilderWithSource(uri)
    maybeSetRequestPriority(builder, imageOptions.priority)
    if (imageOptions.cacheChoice != null) {
      builder.cacheChoice = imageOptions.cacheChoice
    }
    if (imageOptions.diskCacheId != null) {
      builder.diskCacheId = imageOptions.diskCacheId
    }
    return builder
  }

  protected fun createEncodedImageRequestBuilder(
      imageRequest: ImageRequest?,
      imageOptions: EncodedImageOptions
  ): ImageRequestBuilder? {
    if (imageRequest == null) {
      return null
    }
    val builder = ImageRequestBuilder.fromRequest(imageRequest)
    maybeSetRequestPriority(builder, imageOptions.priority)
    return builder
  }

  companion object {
    private fun maybeSetRequestPriority(builder: ImageRequestBuilder, priority: Priority?) {
      if (priority != null) {
        builder.requestPriority = priority
      }
    }
  }
}
