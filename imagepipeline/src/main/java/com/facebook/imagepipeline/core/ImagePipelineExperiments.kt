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
import com.facebook.imagepipeline.cache.CacheKeyFactory
import com.facebook.imagepipeline.cache.MemoryCache
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.platform.PlatformDecoderOptions
import com.facebook.imageutils.BitmapUtil

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
  val useBalancedAnimationStrategy: Boolean
  val animationStrategyBufferLengthMilliseconds: Int
  val bitmapPrepareToDrawMinSizeBytes: Int
  val bitmapPrepareToDrawMaxSizeBytes: Int
  val bitmapPrepareToDrawForPrefetch: Boolean
  val maxBitmapDimension: Int
  val isNativeCodeDisabled: Boolean
  val isPartialImageCachingEnabled: Boolean
  val producerFactoryMethod: ProducerFactoryMethod
  val isLazyDataSource: Supplier<Boolean>
  val isGingerbreadDecoderEnabled: Boolean
  val downscaleFrameToDrawableDimensions: Boolean
  val suppressBitmapPrefetchingSupplier: Supplier<Boolean>
  val isExperimentalThreadHandoffQueueEnabled: Boolean
  val memoryType: Long
  val keepCancelledFetchAsLowPriority: Boolean
  val downsampleIfLargeBitmap: Boolean
  val isEncodedCacheEnabled: Boolean
  val isEnsureTranscoderLibraryLoaded: Boolean
  val isEncodedMemoryCacheProbingEnabled: Boolean
  val isDiskCacheProbingEnabled: Boolean
  val trackedKeysSize: Int
  val allowDelay: Boolean
  val handOffOnUiThreadOnly: Boolean
  val shouldStoreCacheEntrySize: Boolean
  val shouldIgnoreCacheSizeMismatch: Boolean
  val shouldUseDecodingBufferHelper: Boolean
  val allowProgressiveOnPrefetch: Boolean
  val cancelDecodeOnCacheMiss: Boolean
  val animationRenderFpsLimit: Int
  val prefetchShortcutEnabled: Boolean
  val platformDecoderOptions: PlatformDecoderOptions
  val isBinaryXmlEnabled: Boolean

  class Builder(private val configBuilder: ImagePipelineConfig.Builder) {
    @JvmField var shouldUseDecodingBufferHelper = false
    @JvmField var webpSupportEnabled = false
    @JvmField var webpErrorLogger: WebpErrorLogger? = null
    @JvmField var decodeCancellationEnabled = false
    @JvmField var webpBitmapFactory: WebpBitmapFactory? = null
    @JvmField var useDownsamplingRatioForResizing = false
    @JvmField var useBitmapPrepareToDraw = false
    @JvmField var useBalancedAnimationStrategy = false
    @JvmField var animationStrategyBufferLengthMilliseconds = 1000
    @JvmField var bitmapPrepareToDrawMinSizeBytes = 0
    @JvmField var bitmapPrepareToDrawMaxSizeBytes = 0

    @JvmField var bitmapPrepareToDrawForPrefetch = false
    @JvmField var maxBitmapDimension = BitmapUtil.MAX_BITMAP_DIMENSION.toInt()
    @JvmField var nativeCodeDisabled = false
    @JvmField var isPartialImageCachingEnabled = false
    @JvmField var producerFactoryMethod: ProducerFactoryMethod? = null

    @JvmField var lazyDataSource: Supplier<Boolean>? = null

    @JvmField var gingerbreadDecoderEnabled = false

    @JvmField var downscaleFrameToDrawableDimensions = false

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
    @JvmField var allowProgressiveOnPrefetch = false
    @JvmField var animationRenderFpsLimit = 30
    @JvmField var cancelDecodeOnCacheMiss = false
    @JvmField var prefetchShortcutEnabled = false

    @JvmField var platformDecoderOptions = PlatformDecoderOptions()

    @JvmField var isBinaryXmlEnabled = false

    private fun asBuilder(block: () -> Unit): Builder {
      block()
      return this
    }

    fun setHandOffOnUiThreadOnly(handOffOnUiThreadOnly: Boolean) = asBuilder {
      this.handOffOnUiThreadOnly = handOffOnUiThreadOnly
    }

    fun setStoreCacheEntrySize(shouldStoreCacheEntrySize: Boolean) = asBuilder {
      this.shouldStoreCacheEntrySize = shouldStoreCacheEntrySize
    }

    fun setIgnoreCacheSizeMismatch(shouldIgnoreCacheSizeMismatch: Boolean) = asBuilder {
      this.shouldIgnoreCacheSizeMismatch = shouldIgnoreCacheSizeMismatch
    }

    fun setWebpSupportEnabled(webpSupportEnabled: Boolean) = asBuilder {
      this.webpSupportEnabled = webpSupportEnabled
    }

    fun setPrefetchShortcutEnabled(prefetchShortcutEnabled: Boolean) = asBuilder {
      this.prefetchShortcutEnabled = prefetchShortcutEnabled
    }

    fun shouldUseDecodingBufferHelper(): Boolean = shouldUseDecodingBufferHelper

    fun setShouldUseDecodingBufferHelper(shouldUseDecodingBufferHelper: Boolean) = asBuilder {
      this.shouldUseDecodingBufferHelper = shouldUseDecodingBufferHelper
    }

    fun setUseDownsampligRatioForResizing(useDownsamplingRatioForResizing: Boolean) = asBuilder {
      this.useDownsamplingRatioForResizing = useDownsamplingRatioForResizing
    }

    /**
     * Enables the caching of partial image data, for example if the request is cancelled or fails
     * after some data has been received.
     */
    fun setPartialImageCachingEnabled(partialImageCachingEnabled: Boolean) = asBuilder {
      isPartialImageCachingEnabled = partialImageCachingEnabled
    }

    /**
     * If true we cancel decoding jobs when the related request has been cancelled
     *
     * @param decodeCancellationEnabled If true the decoding of cancelled requests are cancelled
     * @return The Builder itself for chaining
     */
    fun setDecodeCancellationEnabled(decodeCancellationEnabled: Boolean) = asBuilder {
      this.decodeCancellationEnabled = decodeCancellationEnabled
    }

    fun setWebpErrorLogger(webpErrorLogger: WebpErrorLogger?) = asBuilder {
      this.webpErrorLogger = webpErrorLogger
    }

    fun setWebpBitmapFactory(webpBitmapFactory: WebpBitmapFactory?) = asBuilder {
      this.webpBitmapFactory = webpBitmapFactory
    }

    /**
     * If enabled, the pipeline will call [android.graphics.Bitmap.prepareToDraw] after decoding.
     * This potentially reduces lag on Android N+ as this step now happens async when the
     * RendererThread is idle.
     *
     * @param useBitmapPrepareToDraw set true for enabling prepareToDraw
     * @param minBitmapSizeBytes Bitmaps with a [Bitmap.getByteCount] smaller than this value are
     *   not uploaded
     * @param maxBitmapSizeBytes Bitmaps with a [Bitmap.getByteCount] larger than this value are not
     *   uploaded
     * @param preparePrefetch If this is true, also pre-fetching image requests will trigger the
     *   [android.graphics.Bitmap.prepareToDraw] call.
     * @return The Builder itself for chaining
     */
    fun setBitmapPrepareToDraw(
        useBitmapPrepareToDraw: Boolean,
        minBitmapSizeBytes: Int,
        maxBitmapSizeBytes: Int,
        preparePrefetch: Boolean
    ) = asBuilder {
      this.useBitmapPrepareToDraw = useBitmapPrepareToDraw
      this.bitmapPrepareToDrawMinSizeBytes = minBitmapSizeBytes
      this.bitmapPrepareToDrawMaxSizeBytes = maxBitmapSizeBytes
      this.bitmapPrepareToDrawForPrefetch = preparePrefetch
    }

    /** Enable balance strategy between RAM and CPU for rendering bitmap animations (WebP, Gif) */
    fun setBalancedAnimationStrategy(useBalancedAnimationStrategy: Boolean) = asBuilder {
      this.useBalancedAnimationStrategy = useBalancedAnimationStrategy
    }

    /** The balanced animation strategy buffer length for single animation */
    fun setAnimationStrategyBufferLengthMilliseconds(
        animationStrategyBufferLengthMilliseconds: Int
    ) = asBuilder {
      this.animationStrategyBufferLengthMilliseconds = animationStrategyBufferLengthMilliseconds
    }

    /**
     * Sets the maximum bitmap size use to compute the downsampling value when decoding Jpeg images.
     */
    fun setMaxBitmapDimension(maxBitmapDimension: Int) = asBuilder {
      this.maxBitmapDimension = maxBitmapDimension
    }

    /**
     * If true, the pipeline will use alternative implementations without native code.
     *
     * @param nativeCodeDisabled set true for disabling native implementation.
     * @return The Builder itself for chaining
     */
    fun setNativeCodeDisabled(nativeCodeDisabled: Boolean) = asBuilder {
      this.nativeCodeDisabled = nativeCodeDisabled
    }

    /**
     * Stores an alternative method to instantiate the [ProducerFactory]. This allows experimenting
     * with overridden producers.
     */
    fun setProducerFactoryMethod(producerFactoryMethod: ProducerFactoryMethod?) = asBuilder {
      this.producerFactoryMethod = producerFactoryMethod
    }

    /** Stores an alternative lazy method to instantiate the data souce. */
    fun setLazyDataSource(lazyDataSource: Supplier<Boolean>?) = asBuilder {
      this.lazyDataSource = lazyDataSource
    }

    fun setGingerbreadDecoderEnabled(gingerbreadDecoderEnabled: Boolean) = asBuilder {
      this.gingerbreadDecoderEnabled = gingerbreadDecoderEnabled
    }

    fun setShouldDownscaleFrameToDrawableDimensions(downscaleFrameToDrawableDimensions: Boolean) =
        asBuilder {
          this.downscaleFrameToDrawableDimensions = downscaleFrameToDrawableDimensions
        }

    fun setSuppressBitmapPrefetchingSupplier(suppressBitmapPrefetchingSupplier: Supplier<Boolean>) =
        asBuilder {
          this.suppressBitmapPrefetchingSupplier = suppressBitmapPrefetchingSupplier
        }

    fun setExperimentalThreadHandoffQueueEnabled(experimentalThreadHandoffQueueEnabled: Boolean) =
        asBuilder {
          this.experimentalThreadHandoffQueueEnabled = experimentalThreadHandoffQueueEnabled
        }

    fun setExperimentalMemoryType(MemoryType: Long) = asBuilder { this.memoryType = MemoryType }

    fun setKeepCancelledFetchAsLowPriority(keepCancelledFetchAsLowPriority: Boolean) = asBuilder {
      this.keepCancelledFetchAsLowPriority = keepCancelledFetchAsLowPriority
    }

    fun setDownsampleIfLargeBitmap(downsampleIfLargeBitmap: Boolean) = asBuilder {
      this.downsampleIfLargeBitmap = downsampleIfLargeBitmap
    }

    fun setEncodedCacheEnabled(encodedCacheEnabled: Boolean) = asBuilder {
      this.encodedCacheEnabled = encodedCacheEnabled
    }

    fun setEnsureTranscoderLibraryLoaded(ensureTranscoderLibraryLoaded: Boolean) = asBuilder {
      this.ensureTranscoderLibraryLoaded = ensureTranscoderLibraryLoaded
    }

    fun setIsDiskCacheProbingEnabled(isDiskCacheProbingEnabled: Boolean) = asBuilder {
      this.isDiskCacheProbingEnabled = isDiskCacheProbingEnabled
    }

    fun setIsEncodedMemoryCacheProbingEnabled(isEncodedMemoryCacheProbingEnabled: Boolean) =
        asBuilder {
          this.isEncodedMemoryCacheProbingEnabled = isEncodedMemoryCacheProbingEnabled
        }

    fun setTrackedKeysSize(trackedKeysSize: Int) = asBuilder {
      this.trackedKeysSize = trackedKeysSize
    }

    fun setAllowDelay(allowDelay: Boolean) = asBuilder { this.allowDelay = allowDelay }

    fun setAllowProgressiveOnPrefetch(allowProgressiveOnPrefetch: Boolean) = asBuilder {
      this.allowProgressiveOnPrefetch = allowProgressiveOnPrefetch
    }

    fun setAnimationRenderFpsLimit(animationRenderFpsLimit: Int) = asBuilder {
      this.animationRenderFpsLimit = animationRenderFpsLimit
    }

    fun setCancelDecodeOnCacheMiss(cancelDecodeOnCacheMiss: Boolean) = asBuilder {
      this.cancelDecodeOnCacheMiss = cancelDecodeOnCacheMiss
    }

    fun setPlatformDecoderOptions(platformDecoderOptions: PlatformDecoderOptions) = asBuilder {
      this.platformDecoderOptions = platformDecoderOptions
    }

    fun setBinaryXmlEnabled(binaryXmlEnabled: Boolean) = asBuilder {
      isBinaryXmlEnabled = binaryXmlEnabled
    }

    fun build(): ImagePipelineExperiments = ImagePipelineExperiments(this)
  }

  interface ProducerFactoryMethod {
    fun createProducerFactory(
        context: Context,
        byteArrayPool: ByteArrayPool,
        imageDecoder: ImageDecoder,
        progressiveJpegConfig: ProgressiveJpegConfig,
        downsampleMode: DownsampleMode,
        resizeAndRotateEnabledForNetwork: Boolean,
        decodeCancellationEnabled: Boolean,
        executorSupplier: ExecutorSupplier,
        pooledByteBufferFactory: PooledByteBufferFactory,
        pooledByteStreams: PooledByteStreams,
        bitmapMemoryCache: MemoryCache<CacheKey?, CloseableImage?>,
        encodedMemoryCache: MemoryCache<CacheKey?, PooledByteBuffer?>,
        diskCachesStoreSupplier: Supplier<DiskCachesStore>,
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
        downsampleMode: DownsampleMode,
        resizeAndRotateEnabledForNetwork: Boolean,
        decodeCancellationEnabled: Boolean,
        executorSupplier: ExecutorSupplier,
        pooledByteBufferFactory: PooledByteBufferFactory,
        pooledByteStreams: PooledByteStreams,
        bitmapMemoryCache: MemoryCache<CacheKey?, CloseableImage?>,
        encodedMemoryCache: MemoryCache<CacheKey?, PooledByteBuffer?>,
        diskCachesStoreSupplier: Supplier<DiskCachesStore>,
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
            downsampleMode,
            resizeAndRotateEnabledForNetwork,
            decodeCancellationEnabled,
            executorSupplier!!,
            pooledByteBufferFactory!!,
            bitmapMemoryCache!!,
            encodedMemoryCache!!,
            diskCachesStoreSupplier,
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
    useBalancedAnimationStrategy = builder.useBalancedAnimationStrategy
    animationStrategyBufferLengthMilliseconds = builder.animationStrategyBufferLengthMilliseconds
    bitmapPrepareToDrawMinSizeBytes = builder.bitmapPrepareToDrawMinSizeBytes
    bitmapPrepareToDrawMaxSizeBytes = builder.bitmapPrepareToDrawMaxSizeBytes
    bitmapPrepareToDrawForPrefetch = builder.bitmapPrepareToDrawForPrefetch
    maxBitmapDimension = builder.maxBitmapDimension
    isNativeCodeDisabled = builder.nativeCodeDisabled
    isPartialImageCachingEnabled = builder.isPartialImageCachingEnabled
    producerFactoryMethod = builder.producerFactoryMethod ?: DefaultProducerFactoryMethod()
    isLazyDataSource = builder.lazyDataSource ?: Suppliers.BOOLEAN_FALSE
    isGingerbreadDecoderEnabled = builder.gingerbreadDecoderEnabled
    downscaleFrameToDrawableDimensions = builder.downscaleFrameToDrawableDimensions
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
    allowProgressiveOnPrefetch = builder.allowProgressiveOnPrefetch
    animationRenderFpsLimit = builder.animationRenderFpsLimit
    allowDelay = builder.allowDelay
    handOffOnUiThreadOnly = builder.handOffOnUiThreadOnly
    shouldStoreCacheEntrySize = builder.shouldStoreCacheEntrySize
    shouldIgnoreCacheSizeMismatch = builder.shouldIgnoreCacheSizeMismatch
    shouldUseDecodingBufferHelper = builder.shouldUseDecodingBufferHelper
    cancelDecodeOnCacheMiss = builder.cancelDecodeOnCacheMiss
    prefetchShortcutEnabled = builder.prefetchShortcutEnabled
    platformDecoderOptions = builder.platformDecoderOptions
    isBinaryXmlEnabled = builder.isBinaryXmlEnabled
  }

  companion object {
    @JvmStatic
    fun newBuilder(configBuilder: ImagePipelineConfig.Builder): Builder = Builder(configBuilder)
  }
}
