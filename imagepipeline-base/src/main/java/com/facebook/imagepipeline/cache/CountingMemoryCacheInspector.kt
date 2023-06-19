/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import java.util.ArrayList
import java.util.HashMap
import kotlin.jvm.JvmField

/** Inspects values cached in bitmap memory cache. */
class CountingMemoryCacheInspector<K, V>(
    private val countingBitmapCache: CountingMemoryCache<K, V>
) {

  /** Cache entry info for use by dumpers. */
  class DumpInfoEntry<K, V>(key: K, valueRef: CloseableReference<V>?) {
    // The key is immutable, so it's safe to store that directly
    @JvmField val key: K

    // The value
    @JvmField val value: CloseableReference<V>?

    fun release() {
      CloseableReference.closeSafely(value)
    }

    init {
      this.key = checkNotNull(key)
      value = CloseableReference.cloneOrNull(valueRef)
    }
  }

  /** Info about the status of the cache for use by dumpers. */
  class DumpInfo<K, V>(size: Int, lruSize: Int, params: MemoryCacheParams) {
    @JvmField val maxSize: Int = params.maxCacheSize

    @JvmField val maxEntriesCount: Int = params.maxCacheEntries

    @JvmField val maxEntrySize: Int = params.maxCacheEntrySize

    @JvmField val size: Int

    @JvmField val lruSize: Int

    @JvmField val lruEntries: MutableList<DumpInfoEntry<K, V>>

    @JvmField val sharedEntries: MutableList<DumpInfoEntry<K, V>>

    @JvmField val otherEntries: MutableMap<Bitmap, Any>

    fun release() {
      for (entry in lruEntries) {
        entry.release()
      }
      for (entry in sharedEntries) {
        entry.release()
      }
    }

    init {

      this.size = size
      this.lruSize = lruSize
      lruEntries = ArrayList()
      sharedEntries = ArrayList()
      otherEntries = HashMap()
    }
  }

  /**
   * Iterates through all entries cached in counting cache and builds snapshot of its content. This
   * should be used by tools that need to know current content of given cache.
   *
   * Caller should call release method on returned DumpInfo after it is done with examining cache
   * contents
   */
  fun dumpCacheContent(): DumpInfo<K, V> {
    synchronized(countingBitmapCache) {
      val dumpInfo =
          DumpInfo<K, V>(
              countingBitmapCache.sizeInBytes,
              countingBitmapCache.evictionQueueSizeInBytes,
              countingBitmapCache.memoryCacheParams)
      val maybeCachedEntries = countingBitmapCache.cachedEntries ?: return dumpInfo
      val cachedEntries: List<Map.Entry<K, CountingMemoryCache.Entry<K, V>>> =
          maybeCachedEntries.getMatchingEntries(null)
      for ((_, entry) in cachedEntries) {
        val dumpEntry = DumpInfoEntry(entry.key, entry.valueRef)
        if (entry.clientCount > 0) {
          dumpInfo.sharedEntries.add(dumpEntry)
        } else {
          dumpInfo.lruEntries.add(dumpEntry)
        }
      }
      val otherEntries = countingBitmapCache.otherEntries
      if (otherEntries != null) {
        for (entry in otherEntries.entries) {
          if (entry != null && !entry.key.isRecycled) {
            dumpInfo.otherEntries[entry.key] = entry.value
          }
        }
      }
      return dumpInfo
    }
  }
}
