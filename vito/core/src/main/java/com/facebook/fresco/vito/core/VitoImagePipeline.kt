/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.content.res.Resources
import android.graphics.Rect
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.listener.RequestListener

interface VitoImagePipeline {

  fun createImageRequest(
      resources: Resources,
      imageSource: ImageSource,
      options: ImageOptions?,
  ): VitoImageRequest = createImageRequest(resources, imageSource, options, null)

  fun createImageRequest(
      resources: Resources,
      imageSource: ImageSource,
      options: ImageOptions?,
      viewport: Rect?
  ): VitoImageRequest

  fun getCachedImage(imageRequest: VitoImageRequest): CloseableReference<CloseableImage>?

  fun fetchDecodedImage(
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?,
      uiComponentId: Long
  ): DataSource<CloseableReference<CloseableImage>>
}
