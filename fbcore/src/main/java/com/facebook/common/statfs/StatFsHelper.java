/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.statfs;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import com.facebook.common.internal.Throwables;

/**
 * Helper class that periodically checks the amount of free space available.
 * <p>To keep the overhead low, it caches the free space information, and
 * only updates that info after two minutes.
 *
 * <p>It is a singleton, and is thread-safe.
 *
 * <p>Initialization is delayed until first use, so the first call to any method may incur some
 * additional cost.
 */
@ThreadSafe
public class StatFsHelper {

  public enum StorageType {
    INTERNAL,
    EXTERNAL
  };

  private static StatFsHelper sStatsFsHelper;

  // Time interval for updating disk information
  private static final long RESTAT_INTERVAL_MS = TimeUnit.MINUTES.toMillis(2);

  private volatile StatFs mInternalStatFs = null;
  private volatile File mInternalPath;

  private volatile StatFs mExternalStatFs = null;
  private volatile File mExternalPath;

  @GuardedBy("lock")
  private long mLastRestatTime;

  private final Lock lock;
  private volatile boolean mInitialized = false;

  public synchronized static StatFsHelper getInstance() {
    if (sStatsFsHelper == null) {
      sStatsFsHelper = new StatFsHelper();
    }
    return sStatsFsHelper;
  }

  /**
   * Constructor.
   *
   * <p>Initialization is delayed until first use, so we must call {@link #ensureInitialized()}
   * when implementing member methods.
   */
  protected StatFsHelper() {
    lock = new ReentrantLock();
  }

  /**
   * Initialization code that can sometimes take a long time.
   */
  private void ensureInitialized() {
    if (!mInitialized) {
     lock.lock();
      try {
        if (!mInitialized) {
          mInternalPath = Environment.getDataDirectory();
          mExternalPath = Environment.getExternalStorageDirectory();
          updateStats();
          mInitialized = true;
        }
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Check if free space available in the filesystem is greater than the given threshold.
   * Note that the free space stats are cached and updated in intervals of RESTAT_INTERVAL_MS.
   * If the amount of free space has crossed over the threshold since the last update, it will
   * return incorrect results till the space stats are updated again.
   *
   * @param storageType StorageType (internal or external) to test
   * @param freeSpaceThreshold compare the available free space to this size
   * @return whether free space is lower than the input freeSpaceThreshold,
   * returns true if disk information is not available
   */
  public boolean testLowDiskSpace(StorageType storageType, long freeSpaceThreshold) {
    ensureInitialized();

    long availableStorageSpace = getAvailableStorageSpace(storageType);
    if (availableStorageSpace > 0) {
      return availableStorageSpace < freeSpaceThreshold;
    }
    return true;
  }

  /**
   * Gets the information about the available storage space
   * either internal or external depends on the give input
   * @param storageType Internal or external storage type
   * @return available space in bytes, 0 if no information is available
   */
  public long getAvailableStorageSpace(StorageType storageType) {
    ensureInitialized();

    maybeUpdateStats();

    StatFs statFS = storageType == StorageType.INTERNAL ? mInternalStatFs : mExternalStatFs;
    if (statFS != null) {
      long blockSize = statFS.getBlockSize();
      long availableBlocks = statFS.getAvailableBlocks();
      return blockSize * availableBlocks;
    }
    return 0;
  }

  /**
   * Thread-safe call to update disk stats. Update occurs if the thread is able to acquire
   * the lock (i.e., no other thread is updating it at the same time), and it has been
   * at least RESTAT_INTERVAL_MS since the last update.
   * Assumes that initialization has been completed before this method is called.
   */
  private void maybeUpdateStats() {
    // Update the free space if able to get the lock,
    // with a frequency of once in RESTAT_INTERVAL_MS
    if (lock.tryLock()) {
      try {
        if ((SystemClock.elapsedRealtime() - mLastRestatTime) > RESTAT_INTERVAL_MS) {
          updateStats();
        }
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Thread-safe call to reset the disk stats.
   * If we know that the free space has changed recently (for example, if we have
   * deleted files), use this method to reset the internal state and
   * start tracking disk stats afresh, resetting the internal timer for updating stats.
   */
  public void resetStats() {
    // Update the free space if able to get the lock
    if (lock.tryLock()) {
      try {
          ensureInitialized();

          updateStats();
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * (Re)calculate the stats.
   * It is the callers responsibility to ensure thread-safety.
   * Assumes that it is called after initialization (or at the end of it).
   */
  @GuardedBy("lock")
  private void updateStats() {
    mInternalStatFs = updateStatsHelper(mInternalStatFs, mInternalPath);
    mExternalStatFs = updateStatsHelper(mExternalStatFs, mExternalPath);
    mLastRestatTime = SystemClock.elapsedRealtime();
  }

  /**
   * Update stats for a single directory and return the StatFs object for that directory. If the
   * directory does not exist or the StatFs restat() or constructor fails (throws), a null StatFs
   * object is returned.
   */
  private StatFs updateStatsHelper(@Nullable StatFs statfs, @Nullable File dir) {
    if(dir == null || !dir.exists()) {
      // The path does not exist, do not track stats for it.
      return null;
    }

    try {
      if (statfs == null) {
        // Create a new StatFs object for this path.
        statfs = createStatFs(dir.getAbsolutePath());
      } else {
        // Call restat and keep the existing StatFs object.
        statfs.restat(dir.getAbsolutePath());
      }
    } catch (IllegalArgumentException ex) {
      // Invalidate the StatFs object for this directory. The native StatFs implementation throws
      // IllegalArgumentException in the case that the statfs() system call fails and it invalidates
      // its internal data structures so subsequent calls against the StatFs object will fail or
      // throw (so we should make no more calls on the object). The most likely reason for this call
      // to fail is because the provided path no longer exists. The next call to updateStats() will
      // a new statfs object if the path exists. This will handle the case that a path is unmounted
      // and later remounted (but it has to have been mounted when this object was initialized).
      statfs = null;
    } catch (Throwable ex) {
      // Any other exception types are not expected and should be propagated as runtime errors.
      throw Throwables.propagate(ex);
    }

    return statfs;
  }

  protected static StatFs createStatFs(String path) {
    return new StatFs(path);
  }
}
