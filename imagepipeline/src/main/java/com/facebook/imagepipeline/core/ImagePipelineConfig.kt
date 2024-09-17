/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.facebook.cache.common.CacheKey
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.callercontext.CallerContextVerifier
import com.facebook.common.executors.SerialExecutorService
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.webp.BitmapCreator
import com.facebook.common.webp.WebpBitmapFactory
import com.facebook.common.webp.WebpSupportStatus
import com.facebook.imagepipeline.bitmaps.HoneycombBitmapCreator
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory
import com.facebook.imagepipeline.cache.BitmapMemoryCacheTrimStrategy
import com.facebook.imagepipeline.cache.CacheKeyFactory
import com.facebook.imagepipeline.cache.CountingLruBitmapMemoryCacheFactory
import com.facebook.imagepipeline.cache.CountingMemoryCache.EntryStateObserver
import com.facebook.imagepipeline.cache.DefaultBitmapMemoryCacheParamsSupplier
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory
import com.facebook.imagepipeline.cache.DefaultEncodedMemoryCacheParamsSupplier
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker
import com.facebook.imagepipeline.cache.MemoryCache
import com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy
import com.facebook.imagepipeline.cache.MemoryCacheParams
import com.facebook.imagepipeline.cache.NativeMemoryCacheTrimStrategy
import com.facebook.imagepipeline.cache.NoOpImageCacheStatsTracker
import com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker
import com.facebook.imagepipeline.debug.NoOpCloseableReferenceLeakTracker
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.decoder.ImageDecoderConfig
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.listener.RequestListener2
import com.facebook.imagepipeline.memory.PoolConfig
import com.facebook.imagepipeline.memory.PoolFactory
import com.facebook.imagepipeline.producers.CustomProducerSequenceFactory
import com.facebook.imagepipeline.producers.HttpUrlConnectionNetworkFetcher
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.systrace.FrescoSystrace.beginSection
import com.facebook.imagepipeline.systrace.FrescoSystrace.endSection
import com.facebook.imagepipeline.systrace.FrescoSystrace.isTracing
import com.facebook.imagepipeline.systrace.FrescoSystrace.traceSection
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory

/**
 * Main configuration class for the image pipeline library.
 *
 * To use: ` ImagePipelineConfig config = ImagePipelineConfig.newBuilder() .setXXX(xxx) .setYYY(yyy)
 * .build(); ImagePipelineFactory factory = new ImagePipelineFactory(config); ImagePipeline pipeline
 * = factory.getImagePipeline(); ` *
 *
 * This should only be done once per process.
 */
class ImagePipelineConfig private constructor(builder: Builder) : ImagePipelineConfigInterface {

  // If a member here is marked @Nullable, it must be constructed by ImagePipelineFactory
  // on demand if needed.
  // There are a lot of parameters in this class. Please follow strict alphabetical order.
  override val bitmapConfig: Bitmap.Config
  override val bitmapMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>
  override val bitmapMemoryCacheTrimStrategy: CacheTrimStrategy
  override val encodedMemoryCacheTrimStrategy: CacheTrimStrategy
  override val bitmapMemoryCacheEntryStateObserver: EntryStateObserver<CacheKey>?
  override val cacheKeyFactory: CacheKeyFactory
  override val context: Context
  override val downsampleMode: DownsampleMode
  override val diskCachesStoreSupplier: Supplier<DiskCachesStore>
  override val encodedMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>
  override val executorSupplier: ExecutorSupplier
  override val imageCacheStatsTracker: ImageCacheStatsTracker
  override val imageDecoder: ImageDecoder?
  override val imageTranscoderFactory: ImageTranscoderFactory?
  override val enableEncodedImageColorSpaceUsage: Supplier<Boolean>

  @get:ImageTranscoderType @ImageTranscoderType override val imageTranscoderType: Int?
  override val isPrefetchEnabledSupplier: Supplier<Boolean>
  override val mainDiskCacheConfig: DiskCacheConfig
  override val memoryTrimmableRegistry: MemoryTrimmableRegistry

