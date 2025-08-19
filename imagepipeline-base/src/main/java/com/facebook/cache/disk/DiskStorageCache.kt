/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import androidx.annotation.VisibleForTesting
import com.facebook.binaryresource.BinaryResource
import com.facebook.cache.common.CacheErrorLogger
import com.facebook.cache.common.CacheEventListener
import com.facebook.cache.common.CacheEventListener.EvictionReason
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.CacheKeyUtil
import com.facebook.cache.common.WriterCallback
import com.facebook.cache.disk.DiskStorage.DiskDumpInfo
import com.facebook.common.disk.DiskTrimmable
import com.facebook.common.disk.DiskTrimmableRegistry
import com.facebook.common.internal.Preconditions
import com.facebook.common.logging.FLog
import com.facebook.common.statfs.StatFsHelper
import com.facebook.common.time.Clock
import com.facebook.common.time.SystemClock
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe
import kotlin.math.abs
import kotlin.math.max

/** Cache that manages disk storage. */
@ThreadSafe
class DiskStorageCache(
    private val storage: DiskStorage,
    private val entryEvictionComparatorSupplier: EntryEvictionComparatorSupplier,
    params: Params,
    private val cacheEventListener: CacheEventListener?,
    private val cacheErrorLogger: CacheErrorLogger,
    diskTrimmableRegistry: DiskTrimmableRegistry?,
    executorForBackgrountInit: Executor,
    private val indexPopulateAtStartupEnabled: Boolean,
) : FileCache, DiskTrimmable {
  private val lowDiskSpaceCacheSizeLimit: Long
  private val defaultCacheSizeLimit: Long
  private var countDownLatch: CountDownLatch? = null
  private var cacheSizeLimit: Long

  @GuardedBy("lock") // All resourceId stored on disk (if any).
  @VisibleForTesting
  val resourceIndex: MutableSet<String?>

  private var cacheSizeLastUpdateTime: Long

  private val cacheSizeLimitMinimum: Long

  private val statFsHelper: StatFsHelper

  private val cacheStats: CacheStats

  private val clock: Clock

  // synchronization object.
  private val lock = Any()

  private var indexReady = false

  /**
   * Stats about the cache - currently size of the cache (in bytes) and number of items in the cache
   */
  @VisibleForTesting
  internal class CacheStats {
    @get:Synchronized
    var isInitialized: Boolean = false
      private set

    @get:Synchronized
    var size: Long = UNINITIALIZED // size of the cache (in bytes)
      private set

    @get:Synchronized
    var count: Long = UNINITIALIZED // number of items in the cache
      private set

    @Synchronized
    fun reset() {
      this.isInitialized = false
      this.count = UNINITIALIZED
      this.size = UNINITIALIZED
    }

    @Synchronized
    fun set(size: Long, count: Long) {
      this.count = count
      this.size = size
      this.isInitialized = true
    }

    @Synchronized
    fun increment(sizeIncrement: Long, countIncrement: Long) {
      if (this.isInitialized) {
        this.size += sizeIncrement
        this.count += countIncrement
      }
    }
  }

  class Params(
      val cacheSizeLimitMinimum: Long,
      val lowDiskSpaceCacheSizeLimit: Long,
      val defaultCacheSizeLimit: Long,
  )

  init {
    this.lowDiskSpaceCacheSizeLimit = params.lowDiskSpaceCacheSizeLimit
    this.defaultCacheSizeLimit = params.defaultCacheSizeLimit
    this.cacheSizeLimit = params.defaultCacheSizeLimit
    this.statFsHelper = StatFsHelper.getInstance()

    this.cacheSizeLastUpdateTime = UNINITIALIZED

    this.cacheSizeLimitMinimum = params.cacheSizeLimitMinimum

    this.cacheStats = CacheStats()

    this.clock = SystemClock.get()

    this.resourceIndex = HashSet<String?>()

    if (diskTrimmableRegistry != null) {
      diskTrimmableRegistry.registerDiskTrimmable(this)
    }

    if (indexPopulateAtStartupEnabled) {
      countDownLatch = CountDownLatch(1)

      executorForBackgrountInit.execute(
          object : Runnable {
            override fun run() {
              synchronized(lock) { maybeUpdateFileCacheSize() }
              indexReady = true
              countDownLatch!!.countDown()
            }
          })
    } else {
      countDownLatch = CountDownLatch(0)
    }
  }

  @Throws(IOException::class)
  override fun getDumpInfo(): DiskDumpInfo {
    return storage.getDumpInfo()
  }

  override fun isEnabled(): Boolean {
    return storage.isEnabled()
  }

  /**
   * Blocks current thread until having finished initialization in Memory Index. Call only when you
   * need memory index in cold start.
   */
  @VisibleForTesting
  fun awaitIndex() {
    try {
      countDownLatch!!.await()
    } catch (e: InterruptedException) {
      FLog.e(TAG, "Memory Index is not ready yet. ")
    }
  }

  val isIndexReady: Boolean
    /**
     * Tells if memory index is completed in initialization. Only call it when you need to know if
     * memory index is completed in cold start.
     */
    get() = indexReady || !indexPopulateAtStartupEnabled

  /**
   * Retrieves the file corresponding to the mKey, if it is in the cache. Also touches the item,
   * thus changing its LRU timestamp. If the file is not present in the file cache, returns null.
   *
   * This should NOT be called on the UI thread.
   *
   * @param key the mKey to check
   * @return The resource if present in cache, otherwise null
   */
  override fun getResource(key: CacheKey): BinaryResource? {
    var resourceId: String? = null
    val cacheEvent = SettableCacheEvent.obtain().setCacheKey(key)
    try {
      synchronized(lock) {
        var resource: BinaryResource? = null
        val resourceIds: List<String?> = CacheKeyUtil.getResourceIds(key)
        for (i in resourceIds.indices) {
          resourceId = resourceIds.get(i)
          cacheEvent.setResourceId(resourceId!!)
          resource = storage.getResource(resourceId, key)
          if (resource != null) {
            break
          }
        }
        if (resource == null) {
          if (cacheEventListener != null) {
            cacheEventListener.onMiss(cacheEvent)
          }
          resourceIndex.remove(resourceId)
        } else {
          Preconditions.checkNotNull<String?>(resourceId)
          if (cacheEventListener != null) {
            cacheEventListener.onHit(cacheEvent)
          }
          resourceIndex.add(resourceId)
        }
        return resource
      }
    } catch (ioe: IOException) {
      cacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.GENERIC_IO,
          TAG,
          "getResource",
          ioe,
      )
      cacheEvent.setException(ioe)
      if (cacheEventListener != null) {
        cacheEventListener.onReadException(cacheEvent)
      }
      return null
    } finally {
      cacheEvent.recycle()
    }
  }

  /**
   * Probes whether the object corresponding to the mKey is in the cache. Note that the act of
   * probing touches the item (if present in cache), thus changing its LRU timestamp.
   *
   * This will be faster than retrieving the object, but it still has file system accesses and
   * should NOT be called on the UI thread.
   *
   * @param key the mKey to check
   * @return whether the keyed mValue is in the cache
   */
  override fun probe(key: CacheKey): Boolean {
    var resourceId: String? = null
    try {
      synchronized(lock) {
        val resourceIds: List<String?> = CacheKeyUtil.getResourceIds(key)
        for (i in resourceIds.indices) {
          resourceId = resourceIds.get(i)
          if (storage.touch(resourceId!!, key)) {
            resourceIndex.add(resourceId)
            return true
          }
        }
        return false
      }
    } catch (e: IOException) {
      val cacheEvent =
          SettableCacheEvent.obtain().setCacheKey(key).setResourceId(resourceId!!).setException(e)
      if (cacheEventListener != null) {
        cacheEventListener.onReadException(cacheEvent)
      }
      cacheEvent.recycle()
      return false
    }
  }

  /** Creates a temp file for writing outside the session lock */
  @Throws(IOException::class)
  private fun startInsert(resourceId: String?, key: CacheKey?): DiskStorage.Inserter {
    maybeEvictFilesInCacheDir()
    return storage.insert(resourceId!!, key!!)
  }

  /**
   * Commits the provided temp file to the cache, renaming it to match the cache's hashing
   * convention.
   */
  @Throws(IOException::class)
  private fun endInsert(
      inserter: DiskStorage.Inserter,
      key: CacheKey?,
      resourceId: String?,
  ): BinaryResource {
    synchronized(lock) {
      val resource = inserter.commit(key!!)
      resourceIndex.add(resourceId)
      cacheStats.increment(resource.size(), 1)
      return resource
    }
  }

  @Throws(IOException::class)
  override fun insert(key: CacheKey, callback: WriterCallback): BinaryResource {
    // Write to a temp file, then move it into place. This allows more parallelism
    // when writing files.
    val cacheEvent = SettableCacheEvent.obtain().setCacheKey(key)
    if (cacheEventListener != null) {
      cacheEventListener.onWriteAttempt(cacheEvent)
    }
    val resourceId: String?
    synchronized(lock) {
      // for multiple resource ids associated with the same image, we only write one file
      resourceId = CacheKeyUtil.getFirstResourceId(key)
    }
    cacheEvent.setResourceId(resourceId!!)
    try {
      // getting the file is synchronized
      val inserter = startInsert(resourceId, key)
      try {
        inserter.writeData(callback, key)
        // Committing the file is synchronized
        val resource = endInsert(inserter, key, resourceId)
        cacheEvent.setItemSize(resource.size()).setCacheSize(cacheStats.size)
        if (cacheEventListener != null) {
          cacheEventListener.onWriteSuccess(cacheEvent)
        }
        return resource
      } finally {
        if (!inserter.cleanUp()) {
          FLog.e(TAG, "Failed to delete temp file")
        }
      }
    } catch (ioe: IOException) {
      cacheEvent.setException(ioe)
      if (cacheEventListener != null) {
        cacheEventListener.onWriteException(cacheEvent)
      }
      FLog.e(TAG, "Failed inserting a file into the cache", ioe)
      throw ioe
    } finally {
      cacheEvent.recycle()
    }
  }

  override fun remove(key: CacheKey) {
    synchronized(lock) {
      try {
        var resourceId: String? = null
        val resourceIds: List<String> = CacheKeyUtil.getResourceIds(key)
        for (i in resourceIds.indices) {
          resourceId = resourceIds.get(i)
          storage.remove(resourceId)
          resourceIndex.remove(resourceId)
        }
      } catch (e: IOException) {
        cacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.DELETE_FILE,
            TAG,
            "delete: " + e.message,
            e,
        )
      }
    }
  }

  /**
   * Deletes old cache files.
   *
   * @param cacheExpirationMs files older than this will be deleted.
   * @return the age in ms of the oldest file remaining in the cache.
   */
  override fun clearOldEntries(cacheExpirationMs: Long): Long {
    var oldestRemainingEntryAgeMs = 0L
    synchronized(lock) {
      try {
        val now = clock.now()
        val allEntries = storage.getEntries()
        val cacheSizeBeforeClearance = cacheStats.size
        var itemsRemovedCount = 0
        var itemsRemovedSize = 0L
        for (entry in allEntries) {
          // entry age of zero is disallowed.
          val entryAgeMs = max(1.0, abs((now - entry.getTimestamp()).toDouble())).toLong()
          if (entryAgeMs >= cacheExpirationMs) {
            val entryRemovedSize = storage.remove(entry)
            resourceIndex.remove(entry.getId())
            if (entryRemovedSize > 0) {
              itemsRemovedCount++
              itemsRemovedSize += entryRemovedSize
              val cacheEvent =
                  SettableCacheEvent.obtain()
                      .setResourceId(entry.getId())
                      .setEvictionReason(EvictionReason.CONTENT_STALE)
                      .setItemSize(entryRemovedSize)
                      .setCacheSize(cacheSizeBeforeClearance - itemsRemovedSize)
              if (cacheEventListener != null) {
                cacheEventListener.onEviction(cacheEvent)
              }
              cacheEvent.recycle()
            }
          } else {
            oldestRemainingEntryAgeMs =
                max(oldestRemainingEntryAgeMs.toDouble(), entryAgeMs.toDouble()).toLong()
          }
        }
        storage.purgeUnexpectedResources()
        if (itemsRemovedCount > 0) {
          maybeUpdateFileCacheSize()
          cacheStats.increment(-itemsRemovedSize, -itemsRemovedCount.toLong())
        }
      } catch (ioe: IOException) {
        cacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.EVICTION,
            TAG,
            "clearOldEntries: " + ioe.message,
            ioe,
        )
      }
    }
    return oldestRemainingEntryAgeMs
  }

  /**
   * Test if the cache size has exceeded its limits, and if so, evict some files. It also calls
   * maybeUpdateFileCacheSize
   *
   * This method uses lock for synchronization purposes.
   */
  @Throws(IOException::class)
  private fun maybeEvictFilesInCacheDir() {
    synchronized(lock) {
      val calculatedRightNow = maybeUpdateFileCacheSize()
      // Update the size limit (cacheSizeLimit)
      updateFileCacheSizeLimit()

      val cacheSize = cacheStats.size
      // If we are going to evict force a recalculation of the size
      // (except if it was already calculated!)
      if (cacheSize > cacheSizeLimit && !calculatedRightNow) {
        cacheStats.reset()
        maybeUpdateFileCacheSize()
      }

      // If size has exceeded the size limit, evict some files
      if (cacheSize > cacheSizeLimit) {
        evictAboveSize(cacheSizeLimit * 9 / 10, EvictionReason.CACHE_FULL) // 90%
      }
    }
  }

  @GuardedBy("lock")
  @Throws(IOException::class)
  private fun evictAboveSize(desiredSize: Long, reason: EvictionReason?) {
    val entries: MutableCollection<DiskStorage.Entry>?
    try {
      entries = getSortedEntries(storage.getEntries())
    } catch (ioe: IOException) {
      cacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.EVICTION,
          TAG,
          "evictAboveSize: " + ioe.message,
          ioe,
      )
      throw ioe
    }

    val cacheSizeBeforeClearance = cacheStats.size
    val deleteSize = cacheSizeBeforeClearance - desiredSize
    var itemCount = 0
    var sumItemSizes = 0L
    for (entry in entries) {
      if (sumItemSizes > (deleteSize)) {
        break
      }
      val deletedSize = storage.remove(entry)
      resourceIndex.remove(entry.getId())
      if (deletedSize > 0) {
        itemCount++
        sumItemSizes += deletedSize
        val cacheEvent =
            SettableCacheEvent.obtain()
                .setResourceId(entry.getId())
                .setEvictionReason(reason!!)
                .setItemSize(deletedSize)
                .setCacheSize(cacheSizeBeforeClearance - sumItemSizes)
                .setCacheLimit(desiredSize)
        if (cacheEventListener != null) {
          cacheEventListener.onEviction(cacheEvent)
        }
        cacheEvent.recycle()
      }
    }
    cacheStats.increment(-sumItemSizes, -itemCount.toLong())
    storage.purgeUnexpectedResources()
  }

  /**
   * If any file timestamp is in the future (beyond now + FUTURE_TIMESTAMP_THRESHOLD_MS), we will
   * set its effective timestamp to 0 (the beginning of unix time), thus sending it to the head of
   * the queue for eviction (entries with the lowest timestamps are evicted first). This is a safety
   * check in case we get files that are written with a future timestamp. We are adding a small
   * delta (this constant) to account for network time changes, timezone changes, etc.
   */
  private fun getSortedEntries(
      allEntries: MutableCollection<DiskStorage.Entry>
  ): MutableCollection<DiskStorage.Entry> {
    val threshold: Long = clock.now() + FUTURE_TIMESTAMP_THRESHOLD_MS
    val sortedList = ArrayList<DiskStorage.Entry>(allEntries.size)
    val listToSort = ArrayList<DiskStorage.Entry>(allEntries.size)
    for (entry in allEntries) {
      if (entry.getTimestamp() > threshold) {
        sortedList.add(entry)
      } else {
        listToSort.add(entry)
      }
    }
    listToSort.sortWith(entryEvictionComparatorSupplier.get())
    sortedList.addAll(listToSort)
    return sortedList
  }

  /**
   * Helper method that sets the cache size limit to be either a high, or a low limit. If there is
   * not enough free space to satisfy the high limit, it is set to the low limit.
   */
  @GuardedBy("lock")
  private fun updateFileCacheSizeLimit() {
    // Test if cacheSizeLimit can be set to the high limit
    val isAvailableSpaceLowerThanHighLimit: Boolean
    val storageType =
        if (storage.isExternal()) StatFsHelper.StorageType.EXTERNAL
        else StatFsHelper.StorageType.INTERNAL
    isAvailableSpaceLowerThanHighLimit =
        statFsHelper.testLowDiskSpace(storageType, defaultCacheSizeLimit - cacheStats.size)
    if (isAvailableSpaceLowerThanHighLimit) {
      cacheSizeLimit = lowDiskSpaceCacheSizeLimit
    } else {
      cacheSizeLimit = defaultCacheSizeLimit
    }
  }

  override fun getSize(): Long {
    return cacheStats.size
  }

  override fun getCount(): Long {
    return cacheStats.count
  }

  override fun clearAll() {
    synchronized(lock) {
      try {
        storage.clearAll()
        resourceIndex.clear()
        if (cacheEventListener != null) {
          cacheEventListener.onCleared()
        }
      } catch (e: IOException) {
        cacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.EVICTION,
            TAG,
            "clearAll: " + e.message,
            e,
        )
      } catch (e: NullPointerException) {
        cacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.EVICTION,
            TAG,
            "clearAll: " + e.message,
            e,
        )
      }
      cacheStats.reset()
    }
  }

  override fun hasKeySync(key: CacheKey): Boolean {
    synchronized(lock) {
      var resourceId: String? = null
      val resourceIds: List<String?> = CacheKeyUtil.getResourceIds(key)
      for (i in resourceIds.indices) {
        resourceId = resourceIds.get(i)
        if (resourceIndex.contains(resourceId)) {
          return true
        }
      }
      return false
    }
  }

  override fun hasKey(key: CacheKey): Boolean {
    synchronized(lock) {
      if (hasKeySync(key)) {
        return true
      }
      try {
        var resourceId: String? = null
        val resourceIds: List<String> = CacheKeyUtil.getResourceIds(key)
        for (i in resourceIds.indices) {
          resourceId = resourceIds.get(i)
          if (storage.contains(resourceId, key)) {
            resourceIndex.add(resourceId)
            return true
          }
        }
        return false
      } catch (e: IOException) {
        return false
      }
    }
  }

  override fun trimToMinimum() {
    synchronized(lock) {
      maybeUpdateFileCacheSize()
      val cacheSize = cacheStats.size
      if (cacheSizeLimitMinimum <= 0 || cacheSize <= 0 || cacheSize < cacheSizeLimitMinimum) {
        return
      }
      val trimRatio = 1 - cacheSizeLimitMinimum.toDouble() / cacheSize.toDouble()
      if (trimRatio > TRIMMING_LOWER_BOUND) {
        trimBy(trimRatio)
      }
    }
  }

  override fun trimToNothing() {
    clearAll()
  }

  private fun trimBy(trimRatio: Double) {
    synchronized(lock) {
      try {
        // Force update the ground truth if we are about to evict
        cacheStats.reset()
        maybeUpdateFileCacheSize()
        val cacheSize = cacheStats.size
        val newMaxBytesInFiles = cacheSize - (trimRatio * cacheSize).toLong()
        evictAboveSize(newMaxBytesInFiles, EvictionReason.CACHE_MANAGER_TRIMMED)
      } catch (ioe: IOException) {
        cacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.EVICTION,
            TAG,
            "trimBy: " + ioe.message,
            ioe,
        )
      }
    }
  }

  /**
   * If file cache size is not calculated or if it was calculated a long time ago
   * (FILECACHE_SIZE_UPDATE_PERIOD_MS) recalculated from file listing.
   *
   * @return true if it was recalculated, false otherwise.
   */
  @GuardedBy("lock")
  private fun maybeUpdateFileCacheSize(): Boolean {
    val now = clock.now()
    if ((!cacheStats.isInitialized) ||
        cacheSizeLastUpdateTime == UNINITIALIZED ||
        (now - cacheSizeLastUpdateTime) > FILECACHE_SIZE_UPDATE_PERIOD_MS) {
      return maybeUpdateFileCacheSizeAndIndex()
    }
    return false
  }

  @GuardedBy("lock")
  private fun maybeUpdateFileCacheSizeAndIndex(): Boolean {
    var size: Long = 0
    var count = 0
    var foundFutureTimestamp = false
    var numFutureFiles = 0
    var sizeFutureFiles = 0
    var maxTimeDelta: Long = -1
    val now = clock.now()
    val timeThreshold: Long = now + FUTURE_TIMESTAMP_THRESHOLD_MS
    val tempResourceIndex: MutableSet<String?>?
    if (indexPopulateAtStartupEnabled && resourceIndex.isEmpty()) {
      tempResourceIndex = resourceIndex
    } else if (indexPopulateAtStartupEnabled) {
      tempResourceIndex = HashSet<String?>()
    } else {
      tempResourceIndex = null
    }
    try {
      val entries = storage.getEntries()
      for (entry in entries) {
        count++
        size += entry.getSize()

        // Check if any files have a future timestamp, beyond our threshold
        if (entry.getTimestamp() > timeThreshold) {
          foundFutureTimestamp = true
          numFutureFiles++
          sizeFutureFiles += entry.getSize().toInt()
          maxTimeDelta =
              max((entry.getTimestamp() - now).toDouble(), maxTimeDelta.toDouble()).toLong()
        } else if (indexPopulateAtStartupEnabled) {
          Preconditions.checkNotNull<MutableSet<String?>?>(tempResourceIndex)
          tempResourceIndex!!.add(entry.getId())
        }
      }
      if (foundFutureTimestamp) {
        cacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.READ_INVALID_ENTRY,
            TAG,
            ("Future timestamp found in " +
                numFutureFiles +
                " files , with a total size of " +
                sizeFutureFiles +
                " bytes, and a maximum time delta of " +
                maxTimeDelta +
                "ms"),
            null,
        )
      }
      if (cacheStats.count != count.toLong() || cacheStats.size != size) {
        if (indexPopulateAtStartupEnabled && resourceIndex !== tempResourceIndex) {
          Preconditions.checkNotNull<MutableSet<String?>?>(tempResourceIndex)
          resourceIndex.clear()
          resourceIndex.addAll(tempResourceIndex!!)
        }
        cacheStats.set(size, count.toLong())
      }
    } catch (ioe: IOException) {
      cacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.GENERIC_IO,
          TAG,
          "calcFileCacheSize: " + ioe.message,
          ioe,
      )
      return false
    }
    cacheSizeLastUpdateTime = now
    return true
  }

  companion object {
    private val TAG: Class<*> = DiskStorageCache::class.java

    // Any subclass that uses MediaCache/DiskCache's versioning system should use this
    // constant as the very first entry in their list of versions.  When all
    // subclasses of MediaCache have moved on to subsequent versions and are
    // no longer using this constant, it can be removed.
    const val START_OF_VERSIONING: Int = 1
    private val FUTURE_TIMESTAMP_THRESHOLD_MS = TimeUnit.HOURS.toMillis(2)
    // Force recalculation of the ground truth for filecache size at this interval
    private val FILECACHE_SIZE_UPDATE_PERIOD_MS = TimeUnit.MINUTES.toMillis(30)
    private const val TRIMMING_LOWER_BOUND = 0.02
    private val UNINITIALIZED: Long = -1
  }
}
