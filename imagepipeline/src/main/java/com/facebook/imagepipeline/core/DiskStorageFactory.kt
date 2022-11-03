/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.cache.disk.DiskStorage

/**
 * Represents a factory for the DiskStorage to use in the ImagePipeline. Used by
 * ImagePipelineConfig/Factory
 */
fun interface DiskStorageFactory {
  fun get(diskCacheConfig: DiskCacheConfig): DiskStorage
}