  @get:MemoryChunkType @MemoryChunkType override val memoryChunkType: Int
  override val networkFetcher: NetworkFetcher<*>
  private val httpNetworkTimeout: Int
  override val platformBitmapFactory: PlatformBitmapFactory?
  override val poolFactory: PoolFactory
  override val progressiveJpegConfig: ProgressiveJpegConfig
  override val requestListeners: Set<RequestListener>
  override val requestListener2s: Set<RequestListener2>
  override val customProducerSequenceFactories: Set<CustomProducerSequenceFactory>
  override val isResizeAndRotateEnabledForNetwork: Boolean
  override val smallImageDiskCacheConfig: DiskCacheConfig
  override val imageDecoderConfig: ImageDecoderConfig?
  override val experiments: ImagePipelineExperiments
  override val isDiskCacheEnabled: Boolean
  override val callerContextVerifier: CallerContextVerifier?
  override val closeableReferenceLeakTracker: CloseableReferenceLeakTracker
  override val bitmapCacheOverride: MemoryCache<CacheKey, CloseableImage>?
  override val encodedMemoryCacheOverride: MemoryCache<CacheKey, PooledByteBuffer>?
  override val executorServiceForAnimatedImages: SerialExecutorService?
  override val bitmapMemoryCacheFactory: BitmapMemoryCacheFactory
  override val dynamicDiskCacheConfigMap: Map<String, DiskCacheConfig>?

  init {
    if (isTracing()) {
      beginSection("ImagePipelineConfig()")
    }
    // We have to build experiments before the rest
    experiments = builder.experimentsBuilder.build()
    bitmapMemoryCacheParamsSupplier =
        builder.bitmapMemoryCacheParamsSupplier
            ?: DefaultBitmapMemoryCacheParamsSupplier(
                (checkNotNull(builder.context.getSystemService(Context.ACTIVITY_SERVICE))
                    as ActivityManager))
    bitmapMemoryCacheTrimStrategy =
        builder.bitmapMemoryCacheTrimStrategy ?: BitmapMemoryCacheTrimStrategy()
    encodedMemoryCacheTrimStrategy =
        builder.encodedMemoryCacheTrimStrategy ?: NativeMemoryCacheTrimStrategy()
    bitmapMemoryCacheEntryStateObserver = builder.bitmapMemoryCacheEntryStateObserver
    bitmapConfig = builder.bitmapConfig ?: Bitmap.Config.ARGB_8888
    cacheKeyFactory = builder.cacheKeyFactory ?: DefaultCacheKeyFactory.getInstance()
    context = checkNotNull(builder.context)
    downsampleMode = builder.downsampleMode
    encodedMemoryCacheParamsSupplier =
        builder.encodedMemoryCacheParamsSupplier ?: DefaultEncodedMemoryCacheParamsSupplier()
    imageCacheStatsTracker =
        builder.imageCacheStatsTracker ?: NoOpImageCacheStatsTracker.getInstance()
    imageDecoder = builder.imageDecoder
    enableEncodedImageColorSpaceUsage =
        builder.enableEncodedImageColorSpaceUsage ?: Suppliers.BOOLEAN_FALSE
    imageTranscoderFactory = getImageTranscoderFactory(builder)
    imageTranscoderType = builder.imageTranscoderType
    isPrefetchEnabledSupplier = builder.isPrefetchEnabledSupplier ?: Suppliers.BOOLEAN_TRUE
    mainDiskCacheConfig =
        builder.mainDiskCacheConfig ?: getDefaultMainDiskCacheConfig(builder.context)
    memoryTrimmableRegistry =
        builder.memoryTrimmableRegistry ?: NoOpMemoryTrimmableRegistry.getInstance()
    memoryChunkType = getMemoryChunkType(builder, experiments)
    httpNetworkTimeout =
        if (builder.httpConnectionTimeout < 0) HttpUrlConnectionNetworkFetcher.HTTP_DEFAULT_TIMEOUT
        else builder.httpConnectionTimeout
    networkFetcher =
        traceSection("ImagePipelineConfig->mNetworkFetcher") {
          builder.networkFetcher ?: HttpUrlConnectionNetworkFetcher(httpNetworkTimeout)
        }
    platformBitmapFactory = builder.platformBitmapFactory
    poolFactory = builder.poolFactory ?: PoolFactory(PoolConfig.newBuilder().build())
    progressiveJpegConfig = builder.progressiveJpegConfig ?: SimpleProgressiveJpegConfig()
    requestListeners = builder.requestListeners ?: emptySet()
    requestListener2s = builder.requestListener2s ?: emptySet()
    customProducerSequenceFactories = builder.customProducerSequenceFactories ?: emptySet()
    isResizeAndRotateEnabledForNetwork = builder.resizeAndRotateEnabledForNetwork
    smallImageDiskCacheConfig = builder.smallImageDiskCacheConfig ?: mainDiskCacheConfig
    imageDecoderConfig = builder.imageDecoderConfig
    // Below this comment can't be built in alphabetical order, because of dependencies
    val numCpuBoundThreads = poolFactory.flexByteArrayPoolMaxNumThreads
    executorSupplier = builder.executorSupplier ?: DefaultExecutorSupplier(numCpuBoundThreads)
    isDiskCacheEnabled = builder.diskCacheEnabled
    callerContextVerifier = builder.callerContextVerifier
    closeableReferenceLeakTracker = builder.closeableReferenceLeakTracker
    bitmapCacheOverride = builder.bitmapMemoryCache
    bitmapMemoryCacheFactory =
        builder.bitmapMemoryCacheFactory ?: CountingLruBitmapMemoryCacheFactory()
    encodedMemoryCacheOverride = builder.encodedMemoryCache
    executorServiceForAnimatedImages = builder.serialExecutorServiceForAnimatedImages
    dynamicDiskCacheConfigMap = builder.dynamicDiskCacheConfigMap
    diskCachesStoreSupplier =
        builder.diskCachesStoreSupplier
            ?: DiskCachesStoreFactory(
                builder.fileCacheFactory
                    ?: DiskStorageCacheFactory(DynamicDefaultDiskStorageFactory()),
                this@ImagePipelineConfig)
    // Here we manage the WebpBitmapFactory implementation if any
    val webpBitmapFactory = experiments.webpBitmapFactory
    if (webpBitmapFactory != null) {
      val bitmapCreator: BitmapCreator = HoneycombBitmapCreator(poolFactory)
      setWebpBitmapFactory(webpBitmapFactory, experiments, bitmapCreator)
    }
    if (isTracing()) {
      endSection()
    }
  }

