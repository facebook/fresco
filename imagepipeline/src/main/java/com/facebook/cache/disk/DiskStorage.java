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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.WriterCallback;

/**
 * Storage for files in the cache.
 * Responsible for maintaining state (count, size, watch file existence, reachability)
 */
public interface DiskStorage {

  public static class DiskDumpInfoEntry {
    public final String path;
    public final String type;
    public final float size;
    public final String firstBits;
    protected DiskDumpInfoEntry(String path, String type, float size, String firstBits) {
      this.path = path;
      this.type = type;
      this.size = size;
      this.firstBits = firstBits;
    }
  }

  public static class DiskDumpInfo {
    public List<DiskDumpInfoEntry> entries;
    public Map<String, Integer> typeCounts;
    public DiskDumpInfo() {
      entries = new ArrayList<>();
      typeCounts = new HashMap<>();
    }
  }

  /**
   * is this storage enabled?
   * @return true, if enabled
   */
  public boolean isEnabled();

  /**
   * Get the resource with the specified name
   * @param resourceId id of the resource
   * @param debugInfo helper object for debugging
   * @return the resource with the specified name. NULL if not found
   * @throws IOException for unexpected behavior.
   */
  BinaryResource getResource(String resourceId, Object debugInfo) throws IOException;

  /**
   * Does a resource with this name exist?
   * @param resourceId id of the resource
   * @param debugInfo helper object for debugging
   * @return true, if the resource is present in the storage, false otherwise
   * @throws IOException
   */
  boolean contains(String resourceId, Object debugInfo) throws IOException;

  /**
   * Does a resource with this name exist? If so, update the last-accessed time for the
   * resource
   * @param resourceId id of the resource
   * @param debugInfo helper object for debugging
   * @return true, if the resource is present in the storage, false otherwise
   * @throws IOException
   */
  boolean touch(String resourceId, Object debugInfo) throws IOException;

  void purgeUnexpectedResources();

  /**
   * Creates a temporary resource for writing content. Split from commit()
   * in order to allow concurrent writing of cache entries.
   * This entry will not be available to cache clients until
   * commit() is called passing in the resource returned
   * from this method.
   * @param resourceId id of the resource
   * @param debugInfo helper object for debugging
   * @return the temporary resource created
   * @exception IOException on errors during this operation
   */
  BinaryResource createTemporary(String resourceId, Object debugInfo) throws IOException;

  /**
   * Update the contents of the resource. Executes outside the session lock.
   * The resource must exist. The writer callback will be provided with an
   * OutputStream to write to. For high efficiency client should make sure that data
   * is written in big chunks (for example by employing BufferedInputStream or writing all data
   * at once).
   * @param resourceId id of the resource
   * @param resource the existing resource (which will be overwritten)
   * @param callback the write callback
   * @param debugInfo helper object for debugging
   * @throws IOException
   */
  public void updateResource(
      String resourceId,
      BinaryResource resource,
      WriterCallback callback,
      Object debugInfo)
      throws IOException;

  /**
   * Commits the resource created by createTemporary() into the cache.
   * Once this is called the entry will be available to clients of the cache.
   * @param resourceId the id of the resource
   * @param temporary the temporary resource
   * @param debugInfo debug object for debugging
   * @return the permanent resource created
   * @exception IOException on errors during the commit
   */
  BinaryResource commit(
      String resourceId,
      BinaryResource temporary,
      Object debugInfo)
      throws IOException;

  /**
   * Get all entries currently in the storage
   * @return a collection of entries in storage
   * @throws IOException
   */
  Collection<Entry> getEntries() throws IOException;

  /**
   * Remove the resource represented by the entry
   * @param entry entry of the resource to delete
   * @return size of deleted file if successfully deleted, -1 otherwise
   * @throws IOException
   */
  long remove(Entry entry) throws IOException;

  /**
   * Remove the resource with specified id
   * @param resourceId
   * @return size of deleted file if successfully deketed, -1 otherwise
   * @throws IOException
   */
  long remove(String resourceId) throws IOException;
  /**
   * Clear all contents of the storage
   * @exception IOException
   * @throws IOException
   */
  void clearAll() throws IOException;

  public DiskDumpInfo getDumpInfo() throws IOException;

  public interface Entry {
    /** calculated on first time and never changes so it can be used as immutable **/
    public long getTimestamp();
    /** calculated on first time and never changes so it can be used as immutable **/
    public long getSize();
    public BinaryResource getResource();
  }
}
