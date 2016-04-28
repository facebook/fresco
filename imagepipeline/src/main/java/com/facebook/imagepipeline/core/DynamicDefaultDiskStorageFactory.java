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
import com.facebook.cache.disk.DynamicDefaultDiskStorage;

/**
 * Factory for the default implementation of the DiskStorage.
 */
public class DynamicDefaultDiskStorageFactory implements DiskStorageFactory {

  @Override
  public DiskStorage get(DiskCacheConfig diskCacheConfig) {
    return new DynamicDefaultDiskStorage(
        diskCacheConfig.getVersion(),
        diskCacheConfig.getBaseDirectoryPathSupplier(),
        diskCacheConfig.getBaseDirectoryName(),
        diskCacheConfig.getCacheErrorLogger());
  }
}
