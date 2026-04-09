/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ig.imageloader.sample

import android.content.Context
import android.util.Log
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.facebook.quicklog.NoOpQuickPerformanceLogger
import com.facebook.storage.supplier.igapps.IGAppsStorageDependencySupplier
import com.instagram.common.api.base.httprequest.HttpRequestPolicy
import com.instagram.common.cache.igdiskcache.intf.EditorOutputStream
import com.instagram.common.cache.igdiskcache.intf.IgPersistentCache
import com.instagram.common.cache.igdiskcache.intf.OptionalStream
import com.instagram.common.cache.igdiskcache.intf.SnapshotCacheItem
import com.instagram.common.cache.igdiskcache.intf.SnapshotInputStream
import com.instagram.common.cache.igdiskcache.intf.SnapshotMetadata
import com.instagram.common.cache.image.IgImageInfraBuilder
import com.instagram.common.cache.image.IgImageInfraJavaImpl
import com.instagram.common.cache.image.diskcachelayer.DiskCacheFactoryImpl
import com.instagram.common.cache.image.diskcachelayer.SharedDiskCacheFactory
import com.instagram.common.cache.image.diskcachelayer.intf.DiskCacheFactory
import com.instagram.common.cache.image.memorycachelayer.data.CacheType
import com.instagram.common.cache.image.memorycachelayer.data.Configuration
import com.instagram.common.cache.image.memorycachelayer.intf.InMemoryBitmapCacheIntf
import com.instagram.common.cache.image.networklayer.IgFetchConfig
import com.instagram.common.cache.image.networklayer.IgUnifiedImageNetworkLayerImpl
import com.instagram.common.cache.image.networklayer.NetworkImageLoaderFactoryImpl
import com.instagram.common.cache.image.networklayer.intf.NetworkImageLoader
import com.instagram.common.cache.image.utils.IgImageInfraConstants
import com.instagram.common.cache.image.utils.UnsafeImageTaskConfig
import com.instagram.common.context.AppContext
import com.instagram.common.session.Session
import com.instagram.common.storage.cask.IgCask
import com.instagram.common.typedurl.SimpleImageUrl
import com.instagram.common.util.concurrent.LoggedRunnable
import com.instagram.criticalpath.CriticalPath
import com.instagram.criticalpath.CriticalPathJobDispatcher
import com.instagram.fresco.cache.IgBitmapMemoryCacheFactory
import com.instagram.fresco.cache.IgFrescoBitmapCacheAdapter
import com.meta.images.network.UnifiedImageNetworkLayer
import com.meta.images.network.UnifiedImageNetworkLayerAdapter

/**
 * Builds real IG image infrastructure for the sample app.
 *
 * Uses real [IgImageInfraBuilder] construction, wiring real memory cache, disk cache factory, and
 * network factory.
 */
object SampleInfraFactory {

  private const val TAG = "IgImageLoaderSample"
  private const val DISK_CACHE_DIR = "ig_images"
  private const val DISK_CACHE_SIZE_BYTES = 500L * 1024 * 1024
  private const val DISK_CACHE_TRIM_FACTOR = 0.2
  private const val DISK_CACHE_STALE_DAYS = 30

  var storageBootstrapped = false
    private set