  /** Contains default configuration that can be personalized for all the request */
  class DefaultImageRequestConfig {
    var isProgressiveRenderingEnabled = false
  }

  class Builder(context: Context) {
    var bitmapConfig: Bitmap.Config? = null
      private set

    var bitmapMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>? = null
      private set

    var bitmapMemoryCacheEntryStateObserver: EntryStateObserver<CacheKey>? = null
      private set

    var bitmapMemoryCacheTrimStrategy: CacheTrimStrategy? = null
      private set

    var encodedMemoryCacheTrimStrategy: CacheTrimStrategy? = null
      private set

    var cacheKeyFactory: CacheKeyFactory? = null
      private set

    val context: Context

    var downsampleMode = DownsampleMode.AUTO
      private set

    var encodedMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>? = null
      private set

    var executorSupplier: ExecutorSupplier? = null
      private set

    var imageCacheStatsTracker: ImageCacheStatsTracker? = null
      private set

    var imageDecoder: ImageDecoder? = null
      private set

    var enableEncodedImageColorSpaceUsage: Supplier<Boolean>? = null
      private set

    var imageTranscoderFactory: ImageTranscoderFactory? = null
      private set

    @ImageTranscoderType var imageTranscoderType: Int? = null
    var isPrefetchEnabledSupplier: Supplier<Boolean>? = null
      private set

    var mainDiskCacheConfig: DiskCacheConfig? = null
      private set

    var memoryTrimmableRegistry: MemoryTrimmableRegistry? = null
      private set

    @MemoryChunkType var memoryChunkType: Int? = null
    var networkFetcher: NetworkFetcher<*>? = null
      private set

    var platformBitmapFactory: PlatformBitmapFactory? = null
      private set

    var poolFactory: PoolFactory? = null
      private set

