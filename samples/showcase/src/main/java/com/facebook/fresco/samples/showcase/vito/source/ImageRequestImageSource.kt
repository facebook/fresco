/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.source

import android.net.Uri
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.impl.source.ImagePipelineImageSource
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequest.RequestLevel

/** Custom ImageSource that takes an ImageRequest for the ImagePipeline */
data class ImageRequestImageSource(private val imageRequest: ImageRequest) :
    ImagePipelineImageSource {
  override fun maybeExtractFinalImageRequest(
      imagePipelineUtils: ImagePipelineUtils,
      imageOptions: ImageOptions
  ): ImageRequest? {
    return imagePipelineUtils.wrapDecodedImageRequest(imageRequest, imageOptions)
  }

  override val imageUri: Uri = imageRequest.sourceUri

  override val extras: Map<String, Any> = emptyMap()

  override fun getRequestLevelForFetch(): RequestLevel = RequestLevel.FULL_FETCH
}
