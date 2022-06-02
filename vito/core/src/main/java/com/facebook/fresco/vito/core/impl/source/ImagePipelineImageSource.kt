/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.source

import android.net.Uri
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.imagepipeline.request.ImageRequest

/** Image source specific for the Fresco ImagePipeline, based on an ImageRequest. */
interface ImagePipelineImageSource : ImageSource {
  fun maybeExtractFinalImageRequest(
      imagePipelineUtils: ImagePipelineUtils,
      imageOptions: ImageOptions
  ): ImageRequest?

  fun getRequestLevelForFetch(): ImageRequest.RequestLevel = ImageRequest.RequestLevel.FULL_FETCH

  fun getUri(): Uri
}