    var progressiveJpegConfig: ProgressiveJpegConfig? = null
      private set

    var requestListeners: Set<RequestListener>? = null
      private set

    var requestListener2s: Set<RequestListener2>? = null
      private set

    var customProducerSequenceFactories: Set<CustomProducerSequenceFactory>? = null
      private set

    var resizeAndRotateEnabledForNetwork = true
      private set

    var smallImageDiskCacheConfig: DiskCacheConfig? = null
      private set

    var fileCacheFactory: FileCacheFactory? = null
      private set

    var diskCachesStoreSupplier: Supplier<DiskCachesStore>? = null
      private set

    var imageDecoderConfig: ImageDecoderConfig? = null
      private set

    var httpConnectionTimeout = -1
      private set

    val experimentsBuilder = ImagePipelineExperiments.Builder(this)
    var diskCacheEnabled = true
      private set

    var callerContextVerifier: CallerContextVerifier? = null
      private set

    var closeableReferenceLeakTracker: CloseableReferenceLeakTracker =
        NoOpCloseableReferenceLeakTracker()
      private set

    var bitmapMemoryCache: MemoryCache<CacheKey, CloseableImage>? = null
      private set

    var encodedMemoryCache: MemoryCache<CacheKey, PooledByteBuffer>? = null
      private set

    var serialExecutorServiceForAnimatedImages: SerialExecutorService? = null
      private set

    var bitmapMemoryCacheFactory: BitmapMemoryCacheFactory? = null
      private set

    var dynamicDiskCacheConfigMap: Map<String, DiskCacheConfig>? = null
      private set

    fun setBitmapsConfig(config: Bitmap.Config?): Builder = apply { this.bitmapConfig = config }

    fun setBitmapMemoryCacheParamsSupplier(
        bitmapMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>?
    ): Builder = apply {
      this.bitmapMemoryCacheParamsSupplier = checkNotNull(bitmapMemoryCacheParamsSupplier)
    }

    fun setBitmapMemoryCacheEntryStateObserver(
        bitmapMemoryCacheEntryStateObserver: EntryStateObserver<CacheKey>?
    ): Builder = apply {
      this.bitmapMemoryCacheEntryStateObserver = bitmapMemoryCacheEntryStateObserver
    }

    fun setBitmapMemoryCacheTrimStrategy(trimStrategy: CacheTrimStrategy?): Builder = apply {
      this.bitmapMemoryCacheTrimStrategy = trimStrategy
    }

    fun setEncodedMemoryCacheTrimStrategy(trimStrategy: CacheTrimStrategy?): Builder = apply {
      this.encodedMemoryCacheTrimStrategy = trimStrategy
    }

    fun setCacheKeyFactory(cacheKeyFactory: CacheKeyFactory?): Builder = apply {
      this.cacheKeyFactory = cacheKeyFactory
    }

    fun setHttpConnectionTimeout(httpConnectionTimeoutMs: Int): Builder = apply {
      this.httpConnectionTimeout = httpConnectionTimeoutMs
    }

    fun setFileCacheFactory(fileCacheFactory: FileCacheFactory): Builder = apply {
      this.fileCacheFactory = fileCacheFactory
    }

    fun setDiskCachesStoreSupplier(diskCachesStoreSupplier: Supplier<DiskCachesStore>): Builder =
        apply {
          this.diskCachesStoreSupplier = diskCachesStoreSupplier
        }

    fun isDownsampleEnabled(): Boolean = this.downsampleMode === DownsampleMode.ALWAYS

    fun setDownsampleMode(downsampleMode: DownsampleMode): Builder = apply {
      this.downsampleMode = downsampleMode
    }

    @Deprecated("Use the new setDownsampleMode() method")
    fun setDownsampleEnabled(downsampleEnabled: Boolean): Builder = apply {
      if (downsampleEnabled) {
        setDownsampleMode(DownsampleMode.ALWAYS)
      } else {
        setDownsampleMode(DownsampleMode.AUTO)
      }
    }

    fun isDiskCacheEnabled(): Boolean = this.diskCacheEnabled

