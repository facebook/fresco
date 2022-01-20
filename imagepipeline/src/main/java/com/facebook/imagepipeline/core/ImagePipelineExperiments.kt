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
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.common.memory.ByteArrayPool
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.common.memory.PooledByteStreams
import com.facebook.common.webp.WebpBitmapFactory
import com.facebook.common.webp.WebpBitmapFactory.WebpErrorLogger
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.cache.BufferedDiskCache
import com.facebook.imagepipeline.cache.CacheKeyFactory
import com.facebook.imagepipeline.cache.MemoryCache
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imageutils.BitmapUtil
import kotlin.jvm.JvmField

/**
 * Encapsulates additional elements of the [ImagePipelineConfig] which are currently in an
 * experimental state.
 *
 * These options may often change or disappear altogether and it is not recommended to change their
 * values from their defaults.
 */
class ImagePipelineExperiments private constructor(builder: Builder) {

  val isWebpSupportEnabled: Boolean
  val webpErrorLogger: WebpErrorLogger?
  val isDecodeCancellationEnabled: Boolean
  val webpBitmapFactory: WebpBitmapFactory?
  val useDownsamplingRatioForResizing: Boolean
  val useBitmapPrepareToDraw: Boolean
  val bitmapPrepareToDrawMinSizeBytes: Int
  val bitmapPrepareToDrawMaxSizeBytes: Int
  val bitmapPrepareToDrawForPrefetch: Boolean
  val maxBitmapSize: Int
  val isNativeCodeDisabled: Boolean
  val isPartialImageCachingEnabled: Boolean
  var producerFactoryMethod: ProducerFactoryMethod? = null
  val isLazyDataSource: Supplier<Boolean>?
  val isGingerbreadDecoderEnabled: Boolean
  private val downscaleFrameToDrawableDimensions: Boolean
  val bitmapCloseableRefType: Int
  val suppressBitmapPrefetchingSupplier: Supplier<Boolean>
  val isExperimentalThreadHandoffQueueEnabled: Boolean
  val memoryType: Long
  private val keepCancelledFetchAsLowPriority: Boolean
  private val downsampleIfLargeBitmap: Boolean
  val isEncodedCacheEnabled: Boolean
  val isEnsureTranscoderLibraryLoaded: Boolean
  val isEncodedMemoryCacheProbingEnabled: Boolean
  val isDiskCacheProbingEnabled: Boolean
  val trackedKeysSize: Int
  private val allowDelay: Boolean
  private val handOffOnUiThreadOnly: Boolean
  private val shouldStoreCacheEntrySize: Boolean
  private val shouldIgnoreCacheSizeMismatch: Boolean
  private val shouldUseDecodingBufferHelper: Boolean

  fun shouldDownsampleIfLargeBitmap(): Boolean = downsampleIfLargeBitmap

  fun shouldDownscaleFrameToDrawableDimensions(): Boolean = downscaleFrameToDrawableDimensions

  fun shouldKeepCancelledFetchAsLowPriority(): Boolean = keepCancelledFetchAsLowPriority

  fun allowDelay(): Boolean = allowDelay

  fun handoffOnUiThreadOnly(): Boolean = handOffOnUiThreadOnly

  fun shouldStoreCacheEntrySize(): Boolean = shouldStoreCacheEntrySize

  fun shouldIgnoreCacheSizeMismatch(): Boolean = shouldIgnoreCacheSizeMismatch

  fun shouldUseDecodingBufferHelper(): Boolean = shouldUseDecodingBufferHelper

  class Builder(private val configBuilder: ImagePipelineConfig.Builder) {
    @JvmField var shouldUseDecodingBufferHelper = false
    @JvmField var webpSupportEnabled = false
    @JvmField var webpErrorLogger: WebpErrorLogger? = null
    @JvmField var decodeCancellationEnabled = false
    @JvmField var webpBitmapFactory: WebpBitmapFactory? = null
    @JvmField var useDownsamplingRatioForResizing = false
    @JvmField var useBitmapPrepareToDraw = false
    @JvmField var bitmapPrepareToDrawMinSizeBytes = 0
    @JvmField var bitmapPrepareToDrawMaxSizeBytes = 0

