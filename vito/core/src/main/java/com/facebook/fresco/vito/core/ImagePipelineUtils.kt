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
import com.facebook.fresco.vito.source.SingleImageSource
import com.facebook.imagepipeline.request.ImageRequest

interface ImagePipelineUtils {

  fun buildImageRequest(uri: Uri?, imageOptions: DecodedImageOptions): ImageRequest?

  /**
   * Builds the image request for a whole [SingleImageSource] rather than just its URI.
   *
   * An app that defines its own [SingleImageSource] subtype can carry more than a URI on it — a
   * typed URL, cache-key material, decode hints — and override this to read that state when
   * building the request. The default keeps the URI-only behaviour, so existing implementations are
   * unaffected.
   *
   * Both the fetch path and the cache-key path go through this method, so an override applies to
   * each consistently.
   */
  fun buildImageRequest(
      imageSource: SingleImageSource,
      imageOptions: DecodedImageOptions,
  ): ImageRequest? = buildImageRequest(imageSource.imageUri, imageOptions)

  fun wrapDecodedImageRequest(
      originalRequest: ImageRequest,
      imageOptions: DecodedImageOptions,
  ): ImageRequest?

  fun buildEncodedImageRequest(uri: Uri?, imageOptions: EncodedImageOptions): ImageRequest?
}
