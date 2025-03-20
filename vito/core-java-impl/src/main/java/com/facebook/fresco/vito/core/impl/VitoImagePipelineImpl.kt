/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Rect
import android.os.Looper
import com.facebook.common.callercontext.ContextChain
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSources
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.urimod.ClassicFetchStrategy
import com.facebook.fresco.urimod.Dimensions
import com.facebook.fresco.urimod.FetchStrategy
import com.facebook.fresco.urimod.NoPrefetchInOnPrepareStrategy
import com.facebook.fresco.urimod.SmartFetchStrategy
import com.facebook.fresco.urimod.UriModifier
import com.facebook.fresco.urimod.UriModifierInterface
import com.facebook.fresco.urimod.asDimensions
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.impl.source.ImagePipelineImageSource
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptions.Companion.defaults
import com.facebook.fresco.vito.source.BitmapImageSource
import com.facebook.fresco.vito.source.DrawableImageSource
import com.facebook.fresco.vito.source.EmptyImageSource
import com.facebook.fresco.vito.source.FirstAvailableImageSource
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.source.IncreasingQualityImageSource
import com.facebook.fresco.vito.source.SingleImageSource
import com.facebook.fresco.vito.source.UriImageSource
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.systrace.FrescoSystrace.traceSection
import java.util.concurrent.TimeUnit

