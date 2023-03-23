/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Rect
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.urimod.Dimensions
import com.facebook.fresco.urimod.UriModifier
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptions.Companion.defaults
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.source.SingleImageSource
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.systrace.FrescoSystrace

/** Vito image pipeline to fetch an image for a given VitoImageRequest. */
class VitoImagePipelineImpl(
    private val imagePipeline: ImagePipeline,
    private val imagePipelineUtils: ImagePipelineUtils
) : VitoImagePipeline {

  override fun createImageRequest(
      resources: Resources,
      imageSource: ImageSource,
      options: ImageOptions?,
      viewport: Rect?
  ): VitoImageRequest {
    val imageOptions = options ?: defaults()
    val extras: MutableMap<String, Any?> = mutableMapOf()
    var finalImageSource = imageSource
    if (imageSource is SingleImageSource) {
      val uri = imageSource.uri
      val maybeModifiedUri =
          UriModifier.INSTANCE.modifyUri(
              uri,
              viewport?.let { Dimensions(it.width(), it.height()) },
              imageOptions.actualImageScaleType)

      if (maybeModifiedUri != uri) {
        extras[HasExtraData.KEY_MODIFIED_URL] = true
        finalImageSource = ImageSourceProvider.forUri(maybeModifiedUri)
      }
      if (imageSource.extras != null) {
        extras[HasExtraData.KEY_IMAGE_SOURCE_EXTRAS] = imageSource.extras
      }
    }

    val finalImageRequest =
        ImageSourceToImagePipelineAdapter.maybeExtractFinalImageRequest(
            finalImageSource, imagePipelineUtils, imageOptions)
    val finalImageCacheKey = finalImageRequest?.let { imagePipeline.getCacheKey(it, null) }

    return VitoImageRequest(
        resources, finalImageSource, imageOptions, finalImageRequest, finalImageCacheKey, extras)
  }

  override fun getCachedImage(imageRequest: VitoImageRequest): CloseableReference<CloseableImage>? {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("VitoImagePipeline#getCachedImage")
    }

    return try {
      val cachedImageReference = imagePipeline.getCachedImage(imageRequest.finalImageCacheKey)
      if (CloseableReference.isValid(cachedImageReference)) {
        cachedImageReference
      } else {
        null
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection()
      }
    }
  }

  override fun fetchDecodedImage(
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?,
      uiComponentId: Long
  ): DataSource<CloseableReference<CloseableImage>> =
      ImageSourceToImagePipelineAdapter.createDataSourceSupplier(
              imageRequest.imageSource,
              imagePipeline,
              imagePipelineUtils,
              imageRequest.imageOptions,
              callerContext,
              requestListener,
              VitoUtils.getStringId(uiComponentId),
              imageRequest.extras)
          .get()
}
