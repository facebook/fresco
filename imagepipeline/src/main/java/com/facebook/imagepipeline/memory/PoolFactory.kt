/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.os.Build
import com.facebook.common.memory.ByteArrayPool
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.common.memory.PooledByteStreams
import com.facebook.imagepipeline.core.MemoryChunkType
import com.facebook.imagepipeline.core.MemoryChunkType.BUFFER_MEMORY
import com.facebook.imagepipeline.core.MemoryChunkType.NATIVE_MEMORY
import com.facebook.imagepipeline.core.NativeCodeSetup
import com.facebook.imagepipeline.memory.DefaultBitmapPoolParams.get
import java.lang.reflect.InvocationTargetException

/** Factory class for pools. */
open class PoolFactory(private val config: PoolConfig) {

  open val bitmapPool: BitmapPool by lazy {
    val bitmapPoolType = config.bitmapPoolType
    when (bitmapPoolType) {
      BitmapPoolType.DUMMY -> DummyBitmapPool()
      BitmapPoolType.DUMMY_WITH_TRACKING -> DummyTrackingInUseBitmapPool()
      BitmapPoolType.EXPERIMENTAL ->
          LruBitmapPool(
              config.bitmapPoolMaxPoolSize,
              config.bitmapPoolMaxBitmapSize,
              NoOpPoolStatsTracker.getInstance(),
              if (config.isRegisterLruBitmapPoolAsMemoryTrimmable) config.memoryTrimmableRegistry
              else null)

      BitmapPoolType.LEGACY_DEFAULT_PARAMS ->
          BucketsBitmapPool(
              config.memoryTrimmableRegistry,
              get(),
              config.bitmapPoolStatsTracker,
              config.isIgnoreBitmapPoolHardCap)

      BitmapPoolType.LEGACY ->
          if (Build.VERSION.SDK_INT >= 21) {
            BucketsBitmapPool(
                config.memoryTrimmableRegistry,
                config.bitmapPoolParams,
                config.bitmapPoolStatsTracker,
                config.isIgnoreBitmapPoolHardCap)
          } else {
            DummyBitmapPool()
          }

      else ->
          if (Build.VERSION.SDK_INT >= 21) {
            BucketsBitmapPool(
                config.memoryTrimmableRegistry,
                config.bitmapPoolParams,
                config.bitmapPoolStatsTracker,
                config.isIgnoreBitmapPoolHardCap)
          } else {
            DummyBitmapPool()
          }
    }
  }

  val bufferMemoryChunkPool: MemoryChunkPool? by lazy {
    try {
      val clazz = Class.forName("com.facebook.imagepipeline.memory.BufferMemoryChunkPool")
      val cons =
          clazz.getConstructor(
              MemoryTrimmableRegistry::class.java,
              PoolParams::class.java,
              PoolStatsTracker::class.java,
              Boolean::class.javaPrimitiveType)
      cons.newInstance(
          config.memoryTrimmableRegistry,
          config.memoryChunkPoolParams,
          config.memoryChunkPoolStatsTracker,
          config.isIgnoreBitmapPoolHardCap) as MemoryChunkPool
    } catch (e: ClassNotFoundException) {
      null
    } catch (e: IllegalAccessException) {
      null
    } catch (e: InstantiationException) {
      null
    } catch (e: NoSuchMethodException) {
      null
    } catch (e: InvocationTargetException) {
      null
    }
  }

  val flexByteArrayPool: FlexByteArrayPool by lazy {
    FlexByteArrayPool(config.memoryTrimmableRegistry, config.flexByteArrayPoolParams)
  }

  val flexByteArrayPoolMaxNumThreads: Int
    get() = config.flexByteArrayPoolParams.maxNumThreads

  val nativeMemoryChunkPool: MemoryChunkPool? by lazy {
    try {
      val clazz = Class.forName("com.facebook.imagepipeline.memory.NativeMemoryChunkPool")
      val cons =
          clazz.getConstructor(
              MemoryTrimmableRegistry::class.java,
              PoolParams::class.java,
              PoolStatsTracker::class.java,
              Boolean::class.javaPrimitiveType)
      cons.newInstance(
          config.memoryTrimmableRegistry,
          config.memoryChunkPoolParams,
          config.memoryChunkPoolStatsTracker,
          config.isIgnoreBitmapPoolHardCap) as MemoryChunkPool
    } catch (e: ClassNotFoundException) {
      null
    } catch (e: IllegalAccessException) {
      null
    } catch (e: InstantiationException) {
      null
    } catch (e: NoSuchMethodException) {
      null
    } catch (e: InvocationTargetException) {
      null
    }
  }

  private val ashmemMemoryChunkPool: MemoryChunkPool? by lazy {
    try {
      val clazz = Class.forName("com.facebook.imagepipeline.memory.AshmemMemoryChunkPool")
      val cons =
          clazz.getConstructor(
              MemoryTrimmableRegistry::class.java,
              PoolParams::class.java,
              PoolStatsTracker::class.java,
              Boolean::class.javaPrimitiveType)
      cons.newInstance(
          config.memoryTrimmableRegistry,
          config.memoryChunkPoolParams,
          config.memoryChunkPoolStatsTracker,
          config.isIgnoreBitmapPoolHardCap) as MemoryChunkPool
    } catch (e: ClassNotFoundException) {
      null
    } catch (e: IllegalAccessException) {
      null
    } catch (e: InstantiationException) {
      null
    } catch (e: NoSuchMethodException) {
      null
    } catch (e: InvocationTargetException) {
      null
    }
  }

  val pooledByteBufferFactory: PooledByteBufferFactory
    get() =
        getPooledByteBufferFactory(
            if (NativeCodeSetup.getUseNativeCode()) NATIVE_MEMORY else BUFFER_MEMORY)

  private var _pooledByteBufferFactory: PooledByteBufferFactory? = null

  fun getPooledByteBufferFactory(memoryChunkType: Int): PooledByteBufferFactory {
    _pooledByteBufferFactory?.let {
      return it
    }
    val memoryChunkPool = this.getMemoryChunkPool(memoryChunkType)
    requireNotNull(memoryChunkPool) { "failed to get pool for chunk type: $memoryChunkType" }
    val pooledByteBufferFactory = MemoryPooledByteBufferFactory(memoryChunkPool, pooledByteStreams)
    _pooledByteBufferFactory = pooledByteBufferFactory
    return pooledByteBufferFactory
  }

  val pooledByteStreams: PooledByteStreams by lazy { PooledByteStreams(smallByteArrayPool) }

  val sharedByteArray: SharedByteArray by lazy {
    SharedByteArray(config.memoryTrimmableRegistry, config.flexByteArrayPoolParams)
  }

  val smallByteArrayPool: ByteArrayPool by lazy {
    GenericByteArrayPool(
        config.memoryTrimmableRegistry,
        config.smallByteArrayPoolParams,
        config.smallByteArrayPoolStatsTracker)
  }

  private fun getMemoryChunkPool(@MemoryChunkType memoryChunkType: Int): MemoryChunkPool? {
    return when (memoryChunkType) {
      MemoryChunkType.NATIVE_MEMORY -> nativeMemoryChunkPool
      MemoryChunkType.BUFFER_MEMORY -> bufferMemoryChunkPool
      MemoryChunkType.ASHMEM_MEMORY -> ashmemMemoryChunkPool
      else -> throw IllegalArgumentException("Invalid MemoryChunkType")
    }
  }
}
