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
  ): VitoImageRequest = createImageRequest(resources, imageSource, options, false, null)

  /**
   * @param logWithHighSamplingRate The flag is a hint to loggers that they should log all events
   *   for this request at a higher sampling rate. A logger may choose to satisfy this request for
   *   example, by logging every event (sampling ratio 1:1) at the expense of potentially increased
   *   storage and compute capacity. Each logger may interpret this field differently and can choose
   *   to ignore it.
   * @return a new instance of VitoImageRequest
   */
  fun createImageRequest(
      resources: Resources,
      imageSource: ImageSource,
      options: ImageOptions?,
      logWithHighSamplingRate: Boolean = false,
      viewport: Rect? = null,
  ): VitoImageRequest

  fun getCachedImage(imageRequest: VitoImageRequest): CloseableReference<CloseableImage>?

  fun fetchDecodedImage(
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?,
      uiComponentId: Long
  ): DataSource<CloseableReference<CloseableImage>>
}
