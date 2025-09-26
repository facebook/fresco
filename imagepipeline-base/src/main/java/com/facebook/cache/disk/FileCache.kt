/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import com.facebook.binaryresource.BinaryResource
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.WriterCallback
import com.facebook.cache.disk.DiskStorage.DiskDumpInfo
import com.facebook.common.disk.DiskTrimmable
import java.io.IOException

/** Interface that caches based on disk should implement. */
interface FileCache : DiskTrimmable {
  /**
   * Tells if this cache is enabled. It's important for some caches that can be disabled without
   * further notice (like in removable/unmountable storage). Anyway a disabled cache should just
   * ignore calls, not fail.
   *
   * @return true if this cache is usable, false otherwise.
   */
  fun isEnabled(): Boolean

  /** Returns the binary resource cached with key. */
  fun getResource(key: CacheKey): BinaryResource?

  /**
   * Returns true if the key is in the in-memory key index.
   *
   * Not guaranteed to be correct. The cache may yet have this key even if this returns false. But
   * if it returns true, it definitely has it.
   *
   * Avoids a disk read.
   */
  fun hasKeySync(key: CacheKey): Boolean

  fun hasKey(key: CacheKey): Boolean

  fun probe(key: CacheKey): Boolean

  /**
   * Inserts resource into file with key
   *
   * @param key cache key
   * @param writer Callback that writes to an output stream
   * @return a sequence of bytes
   * @throws IOException
   */
  @Throws(IOException::class) fun insert(key: CacheKey, writer: WriterCallback): BinaryResource?

  /**
   * Removes a resource by key from cache.
   *
   * @param key cache key
   */
  fun remove(key: CacheKey)

  /** @return the in-use size of the cache */
  fun getSize(): Long

  /** @return the count of pictures in the cache */
  fun getCount(): Long

  /**
   * Deletes old cache files.
   *
   * @param cacheExpirationMs files older than this will be deleted.
   * @return the age in ms of the oldest file remaining in the cache.
   */
  fun clearOldEntries(cacheExpirationMs: Long): Long

  fun clearAll()

  @Throws(IOException::class) fun getDumpInfo(): DiskDumpInfo

  fun getMetadata(key: CacheKey): String? = null

  fun setMetadata(key: CacheKey, metadata: String): Unit = Unit
}
