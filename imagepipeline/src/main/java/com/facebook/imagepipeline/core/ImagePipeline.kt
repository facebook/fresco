/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import android.net.Uri
import android.os.StrictMode
import bolts.CancellationTokenSource
import bolts.Continuation
import bolts.Task
import com.facebook.cache.common.CacheKey
import com.facebook.callercontext.CallerContextVerifier
import com.facebook.common.internal.Objects
import com.facebook.common.internal.Predicate
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.common.util.UriUtil
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSources
import com.facebook.datasource.SimpleDataSource
import com.facebook.fresco.urimod.UriModifier
import com.facebook.imagepipeline.cache.CacheKeyFactory
import com.facebook.imagepipeline.cache.MemoryCache
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.datasource.CloseableProducerToDataSourceAdapter
import com.facebook.imagepipeline.datasource.ProducerToDataSourceAdapter.Companion.create
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.listener.ForwardingRequestListener
import com.facebook.imagepipeline.listener.ForwardingRequestListener2
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.listener.RequestListener2
import com.facebook.imagepipeline.producers.InternalRequestListener
import com.facebook.imagepipeline.producers.Producer
import com.facebook.imagepipeline.producers.SettableProducerContext
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequest.CacheChoice
import com.facebook.imagepipeline.request.ImageRequest.RequestLevel
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.facebook.imagepipeline.systrace.FrescoSystrace.traceSection
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.concurrent.ThreadSafe

