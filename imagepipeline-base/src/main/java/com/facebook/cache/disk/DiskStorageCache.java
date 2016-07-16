/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.disk;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.cache.common.CacheEventListener;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.common.disk.DiskTrimmable;
import com.facebook.common.disk.DiskTrimmableRegistry;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.statfs.StatFsHelper;
import com.facebook.common.time.Clock;
import com.facebook.common.time.SystemClock;
import com.facebook.common.util.SecureHashUtil;

/**
 * Cache that manages disk storage.
 */
@ThreadSafe
public class DiskStorageCache implements FileCache, DiskTrimmable {

  private static final Class<?> TAG = DiskStorageCache.class;

  // Any subclass that uses MediaCache/DiskCache's versioning system should use this
  // constant as the very first entry in their list of versions.  When all
  // subclasses of MediaCache have moved on to subsequent versions and are
  // no longer using this constant, it can be removed.
  public static final int START_OF_VERSIONING = 1;
  private static final long FUTURE_TIMESTAMP_THRESHOLD_MS = TimeUnit.HOURS.toMillis(2);
  // Force recalculation of the ground truth for filecache size at this interval
  private static final long FILECACHE_SIZE_UPDATE_PERIOD_MS = TimeUnit.MINUTES.toMillis(30);
  private static final double TRIMMING_LOWER_BOUND = 0.02;
  private static final long UNINITIALIZED = -1;
  private static final String SHARED_PREFS_FILENAME_PREFIX = "disk_entries_list";

  private final long mLowDiskSpaceCacheSizeLimit;
  private final long mDefaultCacheSizeLimit;
  private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
  private long mCacheSizeLimit;

  private final CacheEventListener mCacheEventListener;
  private final SharedPreferences mSharedPreferences;

  @GuardedBy("mLock")
  // Maps CacheKey to the resource id that is stored on disk (if any).
  @VisibleForTesting Map<Integer, String> mIndex = new HashMap<>();

  @GuardedBy("mLock")
  // All resourceId stored on disk (if any).
  @VisibleForTesting final Set<String> mResourceIndex;

  @GuardedBy("mLock")
  private long mCacheSizeLastUpdateTime;

  private final long mCacheSizeLimitMinimum;

  private final StatFsHelper mStatFsHelper;

  private final DiskStorage mStorage;
  private final EntryEvictionComparatorSupplier mEntryEvictionComparatorSupplier;
  private final CacheErrorLogger mCacheErrorLogger;

  private final CacheStats mCacheStats;

  private final Clock mClock;

  // synchronization object.
  private final Object mLock = new Object();

  /**
   * Stats about the cache - currently size of the cache (in bytes) and number of items in
   * the cache
   */
  @VisibleForTesting
  static class CacheStats {

    private boolean mInitialized = false;
    private long mSize = UNINITIALIZED;    // size of the cache (in bytes)
    private long mCount = UNINITIALIZED;   // number of items in the cache

    public synchronized boolean isInitialized() {
      return mInitialized;
    }

    public synchronized void reset() {
      mInitialized = false;
      mCount = UNINITIALIZED;
      mSize = UNINITIALIZED;
    }

    public synchronized void set(long size, long count) {
      mCount = count;
      mSize = size;
      mInitialized = true;
    }

    public synchronized void increment(long sizeIncrement, long countIncrement) {
      if (mInitialized) {
        mSize += sizeIncrement;
        mCount += countIncrement;
      }
    }

    public synchronized long getSize() {
      return mSize;
    }

    public synchronized long getCount() {
      return mCount;
    }
  }

  public static class Params {
    public final long mCacheSizeLimitMinimum;
    public final long mLowDiskSpaceCacheSizeLimit;
    public final long mDefaultCacheSizeLimit;

