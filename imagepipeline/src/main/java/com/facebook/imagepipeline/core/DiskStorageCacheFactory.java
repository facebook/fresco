/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.cache.disk.DiskStorage;
import com.facebook.cache.disk.DiskStorageCache;
import com.facebook.cache.disk.FileCache;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Factory for the default implementation of the FileCache.
 */
public class DiskStorageCacheFactory implements FileCacheFactory {

  private DiskStorageFactory mDiskStorageFactory;

  public DiskStorageCacheFactory(DiskStorageFactory diskStorageFactory) {
    mDiskStorageFactory = diskStorageFactory;
  }

  public static DiskStorageCache buildDiskStorageCache(
      DiskCacheConfig diskCacheConfig,
      DiskStorage diskStorage) {
    return buildDiskStorageCache(diskCacheConfig, diskStorage, Executors.newSingleThreadExecutor());
  }

  public static DiskStorageCache buildDiskStorageCache(
      DiskCacheConfig diskCacheConfig,
      DiskStorage diskStorage,
      Executor executorForBackgroundInit) {
    DiskStorageCache.Params params = new DiskStorageCache.Params(
        diskCacheConfig.getMinimumSizeLimit(),
        diskCacheConfig.getLowDiskSpaceSizeLimit(),
        diskCacheConfig.getDefaultSizeLimit());

    return new DiskStorageCache(
        diskStorage,
        diskCacheConfig.getEntryEvictionComparatorSupplier(),
        params,
        diskCacheConfig.getCacheEventListener(),
        diskCacheConfig.getCacheErrorLogger(),
        diskCacheConfig.getDiskTrimmableRegistry(),
        diskCacheConfig.getContext(),
        executorForBackgroundInit,
        diskCacheConfig.getIndexPopulateAtStartupEnabled());
  }

  @Override
  public FileCache get(DiskCacheConfig diskCacheConfig) {
    return buildDiskStorageCache(diskCacheConfig, mDiskStorageFactory.get(diskCacheConfig));
  }
}
