/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.net.Uri
import com.facebook.callercontext.CallerContextVerifier
import com.facebook.datasource.DataSource
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.PrefetchTarget
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.DrawableResImageSource
import com.facebook.fresco.vito.source.SingleImageSourceImpl
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.request.ImageRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FrescoVitoPrefetcherImplTest {

  private val imagePipeline: ImagePipeline = mock()
  private val imagePipelineUtils: ImagePipelineUtils = mock()
  private val bitmapResult: DataSource<Void?> = mock()
  private val encodedResult: DataSource<Void?> = mock()
  private val diskResult: DataSource<Void?> = mock()
  private val callerContextVerifier = CallerContextVerifier { _, _ -> }

  private lateinit var prefetcher: FrescoVitoPrefetcherImpl

  @Before
  fun setUp() {
    prefetcher = FrescoVitoPrefetcherImpl(imagePipeline, imagePipelineUtils, callerContextVerifier)
  }

  @After
  fun tearDown() {
    DrawableResImageSource.isPrefetchEnabled = false
    DrawableResImageSource.customPrefetchFunction = null
  }

  @Test
  fun prefetch_memoryDecodedWithNullOptions_buildsDefaultRequest() {
    val uri = Uri.parse("https://example.com/decoded.jpg")
    val imageRequest = requestFor(uri)
    whenever(imagePipelineUtils.buildImageRequest(uri, ImageOptions.defaults()))
        .thenReturn(imageRequest)
    whenever(
        imagePipeline.prefetchToBitmapCache(
            imageRequest = imageRequest,
            callerContext = null,
            requestListener = null,
            extras = null,
        ),
    )
        .thenReturn(bitmapResult)

    val result = prefetcher.prefetch(PrefetchTarget.MEMORY_DECODED, uri, null, null, "feed")

    assertThat(result).isSameAs(bitmapResult)
  }

  @Test
  fun prefetch_memoryEncodedWithOptions_returnsEncodedCacheResult() {
    val uri = Uri.parse("https://example.com/encoded.jpg")
    val imageOptions = ImageOptions.create().build()
    val imageRequest = requestFor(uri)
    val callerContext = Any()
    whenever(imagePipelineUtils.buildEncodedImageRequest(uri, imageOptions))
        .thenReturn(imageRequest)
    whenever(
        imagePipeline.prefetchToEncodedCache(
            imageRequest = imageRequest,
            callerContext = callerContext,
            requestListener = null,
            extras = null,
        ),
    )
        .thenReturn(encodedResult)

    val result =
        prefetcher.prefetch(
            PrefetchTarget.MEMORY_ENCODED,
            uri,
            imageOptions,
            callerContext,
            "notification",
        )

    assertThat(result).isSameAs(encodedResult)
  }

  @Test
  fun prefetch_diskWithOptions_returnsDiskCacheResultAtMediumPriority() {
    val uri = Uri.parse("https://example.com/disk.jpg")
    val imageOptions = ImageOptions.create().build()
    val imageRequest = requestFor(uri)
    val callerContext = Any()
    whenever(imagePipelineUtils.buildEncodedImageRequest(uri, imageOptions))
        .thenReturn(imageRequest)
    whenever(
        imagePipeline.prefetchToDiskCache(
            imageRequest = imageRequest,
            callerContext = callerContext,
            priority = Priority.MEDIUM,
            requestListener = null,
            extras = null,
        ),
    )
        .thenReturn(diskResult)

    val result =
        prefetcher.prefetch(PrefetchTarget.DISK, uri, imageOptions, callerContext, "gallery")

    assertThat(result).isSameAs(diskResult)
  }

  @Test
  fun prefetch_imageRequest_routesEveryTargetToMatchingCache() {
    val imageRequest = requestFor(Uri.parse("https://example.com/direct.jpg"))
    whenever(
        imagePipeline.prefetchToBitmapCache(
            imageRequest = imageRequest,
            callerContext = null,
            requestListener = null,
            extras = null,
        ),
    )
        .thenReturn(bitmapResult)
    whenever(
        imagePipeline.prefetchToEncodedCache(
            imageRequest = imageRequest,
            callerContext = null,
            requestListener = null,
            extras = null,
        ),
    )
        .thenReturn(encodedResult)
    whenever(
        imagePipeline.prefetchToDiskCache(
            imageRequest = imageRequest,
            callerContext = null,
            priority = Priority.MEDIUM,
            requestListener = null,
            extras = null,
        ),
    )
        .thenReturn(diskResult)

    val results =
        PrefetchTarget.entries.map { target ->
          prefetcher.prefetch(target, imageRequest, null, null, "direct")
        }

    assertThat(results).containsExactly(bitmapResult, encodedResult, diskResult)
  }

  @Test
  fun prefetch_vitoRequest_forwardsListenerAndExtrasToDiskCache() {
    val uri = Uri.parse("https://example.com/vito.jpg")
    val imageRequest = requestFor(uri)
    val callerContext = Any()
    val requestListener: RequestListener = mock()
    val extras = mutableMapOf<String, Any>("request_id" to "vito-prefetch")
    val vitoRequest = vitoRequest(SingleImageSourceImpl(uri), imageRequest, extras)
    whenever(
        imagePipeline.prefetchToDiskCache(
            imageRequest = imageRequest,
            callerContext = callerContext,
            priority = Priority.MEDIUM,
            requestListener = requestListener,
            extras = extras,
        ),
    )
        .thenReturn(diskResult)

    val result =
        prefetcher.prefetch(
            PrefetchTarget.DISK,
            vitoRequest,
            callerContext,
            requestListener,
            "vito",
        )

    assertThat(result).isSameAs(diskResult)
  }

  @Test
  fun prefetch_vitoRequestWithNullFinalRequest_returnsFailedDataSource() {
    val uri = Uri.parse("https://example.com/missing.jpg")
    val callerContext = Any()
    val vitoRequest = vitoRequest(SingleImageSourceImpl(uri), null)

    val result =
        prefetcher.prefetch(PrefetchTarget.MEMORY_DECODED, vitoRequest, callerContext, null, "vito")

    assertThat(result.isFinished).isTrue()
    assertThat(result.hasFailed()).isTrue()
    assertThat(result.failureCause)
        .isInstanceOf(NullPointerException::class.java)
        .hasMessage("No image to prefetch.")
  }

  @Test
  fun prefetch_drawableResource_prefetchesDrawableWithoutImagePipelineRequest() {
    val resources = RuntimeEnvironment.getApplication().resources
    val imageSource = DrawableResImageSource(android.R.drawable.btn_star)
    val vitoRequest = vitoRequest(imageSource, null)
    var prefetchedResources: Resources? = null
    var prefetchedResourceId: Int? = null
    DrawableResImageSource.customPrefetchFunction = { receivedResources, resourceId ->
      prefetchedResources = receivedResources
      prefetchedResourceId = resourceId
    }

    val result = prefetcher.prefetch(PrefetchTarget.DISK, vitoRequest, null, null, "drawable")

    assertThat(prefetchedResources).isSameAs(resources)
    assertThat(prefetchedResourceId).isEqualTo(android.R.drawable.btn_star)
    assertThat(result.isFinished).isTrue()
    assertThat(result.hasFailed()).isFalse()
  }

  @Test
  fun prefetch_whenCallerContextVerifierRejects_propagatesFailure() {
    val expectedFailure = IllegalArgumentException("invalid caller context")
    val rejectingPrefetcher =
        FrescoVitoPrefetcherImpl(imagePipeline, imagePipelineUtils) { _, _ ->
          throw expectedFailure
        }
    val imageRequest = requestFor(Uri.parse("https://example.com/rejected.jpg"))

    assertThatThrownBy {
          rejectingPrefetcher.prefetch(PrefetchTarget.MEMORY_DECODED, imageRequest, null, null, "x")
        }
        .isSameAs(expectedFailure)
  }

  private fun requestFor(uri: Uri): ImageRequest = checkNotNull(ImageRequest.fromUri(uri))

  private fun vitoRequest(
      imageSource: com.facebook.fresco.vito.source.ImageSource,
      finalImageRequest: ImageRequest?,
      extras: MutableMap<String, Any> = mutableMapOf(),
  ): VitoImageRequest = VitoImageRequest(
      resources = RuntimeEnvironment.getApplication().resources,
      imageSource = imageSource,
      imageOptions = ImageOptions.defaults(),
      finalImageRequest = finalImageRequest,
      finalImageCacheKey = null,
      extras = extras,
  )
}
