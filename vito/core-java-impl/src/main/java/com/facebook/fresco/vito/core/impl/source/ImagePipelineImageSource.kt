/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.source

import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.UriImageSource
import com.facebook.imagepipeline.request.ImageRequest

/** Image source specific for the Fresco ImagePipeline, based on an ImageRequest. */
interface ImagePipelineImageSource : UriImageSource {
  fun maybeExtractFinalImageRequest(
      imagePipelineUtils: ImagePipelineUtils,
      imageOptions: ImageOptions
  ): ImageRequest?

  fun getRequestLevelForFetch(): ImageRequest.RequestLevel = ImageRequest.RequestLevel.FULL_FETCH
}