  /**
   * Bootstrap IG's storage chain: [IGAppsStorageDependencySupplier] → [IgCask].
   *
   * Must be called before [createDiskCacheFactory]. If it fails (e.g., CriticalPath blocker), disk
   * cache toggles fall back to no-op.
   */
  fun bootstrapStorage(context: Context) {
    try {
      AppContext.setContext(context)

      // Initialize CriticalPath with a no-op dispatcher to prevent the infinite
      // while (!CriticalPath.initialized) { Thread.sleep(300) } loop in
      // IGAppsStorageDependencySupplier.criticalPathJobDispatcher getter.
      // The IDLE executor used by StashFactory.createWithKeyCache() and
      // IgStashFactory.registerIgDiskStashWithCask() submits tasks through this dispatcher.
      if (!CriticalPath.initialized) {
        val noOpDispatcher =
            object : CriticalPathJobDispatcher {
              override fun submitJobToMainThread(runnable: LoggedRunnable?) {
                // No-op: drop idle tasks that require full IG infrastructure
              }

              override fun submitJobToBackgroundThread(runnable: LoggedRunnable) {
                // No-op: drop idle tasks (cask registration, key cache warmup)
              }

              override fun submitJobToBackgroundThread(
                  runnable: LoggedRunnable,
                  override: Boolean,
              ) {
                // No-op
              }

              override fun submitJobToUiCriticalThread(runnable: LoggedRunnable) {
                // No-op
              }
            }
        CriticalPath::class.java.getDeclaredField("initialized").apply {
          isAccessible = true
          setBoolean(null, true)
        }
        CriticalPath::class.java.getDeclaredField("jobDispatcher").apply {
          isAccessible = true
          set(null, noOpDispatcher)
        }
        Log.d(TAG, "CriticalPath initialized with no-op dispatcher")
      }

      val noOpQPL = NoOpQuickPerformanceLogger()
      val supplier = IGAppsStorageDependencySupplier.initialize(context, noOpQPL)
      IgCask.igInit(supplier)
      storageBootstrapped = true
      Log.d(TAG, "Storage chain bootstrapped successfully")
    } catch (e: Throwable) {
      Log.e(TAG, "Storage chain bootstrap failed — disk cache toggles disabled", e)
    }
  }

  fun createMemoryCache(
      context: Context,
      useFrescoCache: Boolean,
  ): InMemoryBitmapCacheIntf {
    val cache =
        IgBitmapMemoryCacheFactory()
            .create(
                useFrescoCache,
                context,
                CacheType.JavaBitmap,
                Configuration.DEFAULT_CONFIG,
                null,
            )
    Log.d(
        TAG,
        "MemoryCache created: useFrescoCache=$useFrescoCache type=${cache.javaClass.simpleName}",
    )
    return cache
  }

  fun createDiskCacheFactory(
      context: Context,
      useSharedCache: Boolean,
  ): DiskCacheFactory {
    if (!storageBootstrapped) {
      Log.w(TAG, "Storage not bootstrapped — using no-op disk cache")
      return NoOpDiskCacheFactory
    }
    return if (useSharedCache) {
      Log.d(TAG, "DiskCache created: SharedDiskCacheFactory (shared with Fresco)")
      SharedDiskCacheFactory(
          context,
          DISK_CACHE_DIR,
          DISK_CACHE_SIZE_BYTES,
          DISK_CACHE_TRIM_FACTOR,
          DISK_CACHE_STALE_DAYS,
      )
    } else {
      Log.d(TAG, "DiskCache created: DiskCacheFactoryImpl (IG-only)")
      DiskCacheFactoryImpl(
          context,
          DISK_CACHE_DIR,
          DISK_CACHE_SIZE_BYTES,
          DISK_CACHE_TRIM_FACTOR,
          DISK_CACHE_STALE_DAYS,
      )
    }
  }

  fun buildIgImageInfra(
      context: Context,
      session: Session,
      memoryCache: InMemoryBitmapCacheIntf,
      diskCacheFactory: DiskCacheFactory,
  ): IgImageInfraJavaImpl {
    val infra =
        IgImageInfraBuilder(session, NetworkImageLoaderFactoryImpl, memoryCache, diskCacheFactory)
            .setContext(context)
            .build()
    IgImageInfraJavaImpl.setInstance(infra)
    Log.d(TAG, "Real IgImageInfraJavaImpl built via IgImageInfraBuilder")
    return infra
  }

