/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.net.Uri
import com.facebook.common.callercontext.ContextChain
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSources
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.PrefetchTarget
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.options.DecodedImageOptions
import com.facebook.fresco.vito.options.EncodedImageOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.imagepipeline.listener.RequestListener
import java.lang.UnsupportedOperationException

class NoOpFrescoVitoPrefetcher(private val throwException: Boolean = false) : FrescoVitoPrefetcher {

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetchToBitmapCache(
      uri: Uri,
      imageOptions: DecodedImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetchToBitmapCache(
      uri: Uri,
      imageOptions: DecodedImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetchToEncodedCache(
      uri: Uri,
      imageOptions: EncodedImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetchToEncodedCache(
      uri: Uri,
      imageOptions: EncodedImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetchToDiskCache(
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetchToDiskCache(
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      contextChain: ContextChain?,
      requestListener: RequestListener?,
      callsite: String
  ): DataSource<Void?> = maybeThrowUnsupportedOperationException()

  private fun maybeThrowUnsupportedOperationException(): DataSource<Void?> {
    if (throwException) {
      throw UnsupportedOperationException(EXCEPTION_MSG)
    }
    return FAILED_DATASOURCE
  }

  private companion object {
    private const val EXCEPTION_MSG = "Image prefetching with Fresco Vito is disabled!"
    private val FAILED_DATASOURCE: DataSource<Void?> =
        DataSources.immediateFailedDataSource<Void?>(UnsupportedOperationException(EXCEPTION_MSG))
  }
}