    @JvmField var bitmapPrepareToDrawForPrefetch = false
    @JvmField var maxBitmapSize = BitmapUtil.MAX_BITMAP_SIZE.toInt()
    @JvmField var nativeCodeDisabled = false
    @JvmField var isPartialImageCachingEnabled = false
    @JvmField var producerFactoryMethod: ProducerFactoryMethod? = null

    @JvmField var lazyDataSource: Supplier<Boolean>? = null

    @JvmField var gingerbreadDecoderEnabled = false

    @JvmField var downscaleFrameToDrawableDimensions = false

    @JvmField var bitmapCloseableRefType = 0

    @JvmField var suppressBitmapPrefetchingSupplier = Suppliers.of(false)

    @JvmField var experimentalThreadHandoffQueueEnabled = false

    @JvmField var memoryType: Long = 0
    @JvmField var keepCancelledFetchAsLowPriority = false

    @JvmField var downsampleIfLargeBitmap = false

    @JvmField var encodedCacheEnabled = true

    @JvmField var ensureTranscoderLibraryLoaded = true
    @JvmField var isEncodedMemoryCacheProbingEnabled = false
    @JvmField var isDiskCacheProbingEnabled = false
    @JvmField var trackedKeysSize = 20
    @JvmField var allowDelay = false
    @JvmField var handOffOnUiThreadOnly = false
    @JvmField var shouldStoreCacheEntrySize = false

    @JvmField var shouldIgnoreCacheSizeMismatch = false
    fun setHandOffOnUiThreadOnly(handOffOnUiThreadOnly: Boolean): ImagePipelineConfig.Builder {
      this.handOffOnUiThreadOnly = handOffOnUiThreadOnly
      return configBuilder
    }

    fun setStoreCacheEntrySize(shouldStoreCacheEntrySize: Boolean): ImagePipelineConfig.Builder {
      this.shouldStoreCacheEntrySize = shouldStoreCacheEntrySize
      return configBuilder
    }

    fun setIgnoreCacheSizeMismatch(
        shouldIgnoreCacheSizeMismatch: Boolean
    ): ImagePipelineConfig.Builder {
      this.shouldIgnoreCacheSizeMismatch = shouldIgnoreCacheSizeMismatch
      return configBuilder
    }

    fun setWebpSupportEnabled(webpSupportEnabled: Boolean): ImagePipelineConfig.Builder {
      this.webpSupportEnabled = webpSupportEnabled
      return configBuilder
    }

    fun shouldUseDecodingBufferHelper(): Boolean = shouldUseDecodingBufferHelper

    fun setShouldUseDecodingBufferHelper(
        shouldUseDecodingBufferHelper: Boolean
    ): ImagePipelineConfig.Builder {
      this.shouldUseDecodingBufferHelper = shouldUseDecodingBufferHelper
      return configBuilder
    }

    fun setUseDownsampligRatioForResizing(
        useDownsamplingRatioForResizing: Boolean
    ): ImagePipelineConfig.Builder {
      this.useDownsamplingRatioForResizing = useDownsamplingRatioForResizing
      return configBuilder
    }

    /**
     * Enables the caching of partial image data, for example if the request is cancelled or fails
     * after some data has been received.
     */
    fun setPartialImageCachingEnabled(
        partialImageCachingEnabled: Boolean
    ): ImagePipelineConfig.Builder {
      isPartialImageCachingEnabled = partialImageCachingEnabled
      return configBuilder
    }

    /**
     * If true we cancel decoding jobs when the related request has been cancelled
     *
     * @param decodeCancellationEnabled If true the decoding of cancelled requests are cancelled
     * @return The Builder itself for chaining
     */
    fun setDecodeCancellationEnabled(
        decodeCancellationEnabled: Boolean
    ): ImagePipelineConfig.Builder {
      this.decodeCancellationEnabled = decodeCancellationEnabled
      return configBuilder
    }

    fun setWebpErrorLogger(webpErrorLogger: WebpErrorLogger?): ImagePipelineConfig.Builder {
      this.webpErrorLogger = webpErrorLogger
      return configBuilder
    }

    fun setWebpBitmapFactory(webpBitmapFactory: WebpBitmapFactory?): ImagePipelineConfig.Builder {
      this.webpBitmapFactory = webpBitmapFactory
      return configBuilder
    }

