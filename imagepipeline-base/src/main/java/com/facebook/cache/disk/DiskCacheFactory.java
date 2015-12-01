/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.disk;

/**
 * Factory class for building a {@link DiskStorageCache}.
 */
public class DiskCacheFactory {

  /**
   * Creates a new {@link DiskStorageCache} from the given {@link DiskCacheConfig}
   */
  public static DiskStorageCache newDiskStorageCache(DiskCacheConfig diskCacheConfig) {
    DiskStorageSupplier diskStorageSupplier = newDiskStorageSupplier(diskCacheConfig);
    DiskStorageCache.Params params = new DiskStorageCache.Params(
        diskCacheConfig.getMinimumSizeLimit(),
        diskCacheConfig.getLowDiskSpaceSizeLimit(),
        diskCacheConfig.getDefaultSizeLimit());
    return new DiskStorageCache(
        diskStorageSupplier,
        params,
        diskCacheConfig.getCacheEventListener(),
        diskCacheConfig.getCacheErrorLogger(),
        diskCacheConfig.getDiskTrimmableRegistry());
  }

  private static DiskStorageSupplier newDiskStorageSupplier(DiskCacheConfig diskCacheConfig) {
    return new DefaultDiskStorageSupplier(
        diskCacheConfig.getVersion(),
        diskCacheConfig.getBaseDirectoryPathSupplier(),
        diskCacheConfig.getBaseDirectoryName(),
        diskCacheConfig.getCacheErrorLogger());
  }
}
