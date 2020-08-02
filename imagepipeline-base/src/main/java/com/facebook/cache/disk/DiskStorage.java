/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.WriterCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage for files in the cache. Responsible for maintaining state (count, size, watch file
 * existence, reachability)
 */
public interface DiskStorage {

  class DiskDumpInfoEntry {
    public final String id;
    public final String path;
    public final String type;
    public final float size;
    public final String firstBits;

    protected DiskDumpInfoEntry(String id, String path, String type, float size, String firstBits) {
      this.id = id;
      this.path = path;
      this.type = type;
      this.size = size;
      this.firstBits = firstBits;
    }
  }

  class DiskDumpInfo {
    public List<DiskDumpInfoEntry> entries;
    public Map<String, Integer> typeCounts;

    public DiskDumpInfo() {
      entries = new ArrayList<>();
      typeCounts = new HashMap<>();
    }
  }

  /**
   * is this storage enabled?
   *
   * @return true, if enabled
   */
  boolean isEnabled();

  /**
   * is this storage external?
   *
   * @return true, if external
   */
  boolean isExternal();

  /**
   * Get the resource with the specified name
   *
   * @param resourceId id of the resource
   * @param debugInfo helper object for debugging
   * @return the resource with the specified name. NULL if not found
   * @throws IOException for unexpected behavior.
   */
  BinaryResource getResource(String resourceId, Object debugInfo) throws IOException;

  /**
   * Does a resource with this name exist?
   *
   * @param resourceId id of the resource
   * @param debugInfo helper object for debugging
   * @return true, if the resource is present in the storage, false otherwise
   * @throws IOException
   */
  boolean contains(String resourceId, Object debugInfo) throws IOException;

  /**
   * Does a resource with this name exist? If so, update the last-accessed time for the resource
   *
   * @param resourceId id of the resource
   * @param debugInfo helper object for debugging
   * @return true, if the resource is present in the storage, false otherwise
   * @throws IOException
   */
  boolean touch(String resourceId, Object debugInfo) throws IOException;

  void purgeUnexpectedResources();

  /**
   * Creates a temporary resource for writing content. Split from commit() in order to allow
   * concurrent writing of cache entries. This entry will not be available to cache clients until
   * commit() is called passing in the resource returned from this method.
   *
   * @param resourceId id of the resource
   * @param debugInfo helper object for debugging
   * @return the Inserter object with methods to write data, commit or cancel the insertion
   * @exception IOException on errors during this operation
   */
  Inserter insert(String resourceId, Object debugInfo) throws IOException;

  /**
   * Get all entries currently in the storage
   *
   * @return a collection of entries in storage
   * @throws IOException
   */
  Collection<Entry> getEntries() throws IOException;

  /**
   * Remove the resource represented by the entry
   *
   * @param entry entry of the resource to delete
   * @return size of deleted file if successfully deleted, -1 otherwise
   * @throws IOException
   */
  long remove(Entry entry) throws IOException;

  /**
   * Remove the resource with specified id
   *
   * @param resourceId
   * @return size of deleted file if successfully deleted, -1 otherwise
   * @throws IOException
   */
  long remove(String resourceId) throws IOException;

  /**
   * Clear all contents of the storage
   *
   * @exception IOException
   * @throws IOException
   */
  void clearAll() throws IOException;

  DiskDumpInfo getDumpInfo() throws IOException;

  /**
   * Get the storage's name, which should be unique
   *
   * @return name of the this storage
   */
  String getStorageName();

  interface Entry {
    /** the id representing the resource */
    String getId();
    /** calculated on first time and never changes so it can be used as immutable * */
    long getTimestamp();
    /** calculated on first time and never changes so it can be used as immutable * */
    long getSize();

    BinaryResource getResource();
  }

  /**
   * This is a builder-like interface returned when calling insert. It holds all the operations
   * carried through an {@link #insert} operation: - writing data - committing - clean up
   */
  interface Inserter {

    /**
     * Update the contents of the resource to be inserted. Executes outside the session lock. The
     * writer callback will be provided with an OutputStream to write to. For high efficiency client
     * should make sure that data is written in big chunks (for example by employing
     * BufferedInputStream or writing all data at once).
     *
     * @param callback the write callback
     * @param debugInfo helper object for debugging
     * @throws IOException
     */
    void writeData(WriterCallback callback, Object debugInfo) throws IOException;

    /**
     * Commits the insertion into the cache. Once this is called the entry will be available to
     * clients of the cache.
     *
     * @param debugInfo debug object for debugging
     * @return the final resource created
     * @exception IOException on errors during the commit
     */
    BinaryResource commit(Object debugInfo) throws IOException;

    /**
     * Commits the insertion into the cache. Once this is called the entry will be available to
     * clients of the cache. It also sets the file's timestamp according to the time passed as an
     * argument.
     *
     * @param debugInfo debug object for debugging
     * @param time in milliseconds
     * @return the final resource created
     * @exception IOException on errors during the commit
     */
    BinaryResource commit(Object debugInfo, long time) throws IOException;

    /**
     * Discards the insertion process. If resource was already committed the call is ignored.
     *
     * @return true if cleanUp is successful (or noop), false if something couldn't be dealt with
     */
    boolean cleanUp();
  }
}