    public Params(
        long cacheSizeLimitMinimum,
        long lowDiskSpaceCacheSizeLimit,
        long defaultCacheSizeLimit) {
      mCacheSizeLimitMinimum = cacheSizeLimitMinimum;
      mLowDiskSpaceCacheSizeLimit = lowDiskSpaceCacheSizeLimit;
      mDefaultCacheSizeLimit = defaultCacheSizeLimit;
    }
  }

  public DiskStorageCache(
      DiskStorage diskStorage,
      EntryEvictionComparatorSupplier entryEvictionComparatorSupplier,
      Params params,
      CacheEventListener cacheEventListener,
      CacheErrorLogger cacheErrorLogger,
      @Nullable DiskTrimmableRegistry diskTrimmableRegistry,
      Context context) {
    this.mLowDiskSpaceCacheSizeLimit = params.mLowDiskSpaceCacheSizeLimit;
    this.mDefaultCacheSizeLimit = params.mDefaultCacheSizeLimit;
    this.mCacheSizeLimit = params.mDefaultCacheSizeLimit;
    this.mStatFsHelper = StatFsHelper.getInstance();

    this.mStorage = diskStorage;
    this.mEntryEvictionComparatorSupplier = entryEvictionComparatorSupplier;

    this.mCacheSizeLastUpdateTime = UNINITIALIZED;

    this.mCacheEventListener = cacheEventListener;

    this.mCacheSizeLimitMinimum = params.mCacheSizeLimitMinimum;

    this.mCacheErrorLogger = cacheErrorLogger;

    this.mCacheStats = new CacheStats();

    if (diskTrimmableRegistry != null) {
      diskTrimmableRegistry.registerDiskTrimmable(this);
    }
    this.mClock = SystemClock.get();

    this.mSharedPreferences = initializeSharedPreferences(context, mStorage.getStorageName());

    this.mResourceIndex = new HashSet<>();

    Executors.newSingleThreadExecutor().execute(new Runnable() {

      @Override
      public void run() {
        synchronized (mLock) {
          maybeUpdateFileCacheSize();
        }
        mCountDownLatch.countDown();
      }
    });
  }

  @Override
  public DiskStorage.DiskDumpInfo getDumpInfo() throws IOException {
    return mStorage.getDumpInfo();
  }

  @Override
  public boolean isEnabled() {
    return mStorage.isEnabled();
  }

  /**
   * Blocks current thread until having finished initialization in Memory Index. Call only when you
   * need memory index in cold start.
   */
  @VisibleForTesting
  protected void awaitIndex() {
    try {
      mCountDownLatch.await();
    } catch (InterruptedException e) {
      FLog.e(TAG, "Memory Index is not ready yet. ");
    }
  }

