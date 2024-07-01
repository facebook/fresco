/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory

import com.facebook.cache.common.CacheKey
import com.facebook.common.executors.SerialExecutorService
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.cache.CountingMemoryCache
import com.facebook.imagepipeline.core.ExecutorSupplier
import com.facebook.imagepipeline.image.CloseableImage
import java.util.concurrent.ExecutorService

object AnimatedFactoryProvider {
  private var implLoaded = false
  private var impl: AnimatedFactory? = null

  @JvmStatic
  fun getAnimatedFactory(
      platformBitmapFactory: PlatformBitmapFactory?,
      executorSupplier: ExecutorSupplier?,
      backingCache: CountingMemoryCache<CacheKey?, CloseableImage?>?,
      downscaleFrameToDrawableDimensions: Boolean,
      useBalancedAnimationStrategy: Boolean,
      animationFpsLimit: Int,
      bufferLengthMilliseconds: Int,
      serialExecutorService: ExecutorService?
  ): AnimatedFactory? {
    if (!implLoaded) {
      try {
        val clazz = Class.forName("com.facebook.fresco.animation.factory.AnimatedFactoryV2Impl")
        val constructor =
            clazz.getConstructor(
                PlatformBitmapFactory::class.java,
                ExecutorSupplier::class.java,
                CountingMemoryCache::class.java,
                java.lang.Boolean.TYPE,
                java.lang.Boolean.TYPE,
                Integer.TYPE,
                Integer.TYPE,
                SerialExecutorService::class.java)
        impl =
            constructor.newInstance(
                platformBitmapFactory,
                executorSupplier,
                backingCache,
                downscaleFrameToDrawableDimensions,
                useBalancedAnimationStrategy,
                animationFpsLimit,
                bufferLengthMilliseconds,
                serialExecutorService) as AnimatedFactory
      } catch (e: Throwable) {
        // Head in the sand
      }
      if (impl != null) {
        implLoaded = true
      }
    }
    return impl
  }
}
