/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.cache.disk.FileCache
import com.facebook.common.internal.ImmutableMap
import com.facebook.common.internal.Supplier
import com.facebook.imagepipeline.cache.BufferedDiskCache
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker
import com.facebook.imagepipeline.memory.PoolFactory

class DiskCachesStoreFactory(
    private val fileCacheFactory: FileCacheFactory,
    private val poolFactory: PoolFactory,
    private val executorSupplier: ExecutorSupplier,
    private val imageCacheStatsTracker: ImageCacheStatsTracker,
    @MemoryChunkType private val memoryChunkType: Int,
    private val mainDiskCacheConfig: DiskCacheConfig,
    private val smallImageDiskCacheConfig: DiskCacheConfig,
    private val dynamicDiskCacheConfigMap: Map<String, DiskCacheConfig>?
) : Supplier<DiskCachesStore> {

  constructor(
      fileCacheFactory: FileCacheFactory,
      config: ImagePipelineConfigInterface,
  ) : this(
      fileCacheFactory = fileCacheFactory,
      poolFactory = config.poolFactory,
      executorSupplier = config.executorSupplier,
      imageCacheStatsTracker = config.imageCacheStatsTracker,
      memoryChunkType = config.memoryChunkType,
      mainDiskCacheConfig = config.mainDiskCacheConfig,
      smallImageDiskCacheConfig = config.smallImageDiskCacheConfig,
      dynamicDiskCacheConfigMap = config.dynamicDiskCacheConfigMap)

  private val diskCachesStore: DiskCachesStore by
      lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        object : DiskCachesStore {

          override val mainFileCache: FileCache by
              lazy(LazyThreadSafetyMode.SYNCHRONIZED) { fileCacheFactory.get(mainDiskCacheConfig) }

          override val mainBufferedDiskCache: BufferedDiskCache by
              lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                BufferedDiskCache(
                    mainFileCache,
                    poolFactory.getPooledByteBufferFactory(memoryChunkType),
                    poolFactory.pooledByteStreams,
                    executorSupplier.forLocalStorageRead(),
                    executorSupplier.forLocalStorageWrite(),
                    imageCacheStatsTracker)
              }

          override val smallImageFileCache: FileCache by
              lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                fileCacheFactory.get(smallImageDiskCacheConfig)
              }

          override val smallImageBufferedDiskCache: BufferedDiskCache by
              lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                BufferedDiskCache(
                    smallImageFileCache,
                    poolFactory.getPooledByteBufferFactory(memoryChunkType),
                    poolFactory.pooledByteStreams,
                    executorSupplier.forLocalStorageRead(),
                    executorSupplier.forLocalStorageWrite(),
                    imageCacheStatsTracker)
              }

          override val dynamicFileCaches: Map<String, FileCache> by
              lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                dynamicDiskCacheConfigMap?.let { cacheConfigMap ->
                  cacheConfigMap.mapValues { (_, cacheConfig) -> fileCacheFactory.get(cacheConfig) }
                } ?: run { emptyMap() }
              }

          override val dynamicBufferedDiskCaches: ImmutableMap<String, BufferedDiskCache> by
              lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                ImmutableMap.copyOf(
                    dynamicFileCaches.mapValues { (_, fileCache) ->
                      BufferedDiskCache(
                          fileCache,
                          poolFactory.getPooledByteBufferFactory(memoryChunkType),
                          poolFactory.pooledByteStreams,
                          executorSupplier.forLocalStorageRead(),
                          executorSupplier.forLocalStorageWrite(),
                          imageCacheStatsTracker)
                    })
              }
        }
      }

  override fun get(): DiskCachesStore = diskCachesStore
}
