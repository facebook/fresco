/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import android.net.Uri
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.imagepipeline.common.ImageDecodeOptionsBuilder
import com.facebook.imagepipeline.request.ImageRequest

/**
 * A single cache key factory that can serve multiple apps (FB4A, IG, etc.) with different
 * configurations via [UriNormalizer] and [CacheKeyConfig].
 *
 * This replaces app-specific subclasses of [DefaultCacheKeyFactory] with one composable
 * implementation.
 */
class UnifiedCacheKeyFactory(
    private val uriNormalizer: UriNormalizer,
    private val config: CacheKeyConfig,
) : CacheKeyFactory {

  override fun getBitmapCacheKey(request: ImageRequest, callerContext: Any?): CacheKey {
    return createBitmapCacheKey(request, callerContext, null, null)
  }

  override fun getPostprocessedBitmapCacheKey(
      request: ImageRequest,
      callerContext: Any?,
  ): CacheKey {
    val postprocessor = request.postprocessor
    return if (postprocessor != null) {
      createBitmapCacheKey(
          request,
          callerContext,
          postprocessor.postprocessorCacheKey,
          postprocessor.javaClass.name,
      )
    } else {
      createBitmapCacheKey(request, callerContext, null, null)
    }
  }

  override fun getEncodedCacheKey(request: ImageRequest, callerContext: Any?): CacheKey {
    return getEncodedCacheKey(request, request.sourceUri, callerContext)
  }

  override fun getEncodedCacheKey(
      request: ImageRequest,
      sourceUri: Uri,
      callerContext: Any?,
  ): CacheKey {
    val customCacheKey = request.customCacheKey
    if (customCacheKey != null) {
      return config.encodedKeyEnricher?.enrich(customCacheKey, callerContext, sourceUri)
          ?: SimpleCacheKey(customCacheKey)
    }

    val normalized = uriNormalizer.normalize(sourceUri, callerContext)
    var keyString = normalized.cacheKeyString

    if (config.includeDimensionsInEncodedKey) {
      val dims = config.dimensionExtractor?.extractDimensions(request, sourceUri, callerContext)
      if (dims != null) {
        keyString =
            if (config.enableDiskSimilarity && normalized.groupKey != null) {
              "${normalized.groupKey}_${dims.first}_${dims.second}"
            } else {
              "${keyString}${dims.first}_${dims.second}"
            }
      }
    }

    if (keyString.length >= config.hashThreshold) {
      keyString = keyString.hashCode().toString()
    }

    return config.encodedKeyEnricher?.enrich(keyString, callerContext, sourceUri)
        ?: SimpleCacheKey(keyString)
  }

  private fun createBitmapCacheKey(
      request: ImageRequest,
      callerContext: Any?,
      postprocessorCacheKey: CacheKey?,
      postprocessorName: String?,
  ): CacheKey {
    val customKey = request.customCacheKey
    var sourceString =
        customKey ?: uriNormalizer.normalize(request.sourceUri, callerContext).cacheKeyString

    // Only append dimensions for URI-derived keys, not customCacheKey.
    // customCacheKey is caller-provided and used as-is (matching DefaultCacheKeyFactory behavior).
    if (customKey == null && config.includeDimensionsInBitmapKey) {
      val dims =
          config.dimensionExtractor?.extractDimensions(request, request.sourceUri, callerContext)
      if (dims != null) {
        sourceString = "${sourceString}${dims.first}_${dims.second}"
      }
    }

    if (customKey == null && sourceString.length >= config.hashThreshold) {
      sourceString = sourceString.hashCode().toString()
    }

    val resizeOptions =
        config.resizeOptionsFilter?.invoke(request.sourceUriType, request.resizeOptions)
            ?: request.resizeOptions

    val decodeOptions =
        if (config.excludeBitmapConfigFromComparison) {
          ImageDecodeOptionsBuilder<ImageDecodeOptionsBuilder<*>>()
              .setFrom(request.imageDecodeOptions)
              .setExcludeBitmapConfigFromComparison(true)
              .build()
        } else {
          request.imageDecodeOptions
        }

    val cacheKey =
        BitmapMemoryCacheKey(
            sourceString,
            resizeOptions,
            request.rotationOptions,
            decodeOptions,
            postprocessorCacheKey,
            postprocessorName,
        )

    config.debugImageTracker?.invoke(request, cacheKey)
    return cacheKey
  }
}
