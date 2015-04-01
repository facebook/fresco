/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.disk;

import java.io.File;

import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.cache.common.CacheEventListener;
import com.facebook.cache.common.NoOpCacheErrorLogger;
import com.facebook.cache.common.NoOpCacheEventListener;
import com.facebook.common.disk.DiskTrimmable;
import com.facebook.common.disk.DiskTrimmableRegistry;
import com.facebook.common.disk.NoOpDiskTrimmableRegistry;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;

/**
 * Configuration class for a {@link DiskStorageCache}.
 */
public class DiskCacheConfig {

  private final int mVersion;
  private final String mBaseDirectoryName;
  private final Supplier<File> mBaseDirectoryPathSupplier;
  private final long mDefaultSizeLimit;
  private final long mLowDiskSpaceSizeLimit;
  private final long mMinimumSizeLimit;
  private final CacheErrorLogger mCacheErrorLogger;
  private final CacheEventListener mCacheEventListener;
  private final DiskTrimmableRegistry mDiskTrimmableRegistry;

  private DiskCacheConfig(Builder builder) {
    mVersion = builder.mVersion;
    mBaseDirectoryName = Preconditions.checkNotNull(builder.mBaseDirectoryName);
    mBaseDirectoryPathSupplier = Preconditions.checkNotNull(builder.mBaseDirectoryPathSupplier);
    mDefaultSizeLimit = builder.mMaxCacheSize;
    mLowDiskSpaceSizeLimit = builder.mMaxCacheSizeOnLowDiskSpace;
    mMinimumSizeLimit = builder.mMaxCacheSizeOnVeryLowDiskSpace;
    mCacheErrorLogger =
        builder.mCacheErrorLogger == null ?
            NoOpCacheErrorLogger.getInstance() :
            builder.mCacheErrorLogger;
    mCacheEventListener =
        builder.mCacheEventListener == null ?
            NoOpCacheEventListener.getInstance() :
            builder.mCacheEventListener;
    mDiskTrimmableRegistry =
        builder.mDiskTrimmableRegistry == null ?
            NoOpDiskTrimmableRegistry.getInstance() :
            builder.mDiskTrimmableRegistry;
  }

  public int getVersion() {
    return mVersion;
  }

  public String getBaseDirectoryName() {
    return mBaseDirectoryName;
  }

  public Supplier<File> getBaseDirectoryPathSupplier() {
    return mBaseDirectoryPathSupplier;
  }

  public long getDefaultSizeLimit() {
    return mDefaultSizeLimit;
  }

  public long getLowDiskSpaceSizeLimit() {
    return mLowDiskSpaceSizeLimit;
  }

  public long getMinimumSizeLimit() {
    return mMinimumSizeLimit;
  }

  public CacheErrorLogger getCacheErrorLogger() {
    return mCacheErrorLogger;
  }

  public CacheEventListener getCacheEventListener() {
    return mCacheEventListener;
  }

  public DiskTrimmableRegistry getDiskTrimmableRegistry() {
    return mDiskTrimmableRegistry;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    public int mVersion = 1;
    public String mBaseDirectoryName;
    public Supplier<File> mBaseDirectoryPathSupplier;
    public long mMaxCacheSize;
    public long mMaxCacheSizeOnLowDiskSpace;
    public long mMaxCacheSizeOnVeryLowDiskSpace;
    public CacheErrorLogger mCacheErrorLogger;
    public CacheEventListener mCacheEventListener;
    public DiskTrimmableRegistry mDiskTrimmableRegistry;

    private Builder() {
    }

    /**
     * Sets the version.
     *
     * <p>The cache lives in a subdirectory identified by this version.
     */
    public Builder setVersion(int version) {
      mVersion = version;
      return this;
    }

    /**
     * Sets the name of the directory where the cache will be located.
     */
    public Builder setBaseDirectoryName(String baseDirectoryName) {
      mBaseDirectoryName = baseDirectoryName;
      return this;
    }

    /**
     * Sets the path to the base directory.
     *
     * <p>A directory with the given base directory name (see {@code setBaseDirectoryName}) will be
     * appended to this path.
     */
    public Builder setBaseDirectoryPath(final File baseDirectoryPath) {
      mBaseDirectoryPathSupplier = Suppliers.of(baseDirectoryPath);
      return this;
    }

    public Builder setBaseDirectoryPathSupplier(Supplier<File> baseDirectoryPathSupplier) {
      mBaseDirectoryPathSupplier = baseDirectoryPathSupplier;
      return this;
    }

    /**
     * This is the default maximum size of the cache.
     */
    public Builder setMaxCacheSize(long maxCacheSize) {
      mMaxCacheSize = maxCacheSize;
      return this;
    }

    /**
     * This is the maximum size of the cache that is used when the device is low on disk space.
     *
     * See {@link DiskTrimmable#trimToMinimum()}.
     */
    public Builder setMaxCacheSizeOnLowDiskSpace(long maxCacheSizeOnLowDiskSpace) {
      mMaxCacheSizeOnLowDiskSpace = maxCacheSizeOnLowDiskSpace;
      return this;
    }

    /**
     * This is the maximum size of the cache when the device is extremely low on disk space.
     *
     * See {@link DiskTrimmable#trimToNothing()}.
     */
    public Builder setMaxCacheSizeOnVeryLowDiskSpace(long maxCacheSizeOnVeryLowDiskSpace) {
      mMaxCacheSizeOnVeryLowDiskSpace = maxCacheSizeOnVeryLowDiskSpace;
      return this;
    }

    /**
     * The logger that is used to log errors made by the cache.
     */
    public Builder setCacheErrorLogger(CacheErrorLogger cacheErrorLogger) {
      mCacheErrorLogger = cacheErrorLogger;
      return this;
    }

    /**
     * The listener for cache events.
     */
    public Builder setCacheEventListener(CacheEventListener cacheEventListener) {
      mCacheEventListener = cacheEventListener;
      return this;
    }

    /**
     * The class that will contain a registry of caches to be trimmed in low disk space conditions.
     *
     * <p>See {@link DiskTrimmableRegistry}.
     */
    public Builder setDiskTrimmableRegistry(DiskTrimmableRegistry diskTrimmableRegistry) {
      mDiskTrimmableRegistry = diskTrimmableRegistry;
      return this;
    }

    public DiskCacheConfig build() {
      return new DiskCacheConfig(this);
    }
  }
}