/** The entry point for the image pipeline. */
@ThreadSafe
class ImagePipeline(
    val producerSequenceFactory: ProducerSequenceFactory,
    requestListeners: Set<RequestListener?>,
    requestListener2s: Set<RequestListener2?>,
    private val isPrefetchEnabledSupplier: Supplier<Boolean>,
    bitmapMemoryCache: MemoryCache<CacheKey, CloseableImage>,
    encodedMemoryCache: MemoryCache<CacheKey, PooledByteBuffer>,
    private val diskCachesStoreSupplier: Supplier<DiskCachesStore>,
    cacheKeyFactory: CacheKeyFactory,
    threadHandoffProducerQueue: ThreadHandoffProducerQueue,
    suppressBitmapPrefetchingSupplier: Supplier<Boolean>,
    lazyDataSource: Supplier<Boolean>,
    callerContextVerifier: CallerContextVerifier?,
    config: ImagePipelineConfigInterface
) {

  private val requestListener: RequestListener = ForwardingRequestListener(requestListeners)
  private val requestListener2: RequestListener2 = ForwardingRequestListener2(requestListener2s)

  /** @return The Bitmap MemoryCache */
  val bitmapMemoryCache: MemoryCache<CacheKey, CloseableImage>
  private val encodedMemoryCache: MemoryCache<CacheKey, PooledByteBuffer>

  /** @return The CacheKeyFactory implementation used by ImagePipeline */
  val cacheKeyFactory: CacheKeyFactory
  private val threadHandoffProducerQueue: ThreadHandoffProducerQueue
  private val suppressBitmapPrefetchingSupplier: Supplier<Boolean>
  private val idCounter: AtomicLong = AtomicLong()
  val isLazyDataSource: Supplier<Boolean>
  private val callerContextVerifier: CallerContextVerifier?
  val config: ImagePipelineConfigInterface

  /**
   * Generates unique id for RequestFuture.
   *
   * @return unique id
   */
  fun generateUniqueFutureId(): String = idCounter.getAndIncrement().toString()

  /**
   * Returns a DataSource supplier that will on get submit the request for execution and return a
   * DataSource representing the pending results of the task.
   *
   * @param imageRequest the request to submit (what to execute).
   * @param callerContext the caller context of the caller of data source supplier
   * @param requestLevel which level to look down until for the image
   * @return a DataSource representing pending results and completion of the request
   */
  fun getDataSourceSupplier(
      imageRequest: ImageRequest,
      callerContext: Any?,
      requestLevel: RequestLevel?
  ): Supplier<DataSource<CloseableReference<CloseableImage>>> {
    return object : Supplier<DataSource<CloseableReference<CloseableImage>>> {
      override fun get() = fetchDecodedImage(imageRequest, callerContext, requestLevel)

      override fun toString(): String =
          Objects.toStringHelper(this).add("uri", imageRequest.sourceUri).toString()
    }
  }

  /**
   * Returns a DataSource supplier that will on get submit the request for execution and return a
   * DataSource representing the pending results of the task.
   *
   * @param imageRequest the request to submit (what to execute).
   * @param callerContext the caller context of the caller of data source supplier
   * @param requestLevel which level to look down until for the image
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @return a DataSource representing pending results and completion of the request
   */
  fun getDataSourceSupplier(
      imageRequest: ImageRequest,
      callerContext: Any?,
      requestLevel: RequestLevel?,
      requestListener: RequestListener?
  ): Supplier<DataSource<CloseableReference<CloseableImage>>> {
    return object : Supplier<DataSource<CloseableReference<CloseableImage>>> {
      override fun get() =
          fetchDecodedImage(imageRequest, callerContext, requestLevel, requestListener)

      override fun toString(): String =
          Objects.toStringHelper(this).add("uri", imageRequest.sourceUri).toString()
    }
  }

  /**
   * Returns a DataSource supplier that will on get submit the request for execution and return a
   * DataSource representing the pending results of the task.
   *
   * @param imageRequest the request to submit (what to execute).
   * @param callerContext the caller context of the caller of data source supplier
   * @param requestLevel which level to look down until for the image
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @param uiComponentId optional UI component ID requesting the image
   * @return a DataSource representing pending results and completion of the request
   */
  fun getDataSourceSupplier(
      imageRequest: ImageRequest,
      callerContext: Any?,
      requestLevel: RequestLevel?,
      requestListener: RequestListener?,
      uiComponentId: String?
  ): Supplier<DataSource<CloseableReference<CloseableImage>>> {
    return object : Supplier<DataSource<CloseableReference<CloseableImage>>> {
      override fun get() =
          fetchDecodedImage(
              imageRequest, callerContext, requestLevel, requestListener, uiComponentId)

      override fun toString(): String =
          Objects.toStringHelper(this).add("uri", imageRequest.sourceUri).toString()
    }
  }

  /**
   * Returns a DataSource supplier that will on get submit the request for execution and return a
   * DataSource representing the pending results of the task.
   *
   * @param imageRequest the request to submit (what to execute).
   * @return a DataSource representing pending results and completion of the request
   */
  fun getEncodedImageDataSourceSupplier(
      imageRequest: ImageRequest,
      callerContext: Any?
  ): Supplier<DataSource<CloseableReference<PooledByteBuffer>>> {
    return object : Supplier<DataSource<CloseableReference<PooledByteBuffer>>> {
      override fun get() = fetchEncodedImage(imageRequest, callerContext)

      override fun toString(): String =
          Objects.toStringHelper(this).add("uri", imageRequest.sourceUri).toString()
    }
  }

  /**
   * Submits a request for bitmap cache lookup.
   *
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @return a DataSource representing the image
   */
  fun fetchImageFromBitmapCache(
      imageRequest: ImageRequest,
      callerContext: Any?
  ): DataSource<CloseableReference<CloseableImage>> =
      fetchDecodedImage(imageRequest, callerContext, RequestLevel.BITMAP_MEMORY_CACHE)

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   *
   * The returned DataSource must be closed once the client has finished with it.
   *
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @param lowestPermittedRequestLevelOnSubmit the lowest request level permitted for image reques
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @param uiComponentId optional UI component ID that is requesting the image
   * @return a DataSource representing the pending decoded image(s)
   */
  fun fetchDecodedImage(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      lowestPermittedRequestLevelOnSubmit: RequestLevel? = null,
      requestListener: RequestListener? = null,
      uiComponentId: String? = null
  ): DataSource<CloseableReference<CloseableImage>> {
    if (imageRequest == null) {
      return DataSources.immediateFailedDataSource(NullPointerException())
    }
    return try {
      val producerSequence = producerSequenceFactory.getDecodedImageProducerSequence(imageRequest)
      submitFetchRequest(
          producerSequence,
          imageRequest,
          lowestPermittedRequestLevelOnSubmit ?: RequestLevel.FULL_FETCH,
          callerContext,
          requestListener,
          uiComponentId)
    } catch (exception: Exception) {
      DataSources.immediateFailedDataSource(exception)
    }
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   *
   * The returned DataSource must be closed once the client has finished with it.
   *
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @return a DataSource representing the pending decoded image(s)
   */
  fun fetchDecodedImage(
      imageRequest: ImageRequest?,
      callerContext: Any?
  ): DataSource<CloseableReference<CloseableImage>> {
    return fetchDecodedImage(imageRequest, callerContext, null)
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   *
   * The returned DataSource must be closed once the client has finished with it.
   *
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @return a DataSource representing the pending decoded image(s)
   */
  fun fetchDecodedImage(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      requestListener: RequestListener
  ): DataSource<CloseableReference<CloseableImage>> {
    return fetchDecodedImage(
        imageRequest,
        callerContext,
        lowestPermittedRequestLevelOnSubmit = null,
        requestListener = requestListener)
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   *
   * The returned DataSource must be closed once the client has finished with it.
   *
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @param lowestPermittedRequestLevelOnSubmit the lowest request level permitted for image reques
   * @return a DataSource representing the pending decoded image(s)
   */
  fun fetchDecodedImage(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      lowestPermittedRequestLevelOnSubmit: RequestLevel,
  ): DataSource<CloseableReference<CloseableImage>> {
    return fetchDecodedImage(
        imageRequest,
        callerContext,
        lowestPermittedRequestLevelOnSubmit = lowestPermittedRequestLevelOnSubmit,
        requestListener = null)
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   *
   * The returned DataSource must be closed once the client has finished with it.
   *
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @param lowestPermittedRequestLevelOnSubmit the lowest request level permitted for image request
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @param uiComponentId optional UI component ID that is requesting the image
   * @param extras optional extra data
   * @return a DataSource representing the pending decoded image(s)
   */
  fun fetchDecodedImage(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      lowestPermittedRequestLevelOnSubmit: RequestLevel,
      requestListener: RequestListener?,
      uiComponentId: String?,
      extras: Map<String, *>?
  ): DataSource<CloseableReference<CloseableImage>> {
    if (imageRequest == null) {
      return DataSources.immediateFailedDataSource(NullPointerException())
    }
    return try {
      val producerSequence = producerSequenceFactory.getDecodedImageProducerSequence(imageRequest)
      submitFetchRequest(
          producerSequence,
          imageRequest,
          lowestPermittedRequestLevelOnSubmit,
          callerContext,
          requestListener,
          uiComponentId,
          extras)
    } catch (exception: Exception) {
      DataSources.immediateFailedDataSource(exception)
    }
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending encoded
   * image(s).
   *
   * The ResizeOptions in the imageRequest will be ignored for this fetch
   *
   * The returned DataSource must be closed once the client has finished with it.
   *
   * @param imageRequest the request to submit
   * @return a DataSource representing the pending encoded image(s)
   */
  fun fetchEncodedImage(
      imageRequest: ImageRequest,
      callerContext: Any?
  ): DataSource<CloseableReference<PooledByteBuffer>> =
      fetchEncodedImage(imageRequest, callerContext, null)

  /**
   * Submits a request for execution and returns a DataSource representing the pending encoded
   * image(s).
   *
   * The ResizeOptions in the imageRequest will be ignored for this fetch
   *
   * The returned DataSource must be closed once the client has finished with it.
   *
   * @param imageRequest the request to submit
   * @return a DataSource representing the pending encoded image(s)
   */
  fun fetchEncodedImage(
      imageRequest: ImageRequest,
      callerContext: Any?,
      requestListener: RequestListener?
  ): DataSource<CloseableReference<PooledByteBuffer>> {
    var imageRequest = imageRequest
    checkNotNull(imageRequest.sourceUri)
    return try {
      val producerSequence = producerSequenceFactory.getEncodedImageProducerSequence(imageRequest)
      // The resize options are used to determine whether images are going to be downsampled during
      // decode or not. For the case where the image has to be downsampled and it's a local image it
      // will be kept as a FileInputStream until decoding instead of reading it in memory. Since
      // this method returns an encoded image, it should always be read into memory. Therefore, the
      // resize options are ignored to avoid treating the image as if it was to be downsampled
      // during decode.
      if (imageRequest.resizeOptions != null) {
        imageRequest = ImageRequestBuilder.fromRequest(imageRequest).setResizeOptions(null).build()
      }
      submitFetchRequest(
          producerSequence,
          imageRequest,
          RequestLevel.FULL_FETCH,
          callerContext,
          requestListener,
          null,
          null)
    } catch (exception: Exception) {
      DataSources.immediateFailedDataSource(exception)
    }
  }

  @JvmOverloads
  fun prefetchToBitmapCache(
      imageRequest: ImageRequest?,
      callerContext: Any?,
  ): DataSource<Void?> = prefetchToBitmapCache(imageRequest, callerContext, null)

  /**
   * Submits a request for prefetching to the bitmap cache.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param imageRequest the request to submit
   * @return a DataSource that can safely be ignored.
   */
  @JvmOverloads
  fun prefetchToBitmapCache(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      requestListener: RequestListener?
  ): DataSource<Void?> =
      traceSection("ImagePipeline#prefetchToBitmapCache") {
        if (!isPrefetchEnabledSupplier.get()) {
          return DataSources.immediateFailedDataSource(PREFETCH_EXCEPTION)
        }
        try {
          if (config.experiments.prefetchShortcutEnabled && isInBitmapMemoryCache(imageRequest)) {
            return DataSources.immediateSuccessfulDataSource()
          }
          checkNotNull(imageRequest)
          val shouldDecodePrefetches = imageRequest.shouldDecodePrefetches()
          val skipBitmapCache =
              if (shouldDecodePrefetches != null)
                  !shouldDecodePrefetches // use imagerequest param if specified
              else
                  suppressBitmapPrefetchingSupplier
                      .get() // otherwise fall back to pipeline's default
          val producerSequence =
              if (skipBitmapCache)
                  producerSequenceFactory.getEncodedImagePrefetchProducerSequence(imageRequest)
              else producerSequenceFactory.getDecodedImagePrefetchProducerSequence(imageRequest)
          submitPrefetchRequest(
              producerSequence,
              imageRequest,
              RequestLevel.FULL_FETCH,
              callerContext,
              Priority.MEDIUM,
              requestListener)
        } catch (exception: Exception) {
          DataSources.immediateFailedDataSource(exception)
        }
      }

  fun prefetchToDiskCache(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      requestListener: RequestListener?
  ): DataSource<Void?> =
      prefetchToDiskCache(imageRequest, callerContext, Priority.MEDIUM, requestListener)

  fun prefetchToDiskCache(imageRequest: ImageRequest?, callerContext: Any?): DataSource<Void?> {
    return prefetchToDiskCache(imageRequest, callerContext, Priority.MEDIUM, null)
  }

  fun prefetchToDiskCache(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      priority: Priority
  ): DataSource<Void?> {
    return prefetchToDiskCache(imageRequest, callerContext, priority, null)
  }

  /**
   * Submits a request for prefetching to the disk cache.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param imageRequest the request to submit
   * @param priority custom priority for the fetch
   * @return a DataSource that can safely be ignored.
   */
  /**
   * Submits a request for prefetching to the disk cache with a default priority.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param imageRequest the request to submit
   * @return a DataSource that can safely be ignored.
   */
  @JvmOverloads
  fun prefetchToDiskCache(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      priority: Priority,
      requestListener: RequestListener?
  ): DataSource<Void?> {
    if (!isPrefetchEnabledSupplier.get()) {
      return DataSources.immediateFailedDataSource(PREFETCH_EXCEPTION)
    }
    return if (imageRequest == null) {
      DataSources.immediateFailedDataSource(NullPointerException("imageRequest is null"))
    } else {
      try {
        val producerSequence =
            producerSequenceFactory.getEncodedImagePrefetchProducerSequence(imageRequest)
        submitPrefetchRequest(
            producerSequence,
            imageRequest,
            RequestLevel.FULL_FETCH,
            callerContext,
            priority,
            requestListener)
      } catch (exception: Exception) {
        DataSources.immediateFailedDataSource(exception)
      }
    }
  }

  fun prefetchToEncodedCache(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      requestListener: RequestListener?
  ): DataSource<Void?> =
      prefetchToEncodedCache(imageRequest, callerContext, Priority.MEDIUM, requestListener)

  /**
   * Submits a request for prefetching to the encoded cache.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param imageRequest the request to submit
   * @param priority custom priority for the fetch
   * @return a DataSource that can safely be ignored.
   */
  /**
   * Submits a request for prefetching to the encoded cache with a default priority.
   *
   * Beware that if your network fetcher doesn't support priorities prefetch requests may slow down
   * images which are immediately required on screen.
   *
   * @param imageRequest the request to submit
   * @return a DataSource that can safely be ignored.
   */
  @JvmOverloads
  fun prefetchToEncodedCache(
      imageRequest: ImageRequest?,
      callerContext: Any?,
      priority: Priority = Priority.MEDIUM,
      requestListener: RequestListener? = null
  ): DataSource<Void?> =
      traceSection("ImagePipeline#prefetchToEncodedCache") {
        if (!isPrefetchEnabledSupplier.get()) {
          return DataSources.immediateFailedDataSource(PREFETCH_EXCEPTION)
        }
        if (imageRequest == null) {
          return DataSources.immediateFailedDataSource(NULL_IMAGEREQUEST_EXCEPTION)
        }
        try {
          if (config.experiments.prefetchShortcutEnabled && isInEncodedMemoryCache(imageRequest)) {
            return DataSources.immediateSuccessfulDataSource()
          }
          val producerSequence =
              producerSequenceFactory.getEncodedImagePrefetchProducerSequence(imageRequest)
          submitPrefetchRequest(
              producerSequence,
              imageRequest,
              RequestLevel.FULL_FETCH,
              callerContext,
              priority,
              requestListener)
        } catch (exception: Exception) {
          DataSources.immediateFailedDataSource(exception)
        }
      }

  /**
   * Removes all images with the specified [Uri] from memory cache.
   *
   * @param uri The uri of the image to evict
   */
  fun evictFromMemoryCache(uri: Uri) {
    val predicate = predicateForUri(uri)
    bitmapMemoryCache.removeAll(predicate)
    encodedMemoryCache.removeAll(predicate)
  }

  /**
   * If you have supplied your own cache key factory when configuring the pipeline, this method may
   * not work correctly. It will only work if the custom factory builds the cache key entirely from
   * the URI. If that is not the case, use [evictFromDiskCache(ImageRequest)].
   *
   * @param uri The uri of the image to evict
   */
  fun evictFromDiskCache(uri: Uri?) {
    evictFromDiskCache(checkNotNull(ImageRequest.fromUri(uri)))
  }

  /**
   * Removes all images with the specified [Uri] from disk cache.
   *
   * @param imageRequest The imageRequest for the image to evict from disk cache
   */
  fun evictFromDiskCache(imageRequest: ImageRequest?) {
    if (imageRequest == null) {
      return
    }
    val cacheKey = cacheKeyFactory.getEncodedCacheKey(imageRequest, null)
    val diskCachesStore = diskCachesStoreSupplier.get()
    diskCachesStore.mainBufferedDiskCache.remove(cacheKey)
    diskCachesStore.smallImageBufferedDiskCache.remove(cacheKey)
    diskCachesStore.dynamicBufferedDiskCaches.forEach { it.value.remove(cacheKey) }
  }

  /**
   * If you have supplied your own cache key factory when configuring the pipeline, this method may
   * not work correctly. It will only work if the custom factory builds the cache key entirely from
   * the URI. If that is not the case, use [evictFromMemoryCache(Uri)] and
   * [evictFromDiskCache(ImageRequest)] separately.
   *
   * @param uri The uri of the image to evict
   */
  fun evictFromCache(uri: Uri) {
    evictFromMemoryCache(uri)
    evictFromDiskCache(uri)
  }

  /** Clear the memory caches */
  fun clearMemoryCaches() {
    val allPredicate: Predicate<CacheKey> = Predicate { true }
    bitmapMemoryCache.removeAll(allPredicate)
    encodedMemoryCache.removeAll(allPredicate)
  }

  /** Clear disk caches */
  fun clearDiskCaches() {
    val diskCachesStore = diskCachesStoreSupplier.get()
    diskCachesStore.mainBufferedDiskCache.clearAll()
    diskCachesStore.smallImageBufferedDiskCache.clearAll()
    diskCachesStore.dynamicBufferedDiskCaches.forEach { it.value.clearAll() }
  }

  val usedDiskCacheSize: Long
    /**
     * Current disk caches size
     *
     * @return size in Bytes
     */
    get() {
      val diskCachesStore = diskCachesStoreSupplier.get()
      return diskCachesStore.mainBufferedDiskCache.size +
          diskCachesStore.smallImageBufferedDiskCache.size +
          diskCachesStore.dynamicBufferedDiskCaches.values.sumOf { it.size }
    }

  /** Clear all the caches (memory and disk) */
  fun clearCaches() {
    clearMemoryCaches()
    clearDiskCaches()
  }

  /**
   * Returns whether the image is stored in the bitmap memory cache.
   *
   * @param uri the uri for the image to be looked up.
   * @return true if the image was found in the bitmap memory cache, false otherwise
   */
  fun isInBitmapMemoryCache(uri: Uri?): Boolean {
    if (uri == null) {
      return false
    }
    val bitmapCachePredicate = predicateForUri(uri)
    return bitmapMemoryCache.contains(bitmapCachePredicate)
  }

  /**
   * Returns whether the image is stored in the bitmap memory cache.
   *
   * @param imageRequest the imageRequest for the image to be looked up.
   * @return true if the image was found in the bitmap memory cache, false otherwise.
   */
  fun isInBitmapMemoryCache(imageRequest: ImageRequest?): Boolean {
    if (imageRequest == null) {
      return false
    }
    val cacheKey = cacheKeyFactory.getBitmapCacheKey(imageRequest, null)
    val ref = bitmapMemoryCache[cacheKey]
    return try {
      CloseableReference.isValid(ref)
    } finally {
      CloseableReference.closeSafely(ref)
    }
  }

  /**
   * Returns whether the image is stored in the encoded memory cache.
   *
   * @param uri the uri for the image to be looked up.
   * @return true if the image was found in the encoded memory cache, false otherwise
   */
  fun isInEncodedMemoryCache(uri: Uri?): Boolean {
    if (uri == null) {
      return false
    }
    val encodedCachePredicate = predicateForUri(uri)
    return encodedMemoryCache.contains(encodedCachePredicate)
  }

  /**
   * Returns whether the image is stored in the encoded memory cache.
   *
   * @param imageRequest the imageRequest for the image to be looked up.
   * @return true if the image was found in the encoded memory cache, false otherwise.
   */
  fun isInEncodedMemoryCache(imageRequest: ImageRequest?): Boolean {
    if (imageRequest == null) {
      return false
    }
    val cacheKey = cacheKeyFactory.getEncodedCacheKey(imageRequest, null)
    val ref = encodedMemoryCache[cacheKey]
    return try {
      CloseableReference.isValid(ref)
    } finally {
      CloseableReference.closeSafely(ref)
    }
  }

  /**
   * Returns whether the image is stored in the disk cache. Performs disk cache check synchronously.
   * It is not recommended to use this unless you know what exactly you are doing. Disk cache check
   * is a costly operation, the call will block the caller thread until the cache check is
   * completed.
   *
   * @param uri the uri for the image to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  fun isInDiskCacheSync(uri: Uri?): Boolean =
      isInDiskCacheSync(uri, CacheChoice.SMALL) ||
          isInDiskCacheSync(uri, CacheChoice.DEFAULT) ||
          isInDiskCacheSync(uri, CacheChoice.DYNAMIC)

  /**
   * Returns whether the image is stored in the disk cache. Performs disk cache check synchronously.
   * It is not recommended to use this unless you know what exactly you are doing. Disk cache check
   * is a costly operation, the call will block the caller thread until the cache check is
   * completed.
   *
   * @param uri the uri for the image to be looked up.
   * @param cacheChoice the cacheChoice for the cache to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  fun isInDiskCacheSync(uri: Uri?, cacheChoice: CacheChoice?): Boolean {
    val imageRequest =
        ImageRequestBuilder.newBuilderWithSource(uri).setCacheChoice(cacheChoice).build()
    return isInDiskCacheSync(imageRequest)
  }

  /**
   * Returns whether the image is stored in the Dynamic disk cache. If a diskCacheId is NOT provided
   * we will search ALL Dynamic Disk Caches.
   */
  private fun isInDynamicDiskCachesSync(imageRequest: ImageRequest): Boolean {
    val diskCachesStore = diskCachesStoreSupplier.get()
    val cacheKey = cacheKeyFactory.getEncodedCacheKey(imageRequest, null)
    val diskCacheId = imageRequest.diskCacheId
    if (diskCacheId != null) {
      return diskCachesStore.dynamicBufferedDiskCaches[diskCacheId]?.diskCheckSync(cacheKey)
          ?: false
    }
    diskCachesStore.dynamicBufferedDiskCaches.forEach {
      if (it.value.diskCheckSync(cacheKey)) {
        return true
      }
    }
    return false
  }

  /**
   * Performs disk cache check synchronously. It is not recommended to use this unless you know what
   * exactly you are doing. Disk cache check is a costly operation, the call will block the caller
   * thread until the cache check is completed.
   *
   * @param imageRequest the imageRequest for the image to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  fun isInDiskCacheSync(imageRequest: ImageRequest): Boolean {
    val diskCachesStore = diskCachesStoreSupplier.get()
    val cacheKey = cacheKeyFactory.getEncodedCacheKey(imageRequest, null)
    val cacheChoice = imageRequest.cacheChoice
    val oldPolicy = StrictMode.allowThreadDiskReads()
    try {
      return when (cacheChoice) {
        CacheChoice.DEFAULT -> diskCachesStore.mainBufferedDiskCache.diskCheckSync(cacheKey)
        CacheChoice.SMALL -> diskCachesStore.smallImageBufferedDiskCache.diskCheckSync(cacheKey)
        CacheChoice.DYNAMIC -> isInDynamicDiskCachesSync(imageRequest)
      }
    } finally {
      StrictMode.setThreadPolicy(oldPolicy)
    }
  }

  /**
   * Returns whether the image is stored in the disk cache.
   *
   * If you have supplied your own cache key factory when configuring the pipeline, this method may
   * not work correctly. It will only work if the custom factory builds the cache key entirely from
   * the URI. If that is not the case, use [isInDiskCache(ImageRequest)].
   *
   * @param uri the uri for the image to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  fun isInDiskCache(uri: Uri?): DataSource<Boolean> =
      isInDiskCache(checkNotNull(ImageRequest.fromUri(uri)))

  private fun isInDynamicDiskCaches(
      imageRequest: ImageRequest?,
      cacheKey: CacheKey,
      intermediateContinuation: Continuation<Boolean, Void>,
      cts: CancellationTokenSource
  ): Task<Boolean> {
    val diskCachesStore = diskCachesStoreSupplier.get()
    val diskCacheId = imageRequest?.diskCacheId
    if (diskCacheId != null) {
      return diskCachesStore.dynamicBufferedDiskCaches[diskCacheId]?.contains(cacheKey)
          ?: Task.forResult(false)
    }
    if (diskCachesStore.dynamicBufferedDiskCaches.size == 0) {
      return Task.forResult(false)
    }

    val dynamicDiskCacheIterator = diskCachesStore.dynamicBufferedDiskCaches.iterator()
    var prevTask: Task<Boolean> = Task.forResult(false)
    var curTask = prevTask

    while (dynamicDiskCacheIterator.hasNext()) {
      val curDynamicDiskCache = dynamicDiskCacheIterator.next().value
      curTask = curDynamicDiskCache.contains(cacheKey)
      prevTask.continueWithTask(
          { task ->
            if (!task.isCancelled && !task.isFaulted && task.result) {
              cts.cancel()
              Task.forResult<Boolean>(true).continueWith(intermediateContinuation)
            } else if (task.isCancelled) {
              Task.forResult<Boolean>(false)
            } else {
              curTask
            }
          },
          cts.token)
      prevTask = curTask
    }
    return prevTask
  }

  /**
   * Returns whether the image is stored in the disk cache.
   *
   * @param imageRequest the imageRequest for the image to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  fun isInDiskCache(imageRequest: ImageRequest?): DataSource<Boolean> {
    val diskCachesStore = diskCachesStoreSupplier.get()
    val cacheKey = cacheKeyFactory.getEncodedCacheKey(imageRequest, null)
    val dataSource = SimpleDataSource.create<Boolean>()
    val cts = CancellationTokenSource()

    val finalContinuation =
        Continuation<Boolean, Void> { task ->
          dataSource.result =
              dataSource.result ?: false || !task.isCancelled && !task.isFaulted && task.result
          null
        }
    val intermediateContinuation =
        Continuation<Boolean, Void> { task ->
          dataSource.setResult(
              dataSource.result ?: false || (!task.isCancelled && !task.isFaulted && task.result),
              false /* isLast */)
          null
        }
    diskCachesStore.mainBufferedDiskCache
        .contains(cacheKey)
        .continueWithTask<Boolean> { task ->
          if (!task.isCancelled && !task.isFaulted && task.result) {
            Task.forResult<Boolean>(true)
          } else {
            diskCachesStore.smallImageBufferedDiskCache.contains(cacheKey)
          }
        }
        .continueWithTask<Boolean>(
            { task ->
              if (!task.isCancelled && !task.isFaulted && task.result) {
                Task.forResult<Boolean>(true)
              } else {
                isInDynamicDiskCaches(imageRequest, cacheKey, intermediateContinuation, cts)
              }
            },
            cts.token)
        .continueWith(finalContinuation)
    return dataSource
  }

  /** @return [CacheKey] for doing bitmap cache lookups in the pipeline. */
  fun getCacheKey(imageRequest: ImageRequest?, callerContext: Any?): CacheKey? =
      traceSection("ImagePipeline#getCacheKey") {
        var cacheKey: CacheKey? = null
        if (imageRequest != null) {
          cacheKey =
              if (imageRequest.postprocessor != null) {
                cacheKeyFactory.getPostprocessedBitmapCacheKey(imageRequest, callerContext)
              } else {
                cacheKeyFactory.getBitmapCacheKey(imageRequest, callerContext)
              }
        }
        return cacheKey
      }

  /**
   * Returns a reference to the cached image
   *
   * @param cacheKey
   * @return a closeable reference or null if image was not present in cache
   */
  fun getCachedImage(cacheKey: CacheKey?): CloseableReference<CloseableImage>? {
    if (cacheKey == null) {
      return null
    }
    val closeableImage = bitmapMemoryCache[cacheKey]
    if (closeableImage != null && !closeableImage.get().qualityInfo.isOfFullQuality) {
      closeableImage.close()
      return null
    }
    return closeableImage
  }

  fun hasCachedImage(cacheKey: CacheKey?): Boolean {
    return if (cacheKey == null) {
      false
    } else {
      bitmapMemoryCache.contains(cacheKey)
    }
  }

  private fun <T> submitFetchRequest(
      producerSequence: Producer<CloseableReference<T>>,
      imageRequest: ImageRequest,
      lowestPermittedRequestLevelOnSubmit: RequestLevel,
      callerContext: Any?,
      requestListener: RequestListener?,
      uiComponentId: String?
  ): DataSource<CloseableReference<T>> =
      submitFetchRequest(
          producerSequence,
          imageRequest,
          lowestPermittedRequestLevelOnSubmit,
          callerContext,
          requestListener,
          uiComponentId,
          null)

  private fun <T> submitFetchRequest(
      producerSequence: Producer<CloseableReference<T>>,
      imageRequest: ImageRequest,
      lowestPermittedRequestLevelOnSubmit: RequestLevel,
      callerContext: Any?,
      requestListener: RequestListener?,
      uiComponentId: String?,
      extras: Map<String, *>?
  ): DataSource<CloseableReference<T>> =
      traceSection("ImagePipeline#submitFetchRequest") {
        val requestListener2 =
            InternalRequestListener(
                getRequestListenerForRequest(imageRequest, requestListener), requestListener2)
        callerContextVerifier?.verifyCallerContext(callerContext, false)
        return try {
          val lowestPermittedRequestLevel =
              RequestLevel.getMax(
                  imageRequest.lowestPermittedRequestLevel, lowestPermittedRequestLevelOnSubmit)
          val settableProducerContext =
              SettableProducerContext(
                  imageRequest,
                  generateUniqueFutureId(),
                  uiComponentId,
                  requestListener2,
                  callerContext,
                  lowestPermittedRequestLevel, /* isPrefetch */
                  false,
                  imageRequest.progressiveRenderingEnabled ||
                      !UriUtil.isNetworkUri(imageRequest.sourceUri),
                  imageRequest.priority,
                  config)
          settableProducerContext.putExtras(extras)
          CloseableProducerToDataSourceAdapter.create(
              producerSequence, settableProducerContext, requestListener2)
        } catch (exception: Exception) {
          DataSources.immediateFailedDataSource(exception)
        }
      }

  private fun <T> submitFetchRequest(
      producerSequence: Producer<CloseableReference<T>>,
      imageRequest: ImageRequest,
      lowestPermittedRequestLevelOnSubmit: RequestLevel,
      callerContext: Any?,
      requestListener: RequestListener?,
      extras: Map<String, *>?
  ): DataSource<CloseableReference<T>> =
      traceSection("ImagePipeline#submitFetchRequest") {
        val requestListener2 =
            InternalRequestListener(
                getRequestListenerForRequest(imageRequest, requestListener), requestListener2)
        callerContextVerifier?.verifyCallerContext(callerContext, false)
        return try {
          val lowestPermittedRequestLevel =
              RequestLevel.getMax(
                  imageRequest.lowestPermittedRequestLevel, lowestPermittedRequestLevelOnSubmit)
          val settableProducerContext =
              SettableProducerContext(
                  imageRequest,
                  generateUniqueFutureId(),
                  null,
                  requestListener2,
                  callerContext,
                  lowestPermittedRequestLevel, /* isPrefetch */
                  false,
                  imageRequest.progressiveRenderingEnabled ||
                      !UriUtil.isNetworkUri(imageRequest.sourceUri),
                  imageRequest.priority,
                  config)
          CloseableProducerToDataSourceAdapter.create(
              producerSequence, settableProducerContext, requestListener2)
        } catch (exception: Exception) {
          DataSources.immediateFailedDataSource(exception)
        }
      }

  fun <T> submitFetchRequest(
      producerSequence: Producer<CloseableReference<T>?>,
      settableProducerContext: SettableProducerContext,
      requestListener: RequestListener?
  ): DataSource<CloseableReference<T>> =
      traceSection("ImagePipeline#submitFetchRequest") {
        return try {
          val requestListener2 = InternalRequestListener(requestListener, requestListener2)
          CloseableProducerToDataSourceAdapter.create(
              producerSequence, settableProducerContext, requestListener2)
        } catch (exception: Exception) {
          DataSources.immediateFailedDataSource(exception)
        }
      }

  private fun submitPrefetchRequest(
      producerSequence: Producer<Void?>,
      imageRequest: ImageRequest,
      lowestPermittedRequestLevelOnSubmit: RequestLevel,
      callerContext: Any?,
      priority: Priority,
      requestListener: RequestListener?
  ): DataSource<Void?> {
    val requestListener2 =
        InternalRequestListener(
            getRequestListenerForRequest(imageRequest, requestListener), requestListener2)
    callerContextVerifier?.verifyCallerContext(callerContext, true)
    val originalUri = imageRequest.sourceUri
    val newUri =
        UriModifier.INSTANCE.modifyPrefetchUri(originalUri, callerContext)
            ?: return DataSources.immediateFailedDataSource(MODIFIED_URL_IS_NULL)
    val imageRequest =
        if (originalUri == newUri) {
          imageRequest
        } else {
          ImageRequestBuilder.fromRequest(imageRequest).setSource(newUri).build()
        }
    return try {
      val lowestPermittedRequestLevel =
          RequestLevel.getMax(
              imageRequest.lowestPermittedRequestLevel, lowestPermittedRequestLevelOnSubmit)
      val settableProducerContext =
          SettableProducerContext(
              imageRequest,
              generateUniqueFutureId(),
              requestListener2,
              callerContext,
              lowestPermittedRequestLevel, /* isPrefetch */
              true,
              config.experiments?.allowProgressiveOnPrefetch == true &&
                  imageRequest.progressiveRenderingEnabled,
              priority,
              config)
      create(producerSequence, settableProducerContext, requestListener2)
    } catch (exception: Exception) {
      DataSources.immediateFailedDataSource(exception)
    }
  }

  fun getRequestListenerForRequest(
      imageRequest: ImageRequest?,
      requestListener: RequestListener?
  ): RequestListener {
    checkNotNull(imageRequest)
    return if (requestListener == null) {
      if (imageRequest.requestListener == null) {
        this.requestListener
      } else {
        ForwardingRequestListener(this.requestListener, imageRequest.requestListener)
      }
    } else {
      if (imageRequest.requestListener == null) {
        ForwardingRequestListener(this.requestListener, requestListener)
      } else {
        ForwardingRequestListener(
            this.requestListener, requestListener, imageRequest.requestListener)
      }
    }
  }

  fun getCombinedRequestListener(listener: RequestListener?): RequestListener =
      listener?.let { ForwardingRequestListener(requestListener, it) } ?: requestListener

  private fun predicateForUri(uri: Uri): Predicate<CacheKey> = Predicate { key ->
    key.containsUri(uri)
  }

  fun pause() {
    threadHandoffProducerQueue.startQueueing()
  }

  fun resume() {
    threadHandoffProducerQueue.stopQueuing()
  }

  val isPaused: Boolean
    get() = threadHandoffProducerQueue.isQueueing

  init {
    this.bitmapMemoryCache = bitmapMemoryCache
    this.encodedMemoryCache = encodedMemoryCache
    this.cacheKeyFactory = cacheKeyFactory
    this.threadHandoffProducerQueue = threadHandoffProducerQueue
    this.suppressBitmapPrefetchingSupplier = suppressBitmapPrefetchingSupplier
    isLazyDataSource = lazyDataSource
    this.callerContextVerifier = callerContextVerifier
    this.config = config
  }

  fun init() {
    // Yes, this does nothing. It's a placeholder method to be used in locations where
    // an injection would otherwise appear to be unused.
  }

  companion object {
    private val PREFETCH_EXCEPTION = CancellationException("Prefetching is not enabled")
    private val NULL_IMAGEREQUEST_EXCEPTION = CancellationException("ImageRequest is null")
    private val MODIFIED_URL_IS_NULL = CancellationException("Modified URL is null")
  }
}
