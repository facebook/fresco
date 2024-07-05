/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.net.Uri
import com.facebook.callercontext.CallerContextVerifier
import com.facebook.common.callercontext.ContextChain
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSources
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.PrefetchTarget
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.options.DecodedImageOptions
import com.facebook.fresco.vito.options.EncodedImageOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptions.Companion.defaults
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.request.ImageRequest
import java.lang.NullPointerException
import java.util.concurrent.CancellationException

class FrescoVitoPrefetcherImpl(
    private val imagePipeline: ImagePipeline,
    private val imagePipelineUtils: ImagePipelineUtils,
    private val callerContextVerifier: CallerContextVerifier?
) : FrescoVitoPrefetcher {

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> {
    return when (prefetchTarget) {
      PrefetchTarget.MEMORY_DECODED ->
          prefetchToBitmapCache(uri, imageOptions, callerContext, callsite)
      PrefetchTarget.MEMORY_ENCODED ->
          prefetchToEncodedCache(uri, imageOptions, callerContext, callsite)
      PrefetchTarget.DISK -> prefetchToDiskCache(uri, imageOptions, callerContext, callsite)
      else ->
          DataSources.immediateFailedDataSource(CancellationException("Prefetching is not enabled"))
    }
  }

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?> = prefetch(prefetchTarget, uri, imageOptions, callerContext, callsite)

  override fun prefetchToBitmapCache(
      uri: Uri,
      imageOptions: DecodedImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> {
    val imageRequest = imagePipelineUtils.buildImageRequest(uri, imageOptions ?: defaults())
    return prefetch(PrefetchTarget.MEMORY_DECODED, imageRequest, callerContext, null)
  }

  override fun prefetchToBitmapCache(
      uri: Uri,
      imageOptions: DecodedImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?> {
    return prefetchToBitmapCache(uri, imageOptions, callerContext, callsite)
  }

  override fun prefetchToEncodedCache(
      uri: Uri,
      imageOptions: EncodedImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> {
    val imageRequest = imagePipelineUtils.buildEncodedImageRequest(uri, imageOptions ?: defaults())
    return prefetch(PrefetchTarget.MEMORY_ENCODED, imageRequest, callerContext, null)
  }

  override fun prefetchToEncodedCache(
      uri: Uri,
      imageOptions: EncodedImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?> = prefetchToEncodedCache(uri, imageOptions, callerContext, callsite)

  override fun prefetchToDiskCache(
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      callsite: String
  ): DataSource<Void?> {
    val imageRequest = imagePipelineUtils.buildEncodedImageRequest(uri, imageOptions ?: defaults())
    return prefetch(PrefetchTarget.DISK, imageRequest, callerContext, null)
  }

  override fun prefetchToDiskCache(
      uri: Uri,
      imageOptions: ImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      callsite: String
  ): DataSource<Void?> = prefetchToDiskCache(uri, imageOptions, callerContext, callsite)

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?,
      callsite: String
  ): DataSource<Void?> =
      prefetch(prefetchTarget, imageRequest.finalImageRequest, callerContext, requestListener)

  override fun prefetch(
      prefetchTarget: PrefetchTarget,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      contextChain: ContextChain?,
      requestListener: RequestListener?,
      callsite: String
  ): DataSource<Void?> =
      prefetch(prefetchTarget, imageRequest, callerContext, requestListener, callsite)

  private fun prefetch(
      prefetchTarget: PrefetchTarget,
      imageRequest: ImageRequest?,
      callerContext: Any?,
      requestListener: RequestListener?
  ): DataSource<Void?> {
    callerContextVerifier?.verifyCallerContext(callerContext, false)
    return if (imageRequest == null) {
      DataSources.immediateFailedDataSource(NULL_IMAGE_MESSAGE)
    } else {
      when (prefetchTarget) {
        PrefetchTarget.MEMORY_DECODED ->
            imagePipeline.prefetchToBitmapCache(imageRequest, callerContext, requestListener)
        PrefetchTarget.MEMORY_ENCODED ->
            imagePipeline.prefetchToEncodedCache(imageRequest, callerContext, requestListener)
        PrefetchTarget.DISK ->
            imagePipeline.prefetchToDiskCache(imageRequest, callerContext, requestListener)
      }
    }
  }

  companion object {
    private val NULL_IMAGE_MESSAGE: Throwable = NullPointerException("No image to prefetch.")
  }
}