    fun setDiskCacheEnabled(diskCacheEnabled: Boolean): Builder = apply {
      this.diskCacheEnabled = diskCacheEnabled
    }

    fun setEncodedMemoryCacheParamsSupplier(
        encodedMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>?
    ): Builder = apply {
      this.encodedMemoryCacheParamsSupplier = checkNotNull(encodedMemoryCacheParamsSupplier)
    }

    fun setExecutorSupplier(executorSupplier: ExecutorSupplier?): Builder = apply {
      this.executorSupplier = executorSupplier
    }

    fun setImageCacheStatsTracker(imageCacheStatsTracker: ImageCacheStatsTracker?): Builder =
        apply {
          this.imageCacheStatsTracker = imageCacheStatsTracker
        }

    fun setImageDecoder(imageDecoder: ImageDecoder?): Builder = apply {
      this.imageDecoder = imageDecoder
    }

    fun setEnableEncodedImageColorSpaceUsage(
        enableEncodedImageColorSpaceUsage: Supplier<Boolean>?
    ): Builder = apply {
      this.enableEncodedImageColorSpaceUsage = enableEncodedImageColorSpaceUsage
    }

    fun setImageTranscoderType(@ImageTranscoderType imageTranscoderType: Int): Builder = apply {
      this.imageTranscoderType = imageTranscoderType
    }

    fun setImageTranscoderFactory(imageTranscoderFactory: ImageTranscoderFactory?): Builder =
        apply {
          this.imageTranscoderFactory = imageTranscoderFactory
        }

    fun setIsPrefetchEnabledSupplier(isPrefetchEnabledSupplier: Supplier<Boolean>?): Builder =
        apply {
          this.isPrefetchEnabledSupplier = isPrefetchEnabledSupplier
        }

    fun setMainDiskCacheConfig(mainDiskCacheConfig: DiskCacheConfig?): Builder = apply {
      this.mainDiskCacheConfig = mainDiskCacheConfig
    }

    fun setMemoryTrimmableRegistry(memoryTrimmableRegistry: MemoryTrimmableRegistry?): Builder =
        apply {
          this.memoryTrimmableRegistry = memoryTrimmableRegistry
        }

    fun setMemoryChunkType(@MemoryChunkType memoryChunkType: Int): Builder = apply {
      this.memoryChunkType = memoryChunkType
    }

    fun setNetworkFetcher(networkFetcher: NetworkFetcher<*>?): Builder = apply {
      this.networkFetcher = networkFetcher
    }

    fun setPlatformBitmapFactory(platformBitmapFactory: PlatformBitmapFactory?): Builder = apply {
      this.platformBitmapFactory = platformBitmapFactory
    }

    fun setPoolFactory(poolFactory: PoolFactory?): Builder = apply {
      this.poolFactory = poolFactory
    }

    fun setProgressiveJpegConfig(progressiveJpegConfig: ProgressiveJpegConfig?): Builder = apply {
      this.progressiveJpegConfig = progressiveJpegConfig
    }

    fun setRequestListeners(requestListeners: Set<RequestListener>?): Builder = apply {
      this.requestListeners = requestListeners
    }

    fun setRequestListener2s(requestListeners: Set<RequestListener2>?): Builder = apply {
      requestListener2s = requestListeners
    }

    fun setCustomFetchSequenceFactories(
        customProducerSequenceFactories: Set<CustomProducerSequenceFactory>?
    ): Builder = apply { this.customProducerSequenceFactories = customProducerSequenceFactories }

    fun setResizeAndRotateEnabledForNetwork(resizeAndRotateEnabledForNetwork: Boolean): Builder =
        apply {
          this.resizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork
        }

    fun setSmallImageDiskCacheConfig(smallImageDiskCacheConfig: DiskCacheConfig?): Builder = apply {
      this.smallImageDiskCacheConfig = smallImageDiskCacheConfig
    }

    fun setImageDecoderConfig(imageDecoderConfig: ImageDecoderConfig?): Builder = apply {
      this.imageDecoderConfig = imageDecoderConfig
    }

    fun setCallerContextVerifier(callerContextVerifier: CallerContextVerifier?): Builder = apply {
      this.callerContextVerifier = callerContextVerifier
    }