  /**
   * Retrieves the file corresponding to the mKey, if it is in the cache. Also
   * touches the item, thus changing its LRU timestamp. If the file is not
   * present in the file cache, returns null.
   * <p>
   * This should NOT be called on the UI thread.
   *
   * @param key the mKey to check
   * @return The resource if present in cache, otherwise null
   */
  @Override
  public BinaryResource getResource(final CacheKey key) {
    String resourceId = null;
    SettableCacheEvent cacheEvent = new SettableCacheEvent()
        .setCacheKey(key);
    Integer hashKey = Integer.valueOf(key.hashCode());

    try {
      synchronized (mLock) {
        BinaryResource resource = null;
        if (mIndex.containsKey(hashKey)) {
          resourceId = mIndex.get(hashKey);
          cacheEvent.setResourceId(resourceId);
          resource = mStorage.getResource(resourceId, key);
        } else {
          List<String> resourceIds = getResourceIds(key);
          for (int i = 0; i < resourceIds.size(); i++) {
            resourceId = resourceIds.get(i);
            if (!mResourceIndex.contains(resourceId)) {
              continue;
            }
            cacheEvent.setResourceId(resourceId);
            resource = mStorage.getResource(resourceId, key);
            if (resource != null) {
              break;
            }
          }
        }
        if (resource == null) {
          mCacheEventListener.onMiss(cacheEvent);
          removeKeyFromIndex(hashKey);
        } else {
          mCacheEventListener.onHit(cacheEvent);
          addKeyToIndex(hashKey, resourceId);
        }
        return resource;
      }
    } catch (IOException ioe) {
      mCacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.GENERIC_IO,
          TAG,
          "getResource",
          ioe);
      cacheEvent.setException(ioe);
      mCacheEventListener.onReadException(cacheEvent);
      return null;
    }
  }

  /**
   * Probes whether the object corresponding to the mKey is in the cache.
   * Note that the act of probing touches the item (if present in cache),
   * thus changing its LRU timestamp.
   * <p>
   * This will be faster than retrieving the object, but it still has
   * file system accesses and should NOT be called on the UI thread.
   *
   * @param key the mKey to check
   * @return whether the keyed mValue is in the cache
   */
  public boolean probe(final CacheKey key) {
    String resourceId = null;
    try {
      synchronized (mLock) {
        boolean retval = false;
        Integer hashKey = Integer.valueOf(key.hashCode());
        if (mIndex.containsKey(hashKey)) {
          resourceId = mIndex.get(hashKey);
          retval = mStorage.touch(resourceId, key);
        } else {
          List<String> resourceIds = getResourceIds(key);
          for (int i = 0; i < resourceIds.size(); i++) {
            resourceId = resourceIds.get(i);
            if (!mResourceIndex.contains(resourceId)) {
              continue;
            }
            retval = mStorage.touch(resourceId, key);
            if (retval) {
              break;
            }
          }
        }
        if (retval) {
          addKeyToIndex(hashKey, resourceId);
        }
        return retval;
      }
    } catch (IOException e) {
      mCacheEventListener.onReadException(new SettableCacheEvent()
          .setCacheKey(key)
          .setResourceId(resourceId)
          .setException(e));
      return false;
    }
  }

  /**
   * Creates a temp file for writing outside the session lock
   */
  private DiskStorage.Inserter startInsert(
      final String resourceId,
      final CacheKey key)
      throws IOException {
    maybeEvictFilesInCacheDir();
    return mStorage.insert(resourceId, key);
  }

  /**
   * Commits the provided temp file to the cache, renaming it to match
   * the cache's hashing convention.
   */
  private BinaryResource endInsert(
      final DiskStorage.Inserter inserter,
      final CacheKey key,
      String resourceId) throws IOException {
    synchronized (mLock) {
      BinaryResource resource = inserter.commit(key);
      addKeyToIndex(key.hashCode(), resourceId);
      mCacheStats.increment(resource.size(), 1);
      return resource;
    }
  }

  @Override
  public BinaryResource insert(CacheKey key, WriterCallback callback) throws IOException {
    // Write to a temp file, then move it into place. This allows more parallelism
    // when writing files.
    SettableCacheEvent cacheEvent = new SettableCacheEvent()
        .setCacheKey(key);
    mCacheEventListener.onWriteAttempt(cacheEvent);
    String resourceId;
    synchronized (mLock) {
      // for multiple resource ids associated with the same image, we only write one file
      Integer hashKey = Integer.valueOf(key.hashCode());
      if (mIndex.containsKey(hashKey)) {
        resourceId = mIndex.get(hashKey);
      } else {
        resourceId = getFirstResourceId(key);
      }
    }
    cacheEvent.setResourceId(resourceId);
    try {
      // getting the file is synchronized
      DiskStorage.Inserter inserter = startInsert(resourceId, key);
      try {
        inserter.writeData(callback, key);
        // Committing the file is synchronized
        BinaryResource resource = endInsert(inserter, key, resourceId);
        cacheEvent.setItemSize(resource.size())
            .setCacheSize(mCacheStats.getSize());
        mCacheEventListener.onWriteSuccess(cacheEvent);
        return resource;
      } finally {
        if (!inserter.cleanUp()) {
          FLog.e(TAG, "Failed to delete temp file");
        }
      }
    } catch (IOException ioe) {
      cacheEvent.setException(ioe);
      mCacheEventListener.onWriteException(cacheEvent);
      FLog.e(TAG, "Failed inserting a file into the cache", ioe);
      throw ioe;
    }
  }

  @Override
  public void remove(CacheKey key) {
    synchronized (mLock) {
      try {
        String resourceId = null;
        Integer hashKey = Integer.valueOf(key.hashCode());
        if (mIndex.containsKey(hashKey)) {
          resourceId = mIndex.get(hashKey);
          mStorage.remove(resourceId);
        } else {
          List<String> resourceIds = getResourceIds(key);
          for (int i = 0; i < resourceIds.size(); i++) {
            resourceId = resourceIds.get(i);
            mStorage.remove(resourceId);
          }
        }
        removeKeyFromIndex(hashKey);
      } catch (IOException e) {
        mCacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.DELETE_FILE,
            TAG,
            "delete: " + e.getMessage(),
            e);
      }
    }
  }

  /**
   * Deletes old cache files.
   * @param cacheExpirationMs files older than this will be deleted.
   * @return the age in ms of the oldest file remaining in the cache.
   */
  @Override
  public long clearOldEntries(long cacheExpirationMs) {
    long oldestRemainingEntryAgeMs = 0L;
    synchronized (mLock) {
      try {
        long now = mClock.now();
        Collection<DiskStorage.Entry> allEntries = mStorage.getEntries();
        final long cacheSizeBeforeClearance = mCacheStats.getSize();
        int itemsRemovedCount = 0;
        long itemsRemovedSize = 0L;
        for (DiskStorage.Entry entry : allEntries) {
          // entry age of zero is disallowed.
          long entryAgeMs = Math.max(1, Math.abs(now - entry.getTimestamp()));
          if (entryAgeMs >= cacheExpirationMs) {
            long entryRemovedSize = mStorage.remove(entry);
            removeValueFromIndex(entry.getId());
            if (entryRemovedSize > 0) {
              itemsRemovedCount++;
              itemsRemovedSize += entryRemovedSize;
              mCacheEventListener.onEviction(new SettableCacheEvent()
                  .setResourceId(entry.getId())
                  .setEvictionReason(CacheEventListener.EvictionReason.CONTENT_STALE)
                  .setItemSize(entryRemovedSize)
                  .setCacheSize(cacheSizeBeforeClearance - itemsRemovedSize));
            }
          } else {
            oldestRemainingEntryAgeMs = Math.max(oldestRemainingEntryAgeMs, entryAgeMs);
          }
        }
        mStorage.purgeUnexpectedResources();
        if (itemsRemovedCount > 0) {
          maybeUpdateFileCacheSize();
          mCacheStats.increment(-itemsRemovedSize, -itemsRemovedCount);
        }
      } catch (IOException ioe) {
        mCacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.EVICTION,
            TAG,
            "clearOldEntries: " + ioe.getMessage(),
            ioe);
      }
    }
    return oldestRemainingEntryAgeMs;
  }

  /**
   * Test if the cache size has exceeded its limits, and if so, evict some files.
   * It also calls maybeUpdateFileCacheSize
   *
   * This method uses mLock for synchronization purposes.
   */
  private void maybeEvictFilesInCacheDir() throws IOException {
    synchronized (mLock) {
      boolean calculatedRightNow = maybeUpdateFileCacheSize();

      // Update the size limit (mCacheSizeLimit)
      updateFileCacheSizeLimit();

      long cacheSize = mCacheStats.getSize();
      // If we are going to evict force a recalculation of the size
      // (except if it was already calculated!)
      if (cacheSize > mCacheSizeLimit && !calculatedRightNow) {
        mCacheStats.reset();
        maybeUpdateFileCacheSize();
      }

      // If size has exceeded the size limit, evict some files
      if (cacheSize > mCacheSizeLimit) {
      evictAboveSize(
          mCacheSizeLimit * 9 / 10,
          CacheEventListener.EvictionReason.CACHE_FULL); // 90%
      }
    }
  }

  @GuardedBy("mLock")
  private void evictAboveSize(
      long desiredSize,
      CacheEventListener.EvictionReason reason) throws IOException {
    Collection<DiskStorage.Entry> entries;
    try {
      entries = getSortedEntries(mStorage.getEntries());
    } catch (IOException ioe) {
      mCacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.EVICTION,
          TAG,
          "evictAboveSize: " + ioe.getMessage(),
          ioe);
      throw ioe;
    }

    long cacheSizeBeforeClearance = mCacheStats.getSize();
    long deleteSize = cacheSizeBeforeClearance - desiredSize;
    int itemCount = 0;
    long sumItemSizes = 0L;
    for (DiskStorage.Entry entry: entries) {
      if (sumItemSizes > (deleteSize)) {
        break;
      }
      long deletedSize = mStorage.remove(entry);
      removeValueFromIndex(entry.getId());
      if (deletedSize > 0) {
        itemCount++;
        sumItemSizes += deletedSize;
        mCacheEventListener.onEviction(new SettableCacheEvent()
            .setResourceId(entry.getId())
            .setEvictionReason(reason)
            .setItemSize(deletedSize)
            .setCacheSize(cacheSizeBeforeClearance - sumItemSizes)
            .setCacheLimit(desiredSize));
      }
    }
    mCacheStats.increment(-sumItemSizes, -itemCount);
    mStorage.purgeUnexpectedResources();
  }

  /**
   * If any file timestamp is in the future (beyond now + FUTURE_TIMESTAMP_THRESHOLD_MS), we will
   * set its effective timestamp to 0 (the beginning of unix time), thus sending it to the head of
   * the queue for eviction (entries with the lowest timestamps are evicted first). This is a
   * safety check in case we get files that are written with a future timestamp.
   * We are adding a small delta (this constant) to account for network time changes, timezone
   * changes, etc.
   */
  private Collection<DiskStorage.Entry> getSortedEntries(Collection<DiskStorage.Entry> allEntries) {
    final long threshold = mClock.now() + DiskStorageCache.FUTURE_TIMESTAMP_THRESHOLD_MS;
    ArrayList<DiskStorage.Entry> sortedList = new ArrayList<>(allEntries.size());
    ArrayList<DiskStorage.Entry> listToSort = new ArrayList<>(allEntries.size());
    for (DiskStorage.Entry entry : allEntries) {
      if (entry.getTimestamp() > threshold) {
        sortedList.add(entry);
      } else {
        listToSort.add(entry);
      }
    }
    Collections.sort(listToSort, mEntryEvictionComparatorSupplier.get());
    sortedList.addAll(listToSort);
    return sortedList;
  }

  /**
   * Helper method that sets the cache size limit to be either a high, or a low limit.
   * If there is not enough free space to satisfy the high limit, it is set to the low limit.
   */
  @GuardedBy("mLock")
  private void updateFileCacheSizeLimit() {
    // Test if mCacheSizeLimit can be set to the high limit
    boolean isAvailableSpaceLowerThanHighLimit =
        mStatFsHelper.testLowDiskSpace(
            StatFsHelper.StorageType.INTERNAL,
            mDefaultCacheSizeLimit - mCacheStats.getSize());
    if (isAvailableSpaceLowerThanHighLimit) {
      mCacheSizeLimit = mLowDiskSpaceCacheSizeLimit;
    } else {
      mCacheSizeLimit = mDefaultCacheSizeLimit;
    }
  }

  public long getSize() {
    return mCacheStats.getSize();
  }

  public void clearAll() {
    synchronized (mLock) {
      try {
        mStorage.clearAll();
        mResourceIndex.clear();
        mIndex.clear();
      } catch (IOException ioe) {
        mCacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.EVICTION,
            TAG,
            "clearAll: " + ioe.getMessage(),
            ioe);
      }
      DiskStorageCacheUtil.clearDiskEntries(mSharedPreferences);
      mCacheStats.reset();
    }
  }

  @Override
  public boolean hasKeySync(CacheKey key) {
    synchronized (mLock) {
      int hashKey = key.hashCode();
      if (mIndex.containsKey(hashKey)) {
        return true;
      }
      String resourceId = null;
      List<String> resourceIds = getResourceIds(key);
      for (int i = 0; i< resourceIds.size(); i++) {
        resourceId = resourceIds.get(i);
        if (mResourceIndex.contains(resourceId)) {
          mIndex.put(hashKey, resourceId);
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public boolean hasKey(final CacheKey key) {
    synchronized (mLock) {
      if (hasKeySync(key)) {
        return true;
      }
      try {
        String resourceId = null;
        boolean retval = false;
        List<String> resourceIds = getResourceIds(key);
        for (int i = 0; i < resourceIds.size(); i++) {
          resourceId = resourceIds.get(i);
          retval = mStorage.contains(resourceId, key);
          if (retval) {
            break;
          }
        }
        if (retval) {
          addKeyToIndex(key.hashCode(), resourceId);
        }
        return retval;
      } catch (IOException e) {
        return false;
      }
    }
  }

  @Override
  public void trimToMinimum() {
    synchronized (mLock) {
      maybeUpdateFileCacheSize();
      long cacheSize = mCacheStats.getSize();
      if (mCacheSizeLimitMinimum <= 0 || cacheSize <= 0 || cacheSize < mCacheSizeLimitMinimum) {
        return;
      }
      double trimRatio = 1 - (double) mCacheSizeLimitMinimum / (double) cacheSize;
      if (trimRatio > TRIMMING_LOWER_BOUND) {
        trimBy(trimRatio);
      }
    }
  }

  @Override
  public void trimToNothing() {
    clearAll();
  }

  private void trimBy(final double trimRatio) {
    synchronized (mLock) {
      try {
        // Force update the ground truth if we are about to evict
        mCacheStats.reset();
        maybeUpdateFileCacheSize();
        long cacheSize = mCacheStats.getSize();
        long newMaxBytesInFiles = cacheSize - (long) (trimRatio * cacheSize);
        evictAboveSize(
            newMaxBytesInFiles,
            CacheEventListener.EvictionReason.CACHE_MANAGER_TRIMMED);
      } catch (IOException ioe) {
        mCacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.EVICTION,
            TAG,
            "trimBy: " + ioe.getMessage(),
            ioe);
      }
    }
  }

  /**
   * If file cache size is not calculated or if it was calculated
   * a long time ago (FILECACHE_SIZE_UPDATE_PERIOD_MS) recalculated from file listing.
   * @return true if it was recalculated, false otherwise.
   */
  @GuardedBy("mLock")
  private boolean maybeUpdateFileCacheSize() {
    boolean result = false;
    long now = mClock.now();
    if ((!mCacheStats.isInitialized()) ||
        mCacheSizeLastUpdateTime == UNINITIALIZED ||
        (now - mCacheSizeLastUpdateTime) > FILECACHE_SIZE_UPDATE_PERIOD_MS) {
      maybeUpdateFileCacheSizeAndIndex();
      mCacheSizeLastUpdateTime = now;
      result = true;
    }
    return result;
  }

  @GuardedBy("mLock")
  private void maybeUpdateFileCacheSizeAndIndex() {
    long size = 0;
    int count = 0;
    boolean foundFutureTimestamp = false;
    int numFutureFiles = 0;
    int sizeFutureFiles = 0;
    long maxTimeDelta = -1;
    long now = mClock.now();
    long timeThreshold = now + FUTURE_TIMESTAMP_THRESHOLD_MS;
    Set<String> tempResourceIndex = new HashSet<>();
    try {
      Collection<DiskStorage.Entry> entries = mStorage.getEntries();
      for (DiskStorage.Entry entry: entries) {
        count++;
        size += entry.getSize();

        //Check if any files have a future timestamp, beyond our threshold
        if (entry.getTimestamp() > timeThreshold) {
          foundFutureTimestamp = true;
          numFutureFiles++;
          sizeFutureFiles += entry.getSize();
          maxTimeDelta = Math.max(entry.getTimestamp() - now, maxTimeDelta);
        } else {
          tempResourceIndex.add(entry.getId());
        }
      }
      if (foundFutureTimestamp) {
        mCacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.READ_INVALID_ENTRY,
            TAG,
            "Future timestamp found in " + numFutureFiles +
                " files , with a total size of " + sizeFutureFiles +
                " bytes, and a maximum time delta of " + maxTimeDelta + "ms",
            null);
      }
      if ((mCacheStats.getCount() != count || mCacheStats.getSize() != size)) {
        mResourceIndex.clear();
        mResourceIndex.addAll(tempResourceIndex);
        mIndex = DiskStorageCacheUtil.readStoredIndex(mSharedPreferences, mResourceIndex);
        mCacheStats.set(size, count);
      }
    } catch (IOException ioe) {
      mCacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.GENERIC_IO,
          TAG,
          "calcFileCacheSize: " + ioe.getMessage(),
          ioe);
    }
  }

  private static List<String> getResourceIds(final CacheKey key) {
    try {
      final List<String> ids;
      if (key instanceof MultiCacheKey) {
        List<CacheKey> keys = ((MultiCacheKey) key).getCacheKeys();
        ids = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
           ids.add(secureHashKey(keys.get(i)));
        }
      } else {
        ids = new ArrayList<>(1);
        ids.add(secureHashKey(key));
      }
      return ids;
    } catch (UnsupportedEncodingException e) {
      // This should never happen. All VMs support UTF-8
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  static String getFirstResourceId(final CacheKey key) {
    try {
      if (key instanceof MultiCacheKey) {
        List<CacheKey> keys = ((MultiCacheKey) key).getCacheKeys();
        return secureHashKey(keys.get(0));
      } else {
        return secureHashKey(key);
      }
    } catch (UnsupportedEncodingException e) {
      // This should never happen. All VMs support UTF-8
      throw new RuntimeException(e);
    }
  }

  private static String secureHashKey(final CacheKey key) throws UnsupportedEncodingException {
    return SecureHashUtil.makeSHA1HashBase64(key.toString().getBytes("UTF-8"));
  }

  private static SharedPreferences initializeSharedPreferences(
      Context context,
      String directoryName) {
    Context applicationContext = context.getApplicationContext();
    return applicationContext.getSharedPreferences(
        SHARED_PREFS_FILENAME_PREFIX + directoryName,
        Context.MODE_PRIVATE);
  }

  @GuardedBy("mLock")
  private void addKeyToIndex(Integer hashKey, String resourceId) {
    mIndex.put(hashKey, resourceId);
    mResourceIndex.add(resourceId);
    DiskStorageCacheUtil.addDiskCacheEntry(hashKey, resourceId, mSharedPreferences);
  }

  @GuardedBy("mLock")
  private void removeKeyFromIndex(Integer hashKey) {
    String resourceId = mIndex.remove(hashKey);
    if (resourceId != null) {
      mResourceIndex.remove(resourceId);
      DiskStorageCacheUtil.deleteDiskCacheEntry(hashKey, mSharedPreferences);
    }
  }

  @GuardedBy("mLock")
  private void removeValueFromIndex(String resourceId) {
    Integer hashKey = removeKeyByValue(mIndex, resourceId);
    removeKeyFromIndex(hashKey);
  }

  private static Integer removeKeyByValue(Map<Integer, String> index, String value) {
    for (Map.Entry entry : index.entrySet()) {
      if (entry.getValue().equals(value)) {
        return (Integer) entry.getKey();
      }
    }
    return Integer.valueOf(0);
  }
}
