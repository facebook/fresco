/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import android.content.Context
import android.graphics.Bitmap
import com.facebook.cache.common.CacheKey
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.callercontext.CallerContextVerifier
import com.facebook.common.executors.SerialExecutorService
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory
import com.facebook.imagepipeline.cache.CacheKeyFactory
import com.facebook.imagepipeline.cache.CountingMemoryCache.EntryStateObserver
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker
import com.facebook.imagepipeline.cache.MemoryCache
import com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy
import com.facebook.imagepipeline.cache.MemoryCacheParams
import com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.decoder.ImageDecoderConfig
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.listener.RequestListener2
import com.facebook.imagepipeline.memory.PoolFactory
import com.facebook.imagepipeline.producers.CustomProducerSequenceFactory
import com.facebook.imagepipeline.producers.DecodeProducer
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory

interface ImagePipelineConfigInterface {

  // Global listeners
  val requestListeners: Set<RequestListener?>
  val requestListener2s: Set<RequestListener2?>

  // General cache configuration
  val cacheKeyFactory: CacheKeyFactory
  val imageCacheStatsTracker: ImageCacheStatsTracker

  // Disk cache
  val isDiskCacheEnabled: Boolean
  val diskCachesStoreSupplier: Supplier<DiskCachesStore>
  val mainDiskCacheConfig: DiskCacheConfig
  val smallImageDiskCacheConfig: DiskCacheConfig
  val dynamicDiskCacheConfigMap: Map<String, DiskCacheConfig>?

  // Encoded memory cache
  val encodedMemoryCacheTrimStrategy: CacheTrimStrategy
  val encodedMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>
  val encodedMemoryCacheOverride: MemoryCache<CacheKey, PooledByteBuffer>?

  // Bitmap memory cache
  val bitmapMemoryCacheFactory: BitmapMemoryCacheFactory
  val bitmapMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>
  val bitmapMemoryCacheTrimStrategy: CacheTrimStrategy
  val bitmapMemoryCacheEntryStateObserver: EntryStateObserver<CacheKey>?
  val bitmapCacheOverride: MemoryCache<CacheKey, CloseableImage>?

  // Network configuration
  val networkFetcher: NetworkFetcher<*>
  val isResizeAndRotateEnabledForNetwork: Boolean

  // Image decoding
  val imageDecoder: ImageDecoder?
  val imageDecoderConfig: ImageDecoderConfig?
  val bitmapConfig: Bitmap.Config?
  val downsampleMode: DownsampleMode
  val imageTranscoderFactory: ImageTranscoderFactory?
  @get:ImageTranscoderType val imageTranscoderType: Int?
  val enableEncodedImageColorSpaceUsage: Supplier<Boolean>
  val progressiveJpegConfig: ProgressiveJpegConfig
  val platformBitmapFactory: PlatformBitmapFactory?

  // Memory handling
  @get:MemoryChunkType val memoryChunkType: Int
  val memoryTrimmableRegistry: MemoryTrimmableRegistry
  val customProducerSequenceFactories: Set<CustomProducerSequenceFactory>
  val closeableReferenceLeakTracker: CloseableReferenceLeakTracker
  val poolFactory: PoolFactory

  // Others
  val context: Context
  val executorSupplier: ExecutorSupplier
  val executorServiceForAnimatedImages: SerialExecutorService?
  val isPrefetchEnabledSupplier: Supplier<Boolean>
  val callerContextVerifier: CallerContextVerifier?
  val decodedOriginalImageAnalyzers: Set<DecodeProducer.DecodedOriginalImageAnalyzer>
  val isAppStarting: (() -> Boolean)?

  // Experiments
  val experiments: ImagePipelineExperiments
}
