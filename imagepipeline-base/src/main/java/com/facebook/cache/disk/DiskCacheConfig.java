/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import android.content.Context;
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
import com.facebook.common.util.ByteConstants;
import java.io.File;
import javax.annotation.Nullable;

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
  private final EntryEvictionComparatorSupplier mEntryEvictionComparatorSupplier;
  private final CacheErrorLogger mCacheErrorLogger;
  private final CacheEventListener mCacheEventListener;
  private final DiskTrimmableRegistry mDiskTrimmableRegistry;
  private final Context mContext;
  private final boolean mIndexPopulateAtStartupEnabled;

  private DiskCacheConfig(Builder builder) {
    mVersion = builder.mVersion;
    mBaseDirectoryName = Preconditions.checkNotNull(builder.mBaseDirectoryName);
    mBaseDirectoryPathSupplier = Preconditions.checkNotNull(builder.mBaseDirectoryPathSupplier);
    mDefaultSizeLimit = builder.mMaxCacheSize;
    mLowDiskSpaceSizeLimit = builder.mMaxCacheSizeOnLowDiskSpace;
    mMinimumSizeLimit = builder.mMaxCacheSizeOnVeryLowDiskSpace;
    mEntryEvictionComparatorSupplier =
        Preconditions.checkNotNull(builder.mEntryEvictionComparatorSupplier);
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
    mContext = builder.mContext;
    mIndexPopulateAtStartupEnabled = builder.mIndexPopulateAtStartupEnabled;
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

  public EntryEvictionComparatorSupplier getEntryEvictionComparatorSupplier() {
    return mEntryEvictionComparatorSupplier;
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

  public Context getContext() {
    return mContext;
  }

  public boolean getIndexPopulateAtStartupEnabled() {
    return mIndexPopulateAtStartupEnabled;
  }

  /**
   * Create a new builder.
   *
   * @param context If this is null, you must explicitly call
   *   {@link Builder#setBaseDirectoryPath(File)} or
   *   {@link Builder#setBaseDirectoryPathSupplier(Supplier)}
   *   or the config won't know where to physically locate the cache.
   * @return
   */
  public static Builder newBuilder(@Nullable Context context) {
    return new Builder(context);
  }

  public static class Builder {

    private int mVersion = 1;
    private String mBaseDirectoryName = "image_cache";
    private Supplier<File> mBaseDirectoryPathSupplier;
    private long mMaxCacheSize = 40 * ByteConstants.MB;
    private long mMaxCacheSizeOnLowDiskSpace = 10 * ByteConstants.MB;
    private long mMaxCacheSizeOnVeryLowDiskSpace = 2 * ByteConstants.MB;
    private EntryEvictionComparatorSupplier mEntryEvictionComparatorSupplier
        = new DefaultEntryEvictionComparatorSupplier();
    private CacheErrorLogger mCacheErrorLogger;
    private CacheEventListener mCacheEventListener;
    private DiskTrimmableRegistry mDiskTrimmableRegistry;
    private boolean mIndexPopulateAtStartupEnabled;

    private final @Nullable Context mContext;

    private Builder(@Nullable Context context) {
      mContext = context;
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
     * Provides the logic to determine the eviction order based on entry's access time and size
     */
    public Builder setEntryEvictionComparatorSupplier(EntryEvictionComparatorSupplier supplier) {
      mEntryEvictionComparatorSupplier = supplier;
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

    public Builder setIndexPopulateAtStartupEnabled(boolean indexEnabled) {
      mIndexPopulateAtStartupEnabled = indexEnabled;
      return this;
    }

    public DiskCacheConfig build() {
      Preconditions.checkState(
          mBaseDirectoryPathSupplier != null || mContext != null,
          "Either a non-null context or a base directory path or supplier must be provided.");
      if (mBaseDirectoryPathSupplier == null && mContext != null) {
        mBaseDirectoryPathSupplier = new Supplier<File>() {
          @Override
          public File get() {
            return mContext.getApplicationContext().getCacheDir();
          }
        };
      }
      return new DiskCacheConfig(this);
    }
  }
}
