/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.disk;

import java.io.IOException;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.common.disk.DiskTrimmable;

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
   * Deletes old cache files.
   * @param cacheExpirationMs files older than this will be deleted.
   * @return the age in ms of the oldest file remaining in the cache.
   */
  long clearOldEntries(long cacheExpirationMs);
  void clearAll();

  public DiskStorage.DiskDumpInfo getDumpInfo() throws IOException;
}
