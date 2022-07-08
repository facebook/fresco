/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.net.Uri
import com.facebook.fresco.vito.options.DecodedImageOptions
import com.facebook.fresco.vito.options.EncodedImageOptions
import com.facebook.imagepipeline.request.ImageRequest

interface ImagePipelineUtils {

  fun buildImageRequest(uri: Uri?, imageOptions: DecodedImageOptions): ImageRequest?

  fun wrapDecodedImageRequest(
      originalRequest: ImageRequest,
      imageOptions: DecodedImageOptions
  ): ImageRequest?

  fun buildEncodedImageRequest(uri: Uri?, imageOptions: EncodedImageOptions): ImageRequest?
}