    fun setCloseableReferenceLeakTracker(
        closeableReferenceLeakTracker: CloseableReferenceLeakTracker
    ): Builder = apply { this.closeableReferenceLeakTracker = closeableReferenceLeakTracker }

    fun setBitmapMemoryCache(bitmapMemoryCache: MemoryCache<CacheKey, CloseableImage>?): Builder =
        apply {
          this.bitmapMemoryCache = bitmapMemoryCache
        }

    fun setEncodedMemoryCache(
        encodedMemoryCache: MemoryCache<CacheKey, PooledByteBuffer>?
    ): Builder = apply { this.encodedMemoryCache = encodedMemoryCache }

    fun setExecutorServiceForAnimatedImages(
        serialExecutorService: SerialExecutorService?
    ): Builder = apply { this.serialExecutorServiceForAnimatedImages = serialExecutorService }

    fun setBitmapMemoryCacheFactory(bitmapMemoryCacheFactory: BitmapMemoryCacheFactory?): Builder =
        apply {
          this.bitmapMemoryCacheFactory = bitmapMemoryCacheFactory
        }

    fun setDynamicDiskCacheConfigMap(
        dynamicDiskCacheConfigMap: Map<String, DiskCacheConfig>
    ): Builder = apply { this.dynamicDiskCacheConfigMap = dynamicDiskCacheConfigMap }

    fun experiment(): ImagePipelineExperiments.Builder = experimentsBuilder

    fun build(): ImagePipelineConfig = ImagePipelineConfig(this)

    init {
      // Doesn't use a setter as always required.
      this.context = context
    }
  }

  companion object {
    @JvmStatic
    var defaultImageRequestConfig = DefaultImageRequestConfig()
      private set

    private fun setWebpBitmapFactory(
        webpBitmapFactory: WebpBitmapFactory,
        imagePipelineExperiments: ImagePipelineExperiments,
        bitmapCreator: BitmapCreator?
    ) {
      WebpSupportStatus.sWebpBitmapFactory = webpBitmapFactory
      val webpErrorLogger = imagePipelineExperiments.webpErrorLogger
      if (webpErrorLogger != null) {
        webpBitmapFactory.setWebpErrorLogger(webpErrorLogger)
      }
      if (bitmapCreator != null) {
        webpBitmapFactory.setBitmapCreator(bitmapCreator)
      }
    }

    private fun getDefaultMainDiskCacheConfig(context: Context): DiskCacheConfig =
        traceSection("DiskCacheConfig.getDefaultMainDiskCacheConfig") {
          DiskCacheConfig.newBuilder(context).build()
        }

    @JvmStatic
    @VisibleForTesting
    fun resetDefaultRequestConfig() {
      defaultImageRequestConfig = DefaultImageRequestConfig()
    }

    @JvmStatic fun newBuilder(context: Context): Builder = Builder(context)

    private fun getImageTranscoderFactory(builder: Builder): ImageTranscoderFactory? {
      check(!(builder.imageTranscoderFactory != null && builder.imageTranscoderType != null)) {
        "You can't define a custom ImageTranscoderFactory and provide an ImageTranscoderType"
      }
      return builder.imageTranscoderFactory
      // This member will be constructed by ImagePipelineFactory
    }

    @MemoryChunkType
    private fun getMemoryChunkType(
        builder: Builder,
        imagePipelineExperiments: ImagePipelineExperiments
    ): Int =
        builder.memoryChunkType
            ?: if (imagePipelineExperiments.memoryType == MemoryChunkType.ASHMEM_MEMORY.toLong() &&
                Build.VERSION.SDK_INT >= 27) {
              MemoryChunkType.ASHMEM_MEMORY
            } else if (imagePipelineExperiments.memoryType ==
                MemoryChunkType.BUFFER_MEMORY.toLong()) {
              MemoryChunkType.BUFFER_MEMORY
            } else if (imagePipelineExperiments.memoryType ==
                MemoryChunkType.NATIVE_MEMORY.toLong()) {
              MemoryChunkType.NATIVE_MEMORY
            } else {
              MemoryChunkType.NATIVE_MEMORY
            }
  }
}
