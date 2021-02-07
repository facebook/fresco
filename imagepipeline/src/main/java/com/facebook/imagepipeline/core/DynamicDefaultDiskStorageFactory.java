/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.cache.disk.DiskStorage;
import com.facebook.cache.disk.DynamicDefaultDiskStorage;
import com.facebook.infer.annotation.Nullsafe;

/** Factory for the default implementation of the DiskStorage. */
@Nullsafe(Nullsafe.Mode.LOCAL)
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
