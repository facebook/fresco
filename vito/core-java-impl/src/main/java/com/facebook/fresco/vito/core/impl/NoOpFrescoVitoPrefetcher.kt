/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.net.Uri
import com.facebook.datasource.DataSource
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.PrefetchTarget
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.options.DecodedImageOptions
import com.facebook.fresco.vito.options.EncodedImageOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.imagepipeline.listener.RequestListener
import java.lang.UnsupportedOperationException

class NoOpFrescoVitoPrefetcher : FrescoVitoPrefetcher {

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> = throwUnsupportedOperationException()

  override fun prefetchToBitmapCache(
      uri: Uri,
      imageOptions: DecodedImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> = throwUnsupportedOperationException()

  override fun prefetchToEncodedCache(
      uri: Uri,
      imageOptions: EncodedImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> = throwUnsupportedOperationException()

  override fun prefetchToDiskCache(
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> = throwUnsupportedOperationException()

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?,
      callsite: String
  ): DataSource<Void?> = throwUnsupportedOperationException()

  override fun setDistanceToViewport(
      distance: Int,
      callerContext: Any?,
      uri: Uri?,
      callsite: String
  ) = throwUnsupportedOperationException()

  private fun throwUnsupportedOperationException(): Nothing {
    throw UnsupportedOperationException(EXCEPTION_MSG)
  }

  private companion object {
    private const val EXCEPTION_MSG = "Image prefetching with Fresco Vito is disabled!"
  }
}
