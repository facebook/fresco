/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.cache.disk.DiskStorage;
import com.facebook.cache.disk.DiskStorageCache;
import com.facebook.cache.disk.FileCache;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Factory for the default implementation of the FileCacheFactory. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DiskStorageCacheFactory implements FileCacheFactory {

  private DiskStorageFactory mDiskStorageFactory;

  public DiskStorageCacheFactory(DiskStorageFactory diskStorageFactory) {
    mDiskStorageFactory = diskStorageFactory;
  }

  private static DiskStorageCache buildDiskStorageCache(
      DiskCacheConfig diskCacheConfig, DiskStorage diskStorage) {
    return buildDiskStorageCache(diskCacheConfig, diskStorage, Executors.newSingleThreadExecutor());
  }

  private static DiskStorageCache buildDiskStorageCache(
      DiskCacheConfig diskCacheConfig,
      DiskStorage diskStorage,
      Executor executorForBackgroundInit) {
    DiskStorageCache.Params params =
        new DiskStorageCache.Params(
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
        executorForBackgroundInit,
        diskCacheConfig.getIndexPopulateAtStartupEnabled());
  }

  @Override
  public FileCache get(DiskCacheConfig diskCacheConfig) {
    return buildDiskStorageCache(diskCacheConfig, mDiskStorageFactory.get(diskCacheConfig));
  }
}