  /**
   * Builds [ImagePipelineConfig] for Fresco, optionally sharing IG's memory cache and routing
   * network through IG's unified network layer.
   */
  fun buildFrescoImagePipelineConfig(
      context: Context,
      toggles: Map<String, Boolean>,
      memoryCache: InMemoryBitmapCacheIntf,
      session: Session,
  ): ImagePipelineConfig {
    val builder = ImagePipelineConfig.newBuilder(context)

    // Share memory cache between IG and Fresco pipelines (production pattern)
    if (memoryCache is IgFrescoBitmapCacheAdapter) {
      builder.setBitmapMemoryCacheFactory { _, _, _, _, _, _ -> memoryCache.countingMemoryCache }
      Log.d(TAG, "Fresco config: sharing IgFrescoBitmapCacheAdapter with Fresco pipeline")
    }

    // Route Fresco network through IG's unified network layer
    if (toggles["use_fresco_network_pipeline"] == true) {
      try {
        val imageInfra = IgImageInfraJavaImpl.getInstance()
        val loaderHost = imageInfra?.getImageLoaderHost()
        if (loaderHost != null) {
          val networkLayerFactory:
              (com.facebook.imagepipeline.producers.ProducerContext) -> UnifiedImageNetworkLayer =
              { producerContext ->
                val uri = producerContext.imageRequest.sourceUri
                val imageUrl = SimpleImageUrl(uri.toString())
                val fetchConfig =
                    IgFetchConfig(
                        fetcherHost = loaderHost,
                        imageUri = imageUrl,
                        startScan = 0,
                        endScan = IgImageInfraConstants.ALL_SCANS,
                        byteArray = null,
                        estimatedScansSizesBytes = imageUrl.estimatedScansSizesBytes,
                        diskCacheKey = uri.toString(),
                        unsafeConfig = UnsafeImageTaskConfig(),
                        imageRefreshMaxProgress = 0,
                        requestPolicy =
                            HttpRequestPolicy.Builder()
                                .setRequestType(HttpRequestPolicy.RequestType.Image)
                                .build(),
                        failOnErrorHttpResponse = true,
                        photosQPL = null,
                        isRequestedByImageView = true,
                        trafficTokenProvider = NetworkImageLoader.TrafficTokenProvider { null },
                        origin = "sample_app",
                        session = session,
                        enableProgressiveStreaming =
                            toggles["use_fresco_progressive_rendering"] == true,
                    )
                IgUnifiedImageNetworkLayerImpl(NetworkImageLoaderFactoryImpl, fetchConfig)
              }
          builder.setNetworkFetcher(UnifiedImageNetworkLayerAdapter(networkLayerFactory))
          Log.d(TAG, "Fresco config: network routed through UnifiedImageNetworkLayerAdapter")
        } else {
          Log.w(TAG, "Fresco network toggle ON but IgImageInfra not available")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to set up Fresco network toggle", e)
      }
    }

    // Enable progressive JPEG rendering when progressive streaming toggle is ON
    if (toggles["use_fresco_progressive_rendering"] == true) {
      builder.setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
      Log.d(TAG, "Fresco config: progressive JPEG rendering enabled")
    }

    return builder.build()
  }

  /** No-op [DiskCacheFactory] used when storage bootstrap fails. */
  private object NoOpDiskCacheFactory : DiskCacheFactory {
    override fun create(): IgPersistentCache = NoOpPersistentCache
  }

  private object NoOpPersistentCache : IgPersistentCache {
    override fun isProbablyInCache(key: String): Boolean = false

    override fun has(key: String): Boolean = false

    override fun get(
        key: String,
        annotations: Map<String, String>?,
    ): OptionalStream<SnapshotInputStream> = OptionalStream.absent()

    override fun getWithMetadata(
        key: String,
        annotations: Map<String, String>?,
    ): OptionalStream<SnapshotCacheItem> = OptionalStream.absent()

    override fun edit(
        key: String,
        annotations: Map<String, String>?,
    ): OptionalStream<EditorOutputStream> = OptionalStream.absent()

    override fun editWithMetadata(
        key: String,
        metadata: SnapshotMetadata?,
        isMetadataCrucial: Boolean,
        annotations: Map<String, String>?,
    ): OptionalStream<EditorOutputStream> = OptionalStream.absent()

    override fun getEntrySize(key: String): Long = 0

    override fun remove(key: String, annotations: Map<String, String>?) {}

    override fun clear() {}

    override fun close() {}

    override fun getMaxSizeInBytes(): Long = 0

    override fun setMaxSizeInBytes(maxSizeInBytes: Long) {}

    override fun size(): Long = 0

    override fun count(): Int = 0

    override val keys: Set<String> = emptySet()

    override fun requestHoldItem(key: String) {}
  }
}
