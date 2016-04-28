/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.cache.disk.DiskStorage;
import com.facebook.cache.disk.DiskStorageCache;
import com.facebook.cache.disk.FileCache;

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
        diskCacheConfig.getDiskTrimmableRegistry());
  }

  @Override
  public FileCache get(DiskCacheConfig diskCacheConfig) {
    return buildDiskStorageCache(diskCacheConfig, mDiskStorageFactory.get(diskCacheConfig));
  }
}
