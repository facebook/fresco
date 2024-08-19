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
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory

interface ImagePipelineConfigInterface {

  val bitmapConfig: Bitmap.Config?
  val bitmapMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>
  val bitmapMemoryCacheTrimStrategy: CacheTrimStrategy
  val encodedMemoryCacheTrimStrategy: CacheTrimStrategy
  val bitmapMemoryCacheEntryStateObserver: EntryStateObserver<CacheKey>?
  val cacheKeyFactory: CacheKeyFactory
  val context: Context
  val diskCachesStoreSupplier: Supplier<DiskCachesStore>
  val downsampleMode: DownsampleMode
  val isDiskCacheEnabled: Boolean
  val encodedMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>
  val executorSupplier: ExecutorSupplier
  val executorServiceForAnimatedImages: SerialExecutorService?
  val imageCacheStatsTracker: ImageCacheStatsTracker
  val imageDecoder: ImageDecoder?
  val imageTranscoderFactory: ImageTranscoderFactory?

  @get:ImageTranscoderType val imageTranscoderType: Int?
  val isPrefetchEnabledSupplier: Supplier<Boolean>
  val enableEncodedImageColorSpaceUsage: Supplier<Boolean>
  val mainDiskCacheConfig: DiskCacheConfig
  val memoryTrimmableRegistry: MemoryTrimmableRegistry

  @get:MemoryChunkType val memoryChunkType: Int
  val networkFetcher: NetworkFetcher<*>
  val platformBitmapFactory: PlatformBitmapFactory?
  val poolFactory: PoolFactory
  val progressiveJpegConfig: ProgressiveJpegConfig
  val requestListeners: Set<RequestListener?>
  val requestListener2s: Set<RequestListener2?>
  val customProducerSequenceFactories: Set<CustomProducerSequenceFactory>
  val isResizeAndRotateEnabledForNetwork: Boolean
  val smallImageDiskCacheConfig: DiskCacheConfig
  val imageDecoderConfig: ImageDecoderConfig?
  val callerContextVerifier: CallerContextVerifier?
  val experiments: ImagePipelineExperiments
  val closeableReferenceLeakTracker: CloseableReferenceLeakTracker
  val bitmapCacheOverride: MemoryCache<CacheKey, CloseableImage>?
  val encodedMemoryCacheOverride: MemoryCache<CacheKey, PooledByteBuffer>?
  val bitmapMemoryCacheFactory: BitmapMemoryCacheFactory
  val dynamicDiskCacheConfigMap: Map<String, DiskCacheConfig>?
}
