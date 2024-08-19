/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import com.facebook.cache.disk.FileCache
import com.facebook.common.internal.ImmutableMap
import com.facebook.imagepipeline.cache.BufferedDiskCache

interface DiskCachesStore {
  val mainFileCache: FileCache
  val mainBufferedDiskCache: BufferedDiskCache
  val smallImageFileCache: FileCache
  val smallImageBufferedDiskCache: BufferedDiskCache
  val dynamicFileCaches: Map<String, FileCache>
  val dynamicBufferedDiskCaches: ImmutableMap<String, BufferedDiskCache>
}
