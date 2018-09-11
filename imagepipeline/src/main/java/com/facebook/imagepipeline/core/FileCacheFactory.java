/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.cache.disk.FileCache;

/**
 * Represents a factory for the FileCache to use in the ImagePipeline.
 * Used by ImagePipelineConfig/Factory
 */
public interface FileCacheFactory {

  FileCache get(DiskCacheConfig diskCacheConfig);
}