    /**
     * If enabled, the pipeline will call [android.graphics.Bitmap.prepareToDraw] after decoding.
     * This potentially reduces lag on Android N+ as this step now happens async when the
     * RendererThread is idle.
     *
     * @param useBitmapPrepareToDraw set true for enabling prepareToDraw
     * @param minBitmapSizeBytes Bitmaps with a [Bitmap.getByteCount] smaller than this value are
     * not uploaded
     * @param maxBitmapSizeBytes Bitmaps with a [Bitmap.getByteCount] larger than this value are not
     * uploaded
     * @param preparePrefetch If this is true, also pre-fetching image requests will trigger the
     * [android.graphics.Bitmap.prepareToDraw] call.
     * @return The Builder itself for chaining
     */
    fun setBitmapPrepareToDraw(
        useBitmapPrepareToDraw: Boolean,
        minBitmapSizeBytes: Int,
        maxBitmapSizeBytes: Int,
        preparePrefetch: Boolean
    ): ImagePipelineConfig.Builder {
      this.useBitmapPrepareToDraw = useBitmapPrepareToDraw
      bitmapPrepareToDrawMinSizeBytes = minBitmapSizeBytes
      bitmapPrepareToDrawMaxSizeBytes = maxBitmapSizeBytes
      bitmapPrepareToDrawForPrefetch = preparePrefetch
      return configBuilder
    }

    /**
     * Sets the maximum bitmap size use to compute the downsampling value when decoding Jpeg images.
     */
    fun setMaxBitmapSize(maxBitmapSize: Int): ImagePipelineConfig.Builder {
      this.maxBitmapSize = maxBitmapSize
      return configBuilder
    }

    /**
     * If true, the pipeline will use alternative implementations without native code.
     *
     * @param nativeCodeDisabled set true for disabling native implementation.
     * @return The Builder itself for chaining
     */
    fun setNativeCodeDisabled(nativeCodeDisabled: Boolean): ImagePipelineConfig.Builder {
      this.nativeCodeDisabled = nativeCodeDisabled
      return configBuilder
    }

    /**
     * Stores an alternative method to instantiate the [ProducerFactory]. This allows experimenting
     * with overridden producers.
     */
    fun setProducerFactoryMethod(
        producerFactoryMethod: ProducerFactoryMethod?
    ): ImagePipelineConfig.Builder {
      this.producerFactoryMethod = producerFactoryMethod
      return configBuilder
    }

    /** Stores an alternative lazy method to instantiate the data souce. */
    fun setLazyDataSource(lazyDataSource: Supplier<Boolean>?): ImagePipelineConfig.Builder {
      this.lazyDataSource = lazyDataSource
      return configBuilder
    }

    fun setGingerbreadDecoderEnabled(
        gingerbreadDecoderEnabled: Boolean
    ): ImagePipelineConfig.Builder {
      this.gingerbreadDecoderEnabled = gingerbreadDecoderEnabled
      return configBuilder
    }

    fun setShouldDownscaleFrameToDrawableDimensions(
        downscaleFrameToDrawableDimensions: Boolean
    ): ImagePipelineConfig.Builder {
      this.downscaleFrameToDrawableDimensions = downscaleFrameToDrawableDimensions
      return configBuilder
    }

    fun setBitmapCloseableRefType(bitmapCloseableRefType: Int): ImagePipelineConfig.Builder {
      this.bitmapCloseableRefType = bitmapCloseableRefType
      return configBuilder
    }

    fun setSuppressBitmapPrefetchingSupplier(
        suppressBitmapPrefetchingSupplier: Supplier<Boolean>
    ): ImagePipelineConfig.Builder {
      this.suppressBitmapPrefetchingSupplier = suppressBitmapPrefetchingSupplier
      return configBuilder
    }

    fun setExperimentalThreadHandoffQueueEnabled(
        experimentalThreadHandoffQueueEnabled: Boolean
    ): ImagePipelineConfig.Builder {
      this.experimentalThreadHandoffQueueEnabled = experimentalThreadHandoffQueueEnabled
      return configBuilder
    }

    fun setExperimentalMemoryType(MemoryType: Long): ImagePipelineConfig.Builder {
      this.memoryType = MemoryType
      return configBuilder
    }