/** Vito image pipeline to fetch an image for a given VitoImageRequest. */
class VitoImagePipelineImpl(
    private val imagePipeline: ImagePipeline,
    private val imagePipelineUtils: ImagePipelineUtils,
    private val config: FrescoVitoConfig
) : VitoImagePipeline {

  override fun createImageRequest(
      resources: Resources,
      imageSource: ImageSource,
      options: ImageOptions?,
      logWithHighSamplingRate: Boolean,
      viewport: Rect?,
      callerContext: Any?,
      contextChain: ContextChain?,
      fetchStrategy: FetchStrategy?,
  ): VitoImageRequest {
    val imageOptions = options ?: defaults()
    val extras: MutableMap<String, Any> = mutableMapOf()
    var finalImageSource = imageSource

    val modifiedUriValue: String?
    when (fetchStrategy) {
      is SmartFetchStrategy -> {
        if (imageSource is UriImageSource) {
          val result: UriModifierInterface.ModificationResult =
              UriModifier.INSTANCE.modifyUri(
                  imageSource,
                  viewport?.let { Dimensions(it.width(), it.height()) },
                  imageOptions.actualImageScaleType,
                  callerContext,
                  contextChain)
          modifiedUriValue = result.toString()
          if (result is UriModifierInterface.ModificationResult.Modified) {
            finalImageSource = ImageSourceProvider.forUri(result.newUri)
            extras[HasExtraData.KEY_ORIGINAL_URL] = imageSource.imageUri
          }
        } else {
          modifiedUriValue = "NotSupportedImageSource: ${imageSource.getClassNameString()}"
        }
      }

      is ClassicFetchStrategy -> {
        modifiedUriValue = "classic" // FIXME: no smart fetch
      }

      is NoPrefetchInOnPrepareStrategy -> {
        modifiedUriValue = "noprefetch" // probably should be null (and above) FIXME
      }

      null -> {
        modifiedUriValue = null
      }
    }

    fetchStrategy?.let { extras[HasExtraData.KEY_SF_FETCH_STRATEGY] = it }
    modifiedUriValue?.let { extras[HasExtraData.KEY_SF_MOD_RESULT] = it }
    extras[HasExtraData.KEY_IMAGE_SOURCE_TYPE] = imageSource.getClassNameString()

    if (imageSource is IncreasingQualityImageSource) {
      imageSource.extras?.let { extras[HasExtraData.KEY_IMAGE_SOURCE_EXTRAS] = it }
    } else if (imageSource is UriImageSource) {
      imageSource.extras?.let { extras[HasExtraData.KEY_IMAGE_SOURCE_EXTRAS] = it }
    }

    val finalImageRequest =
        ImageSourceToImagePipelineAdapter.maybeExtractFinalImageRequest(
            finalImageSource, imagePipelineUtils, imageOptions)
    val finalImageCacheKey = finalImageRequest?.let { imagePipeline.getCacheKey(it, null) }

    return VitoImageRequest(
        resources,
        finalImageSource,
        imageOptions,
        logWithHighSamplingRate,
        finalImageRequest,
        finalImageCacheKey,
        extras,
        viewport = viewport?.asDimensions())
  }

  override fun getCachedImage(imageRequest: VitoImageRequest): CloseableReference<CloseableImage>? =
      traceSection("VitoImagePipeline#getCachedImage") {
        val cachedImageReference = imagePipeline.getCachedImage(imageRequest.finalImageCacheKey)
        if (CloseableReference.isValid(cachedImageReference)) {
          cachedImageReference
        } else {
          null
        }
      }

  override fun fetchDecodedImage(
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?,
      uiComponentId: Long,
      viewport: Dimensions?,
  ): DataSource<CloseableReference<CloseableImage>> =
      ImageSourceToImagePipelineAdapter.createDataSourceSupplier(
              imageRequest.imageSource,
              imagePipeline,
              imagePipelineUtils,
              imageRequest.imageOptions,
              callerContext,
              requestListener,
              VitoUtils.getStringId(uiComponentId),
              imageRequest.extras,
              viewport,
          )
          .get()

  override fun isInDiskCacheSync(
      vitoImageRequest: VitoImageRequest,
  ): Boolean {
    val imageRequest = vitoImageRequest.finalImageRequest ?: return false
    return imagePipeline.isInDiskCacheSync(imageRequest)
  }

  override fun isInDiskCacheSync(
      vitoImageRequest: VitoImageRequest,
      timeout: Long,
      unit: TimeUnit
  ): Boolean? {
    if (timeout <= 0) {
      return isInDiskCacheSync(vitoImageRequest)
    }
    val imageRequest = vitoImageRequest.finalImageRequest ?: return null
    return try {
      DataSources.waitForFinalResult(imagePipeline.isInDiskCache(imageRequest), timeout, unit)
    } catch (t: Throwable) {
      null
    }
  }

  override fun evictFromCaches(imageRequest: VitoImageRequest) {
    val uri = imageRequest.finalImageRequest?.sourceUri ?: return
    imagePipeline.evictFromCache(uri)
  }

  override fun hintUnmodifiedUri(imageRequest: VitoImageRequest) {
    imageRequest.finalImageRequest?.sourceUri?.let {
      UriModifier.INSTANCE.unregisterReverseFallbackUri(it)
    }
  }

  private fun experimentalDynamicSizeVito2(): Boolean = config.experimentalDynamicSizeVito2()

  private fun experimentalDynamicSizeWithCacheFallbackVito2(): Boolean =
      config.experimentalDynamicSizeWithCacheFallbackVito2()

  private fun ImageSource.getClassNameString(): String {
    return when (this) {
      is BitmapImageSource -> "BitmapImageSource"
      is DrawableImageSource -> "DrawableImageSource"
      is EmptyImageSource -> "EmptyImageSource"
      is FirstAvailableImageSource -> "FirstAvailableImageSource"
      is IncreasingQualityImageSource -> "IncreasingQualityImageSource"
      is ImagePipelineImageSource -> "ImagePipelineImageSource"
      is SingleImageSource -> "SingleImageSource"
      // Keep UriImageSource below known subclasses ImagePipelineImageSource/SingleImageSource
      is UriImageSource -> "UriImageSource"
      else -> "Other"
    }
  }

  override fun determineFetchStrategy(
      requestBeforeLayout: VitoImageRequest?,
      callerContext: Any?,
      contextChain: ContextChain?
  ): FetchStrategy {
    if (requestBeforeLayout == null) {
      if (experimentalDynamicSizeVito2()) {
        return SmartFetchStrategy.DEFAULT
      } else {
        return NoPrefetchInOnPrepareStrategy
      }
    }

    if (!experimentalDynamicSizeVito2()) {
      return ClassicFetchStrategy.APP_DISABLED
    }

    if (isProductEnabled(callerContext, contextChain) == false) {
      return ClassicFetchStrategy.PRODUCT_DISABLED
    }

    if (experimentalDynamicSizeWithCacheFallbackVito2() &&
        !isFallbackEnabled(callerContext, contextChain)) {
      return SmartFetchStrategy.FALLBACK_DISABLED
    }

    if (experimentalDynamicSizeWithCacheFallbackVito2() &&
        Looper.myLooper() == Looper.getMainLooper()) {
      // We don't want to check cache if we are running on the main thread
      // By default uses the original URL
      val shouldSmartFetchOnMainThread = config.experimentalDynamicSizeOnPrepareMainThreadVito2()
      if (shouldSmartFetchOnMainThread) {
        return SmartFetchStrategy.MAIN_THREAD
      } else {
        return ClassicFetchStrategy.MAIN_THREAD
      }
    }

    if (config.experimentalDynamicSizeDisableWhenAppIsStarting() && config.isAppStarting()) {
      return ClassicFetchStrategy.APP_STARTING
    }

    if (experimentalDynamicSizeWithCacheFallbackVito2()) {
      if (config.experimentalDynamicSizeBloksDisableDiskCacheCheck() &&
          config.isCallerContextBloks(callerContext)) {
        return SmartFetchStrategy.DEFAULT
      }

      val isInDiskCache =
          isInDiskCacheSync(
              requestBeforeLayout,
              config.experimentalDynamicSizeDiskCacheCheckTimeoutMs(),
              TimeUnit.MILLISECONDS)

      if (isInDiskCache == null) {
        // Disk cache request timed out
        if (config.experimentalDynamicSizeUseSfOnDiskCacheTimeout()) {
          return SmartFetchStrategy.DISK_CACHE_TIMEOUT
        } else {
          return ClassicFetchStrategy.DISK_CACHE_TIMEOUT
        }
      }

      if (isInDiskCache) {
        // Disk MBP fallback
        return ClassicFetchStrategy.DISK_CACHE_HIT
      }
    }

    return SmartFetchStrategy.DEFAULT
  }

  private fun isProductEnabled(callerContext: Any?, contextChain: ContextChain?): Boolean? {
    if (!config.experimentalDynamicSizeCheckIfProductIsEnabled()) {
      return null
    }
    return config.experimentalDynamicSizeIsProductEnabled(callerContext, contextChain)
  }

  private fun isFallbackEnabled(callerContext: Any?, contextChain: ContextChain?): Boolean {
    return config.experimentalDynamicSizeIsFallbackEnabled(callerContext, contextChain)
  }
}
