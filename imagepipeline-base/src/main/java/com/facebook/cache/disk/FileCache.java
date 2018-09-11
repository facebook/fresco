/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.common.disk.DiskTrimmable;
import java.io.IOException;

/**
 * Interface that caches based on disk should implement.
 */
public interface FileCache extends DiskTrimmable {

  /**
   * Tells if this cache is enabled. It's important for some caches that can be disabled
   * without further notice (like in removable/unmountable storage). Anyway a disabled
   * cache should just ignore calls, not fail.
   * @return true if this cache is usable, false otherwise.
   */
  boolean isEnabled();

  /**
   * Returns the binary resource cached with key.
   */
  BinaryResource getResource(CacheKey key);

  /**
   * Returns true if the key is in the in-memory key index.
   *
   * Not guaranteed to be correct. The cache may yet have this key even if this returns false.
   * But if it returns true, it definitely has it.
   *
   * Avoids a disk read.
   */
  boolean hasKeySync(CacheKey key);

  boolean hasKey(CacheKey key);
  boolean probe(CacheKey key);

  /**
   * Inserts resource into file with key
   * @param key cache key
   * @param writer Callback that writes to an output stream
   * @return a sequence of bytes
   * @throws IOException
   */
  BinaryResource insert(CacheKey key, WriterCallback writer) throws IOException;

  /**
   * Removes a resource by key from cache.
   * @param key cache key
   */
  void remove(CacheKey key);

  /**
   * @return the in-use size of the cache
   */
  long getSize();

  /**
   * @return the count of pictures in the cache
   */
  long getCount();

  /**
   * Deletes old cache files.
   * @param cacheExpirationMs files older than this will be deleted.
   * @return the age in ms of the oldest file remaining in the cache.
   */
  long clearOldEntries(long cacheExpirationMs);
  void clearAll();

  DiskStorage.DiskDumpInfo getDumpInfo() throws IOException;
}