    fun setKeepCancelledFetchAsLowPriority(
        keepCancelledFetchAsLowPriority: Boolean
    ): ImagePipelineConfig.Builder {
      this.keepCancelledFetchAsLowPriority = keepCancelledFetchAsLowPriority
      return configBuilder
    }

    fun setDownsampleIfLargeBitmap(downsampleIfLargeBitmap: Boolean): ImagePipelineConfig.Builder {
      this.downsampleIfLargeBitmap = downsampleIfLargeBitmap
      return configBuilder
    }

    fun setEncodedCacheEnabled(encodedCacheEnabled: Boolean): ImagePipelineConfig.Builder {
      this.encodedCacheEnabled = encodedCacheEnabled
      return configBuilder
    }

    fun setEnsureTranscoderLibraryLoaded(
        ensureTranscoderLibraryLoaded: Boolean
    ): ImagePipelineConfig.Builder {
      this.ensureTranscoderLibraryLoaded = ensureTranscoderLibraryLoaded
      return configBuilder
    }

    fun setIsDiskCacheProbingEnabled(
        isDiskCacheProbingEnabled: Boolean
    ): ImagePipelineConfig.Builder {
      this.isDiskCacheProbingEnabled = isDiskCacheProbingEnabled
      return configBuilder
    }

    fun setIsEncodedMemoryCacheProbingEnabled(
        isEncodedMemoryCacheProbingEnabled: Boolean
    ): ImagePipelineConfig.Builder {
      this.isEncodedMemoryCacheProbingEnabled = isEncodedMemoryCacheProbingEnabled
      return configBuilder
    }

    fun setTrackedKeysSize(trackedKeysSize: Int): ImagePipelineConfig.Builder {
      this.trackedKeysSize = trackedKeysSize
      return configBuilder
    }

    fun setAllowDelay(allowDelay: Boolean): ImagePipelineConfig.Builder {
      this.allowDelay = allowDelay
      return configBuilder
    }

    fun build(): ImagePipelineExperiments = ImagePipelineExperiments(this)
  }

  interface ProducerFactoryMethod {
    fun createProducerFactory(
        context: Context,
        byteArrayPool: ByteArrayPool,
        imageDecoder: ImageDecoder,
        progressiveJpegConfig: ProgressiveJpegConfig,
        downsampleEnabled: Boolean,
        resizeAndRotateEnabledForNetwork: Boolean,
        decodeCancellationEnabled: Boolean,
        executorSupplier: ExecutorSupplier,
        pooledByteBufferFactory: PooledByteBufferFactory,
        pooledByteStreams: PooledByteStreams,
        bitmapMemoryCache: MemoryCache<CacheKey?, CloseableImage?>,
        encodedMemoryCache: MemoryCache<CacheKey?, PooledByteBuffer?>,
        defaultBufferedDiskCache: BufferedDiskCache,
        smallImageBufferedDiskCache: BufferedDiskCache,
        cacheKeyFactory: CacheKeyFactory,
        platformBitmapFactory: PlatformBitmapFactory,
        bitmapPrepareToDrawMinSizeBytes: Int,
        bitmapPrepareToDrawMaxSizeBytes: Int,
        bitmapPrepareToDrawForPrefetch: Boolean,
        maxBitmapSize: Int,
        closeableReferenceFactory: CloseableReferenceFactory,
        keepCancelledFetchAsLowPriority: Boolean,
        trackedKeysSize: Int
    ): ProducerFactory
  }

