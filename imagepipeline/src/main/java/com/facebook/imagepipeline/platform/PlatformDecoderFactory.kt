/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.os.Build
import androidx.core.util.Pools
import androidx.core.util.Pools.SynchronizedPool
import com.facebook.common.memory.DecodeBufferHelper
import com.facebook.imagepipeline.core.NativeCodeSetup
import com.facebook.imagepipeline.memory.FlexByteArrayPool
import com.facebook.imagepipeline.memory.PoolFactory
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer

object PlatformDecoderFactory {

  /**
   * Provide the implementation of the PlatformDecoder for the current platform using the provided
   * PoolFactory
   *
   * @param poolFactory The PoolFactory
   * @return The PlatformDecoder implementation
   */
  @JvmStatic
  @JvmOverloads
  fun buildPlatformDecoder(
      poolFactory: PoolFactory,
      gingerbreadDecoderEnabled: Boolean,
      useDecodeBufferHelper: Boolean = false,
      platformDecoderOptions: PlatformDecoderOptions
  ): PlatformDecoder =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        OreoDecoder(
            poolFactory.bitmapPool,
            createPool(poolFactory, useDecodeBufferHelper),
            platformDecoderOptions)
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ||
          !NativeCodeSetup.getUseNativeCode()) {
        ArtDecoder(
            poolFactory.bitmapPool,
            createPool(poolFactory, useDecodeBufferHelper),
            platformDecoderOptions)
      } else {
        try {
          if (gingerbreadDecoderEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val clazz =
                Class.forName("com.facebook.imagepipeline.platform.GingerbreadPurgeableDecoder")
            clazz.getConstructor().newInstance() as PlatformDecoder
          } else {
            val clazz = Class.forName("com.facebook.imagepipeline.platform.KitKatPurgeableDecoder")
            clazz
                .getConstructor(FlexByteArrayPool::class.java)
                .newInstance(poolFactory.flexByteArrayPool) as PlatformDecoder
          }
        } catch (e: ClassNotFoundException) {
          throw RuntimeException("Wrong Native code setup, reflection failed.", e)
        } catch (e: IllegalAccessException) {
          throw RuntimeException("Wrong Native code setup, reflection failed.", e)
        } catch (e: NoSuchMethodException) {
          throw RuntimeException("Wrong Native code setup, reflection failed.", e)
        } catch (e: InvocationTargetException) {
          throw RuntimeException("Wrong Native code setup, reflection failed.", e)
        } catch (e: InstantiationException) {
          throw RuntimeException("Wrong Native code setup, reflection failed.", e)
        }
      }

  @JvmStatic
  fun createPool(poolFactory: PoolFactory, useDecodeBufferHelper: Boolean): Pools.Pool<ByteBuffer> {
    if (useDecodeBufferHelper) {
      return DecodeBufferHelper.INSTANCE
    }
    val maxNumThreads = poolFactory.flexByteArrayPoolMaxNumThreads
    val pool: Pools.Pool<ByteBuffer> = SynchronizedPool(maxNumThreads)
    for (i in 0 until maxNumThreads) {
      pool.release(ByteBuffer.allocate(DecodeBufferHelper.getRecommendedDecodeBufferSize()))
    }
    return pool
  }
}