  class DefaultProducerFactoryMethod : ProducerFactoryMethod {
    override fun createProducerFactory(
        context: Context,
        byteArrayPool: ByteArrayPool,
        imageDecoder: ImageDecoder,
        progressiveJpegConfig: ProgressiveJpegConfig,
        downsampleEnabled: Boolean,
        resizeAndRotateEnabledForNetwork: Boolean,
        decodeCancellationEnabled: Boolean,
        executorSupplier: ExecutorSupplier,
        pooledByteBufferFactory: PooledByteBufferFactory,
        pooledByteStreams: PooledByteStreams,
        bitmapMemoryCache: MemoryCache<CacheKey?, CloseableImage?>,
        encodedMemoryCache: MemoryCache<CacheKey?, PooledByteBuffer?>,
        defaultBufferedDiskCache: BufferedDiskCache,
        smallImageBufferedDiskCache: BufferedDiskCache,
        cacheKeyFactory: CacheKeyFactory,
        platformBitmapFactory: PlatformBitmapFactory,
        bitmapPrepareToDrawMinSizeBytes: Int,
        bitmapPrepareToDrawMaxSizeBytes: Int,
        bitmapPrepareToDrawForPrefetch: Boolean,
        maxBitmapSize: Int,
        closeableReferenceFactory: CloseableReferenceFactory,
        keepCancelledFetchAsLowPriority: Boolean,
        trackedKeysSize: Int
    ): ProducerFactory =
        ProducerFactory(
            context!!,
            byteArrayPool!!,
            imageDecoder!!,
            progressiveJpegConfig!!,
            downsampleEnabled,
            resizeAndRotateEnabledForNetwork,
            decodeCancellationEnabled,
            executorSupplier!!,
            pooledByteBufferFactory!!,
            bitmapMemoryCache!!,
            encodedMemoryCache!!,
            defaultBufferedDiskCache!!,
            smallImageBufferedDiskCache!!,
            cacheKeyFactory!!,
            platformBitmapFactory!!,
            bitmapPrepareToDrawMinSizeBytes,
            bitmapPrepareToDrawMaxSizeBytes,
            bitmapPrepareToDrawForPrefetch,
            maxBitmapSize,
            closeableReferenceFactory!!,
            keepCancelledFetchAsLowPriority,
            trackedKeysSize)
  }

  init {
    isWebpSupportEnabled = builder.webpSupportEnabled
    webpErrorLogger = builder.webpErrorLogger
    isDecodeCancellationEnabled = builder.decodeCancellationEnabled
    webpBitmapFactory = builder.webpBitmapFactory
    useDownsamplingRatioForResizing = builder.useDownsamplingRatioForResizing
    useBitmapPrepareToDraw = builder.useBitmapPrepareToDraw
    bitmapPrepareToDrawMinSizeBytes = builder.bitmapPrepareToDrawMinSizeBytes
    bitmapPrepareToDrawMaxSizeBytes = builder.bitmapPrepareToDrawMaxSizeBytes
    bitmapPrepareToDrawForPrefetch = builder.bitmapPrepareToDrawForPrefetch
    maxBitmapSize = builder.maxBitmapSize
    isNativeCodeDisabled = builder.nativeCodeDisabled
    isPartialImageCachingEnabled = builder.isPartialImageCachingEnabled
    if (builder.producerFactoryMethod == null) {
      producerFactoryMethod = DefaultProducerFactoryMethod()
    } else {
      producerFactoryMethod = builder.producerFactoryMethod
    }
    isLazyDataSource = builder.lazyDataSource
    isGingerbreadDecoderEnabled = builder.gingerbreadDecoderEnabled
    downscaleFrameToDrawableDimensions = builder.downscaleFrameToDrawableDimensions
    bitmapCloseableRefType = builder.bitmapCloseableRefType
    suppressBitmapPrefetchingSupplier = builder.suppressBitmapPrefetchingSupplier
    isExperimentalThreadHandoffQueueEnabled = builder.experimentalThreadHandoffQueueEnabled
    memoryType = builder.memoryType
    keepCancelledFetchAsLowPriority = builder.keepCancelledFetchAsLowPriority
    downsampleIfLargeBitmap = builder.downsampleIfLargeBitmap
    isEncodedCacheEnabled = builder.encodedCacheEnabled
    isEnsureTranscoderLibraryLoaded = builder.ensureTranscoderLibraryLoaded
    isEncodedMemoryCacheProbingEnabled = builder.isEncodedMemoryCacheProbingEnabled
    isDiskCacheProbingEnabled = builder.isDiskCacheProbingEnabled
    trackedKeysSize = builder.trackedKeysSize
    allowDelay = builder.allowDelay
    handOffOnUiThreadOnly = builder.handOffOnUiThreadOnly
    shouldStoreCacheEntrySize = builder.shouldStoreCacheEntrySize
    shouldIgnoreCacheSizeMismatch = builder.shouldIgnoreCacheSizeMismatch
    shouldUseDecodingBufferHelper = builder.shouldUseDecodingBufferHelper
  }

  companion object {
    @JvmStatic
    fun newBuilder(configBuilder: ImagePipelineConfig.Builder): Builder = Builder(configBuilder)
  }
}
