/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.factory

import android.content.Context
import android.graphics.Rect
import com.facebook.cache.common.CacheKey
import com.facebook.common.executors.DefaultSerialExecutorService
import com.facebook.common.executors.SerialExecutorService
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.common.internal.DoNotStrip
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.common.time.RealtimeSinceBootClock
import com.facebook.fresco.animation.drawable.AnimatedDrawable2
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.animated.factory.AnimatedFactory
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImpl
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.cache.CountingMemoryCache
import com.facebook.imagepipeline.core.ExecutorSupplier
import com.facebook.imagepipeline.drawable.DrawableFactory
import com.facebook.imagepipeline.image.CloseableImage
import javax.annotation.concurrent.NotThreadSafe

/**
 * [AnimatedFactory] implementation for animations v2 that creates [AnimatedDrawable2] drawables.
 *
 * This factory handles the creation of animated drawables for GIF and WebP formats. It manages the
 * backend providers and utilities needed for animation processing.
 */
@NotThreadSafe
@DoNotStrip
class AnimatedFactoryV2Impl
@DoNotStrip
constructor(
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val executorSupplier: ExecutorSupplier,
    private val backingCache: CountingMemoryCache<CacheKey, CloseableImage>,
    private val downscaleFrameToDrawableDimensions: Boolean,
    private val useBufferLoaderStrategy: Boolean,
    var animationFpsLimit: Int,
    var bufferLengthMilliseconds: Int,
    var serialExecutorService: SerialExecutorService?,
    private val enableBufferFrameLoaderFix: Boolean = false,
) : AnimatedFactory {

  private var animatedDrawableBackendProvider: AnimatedDrawableBackendProvider? = null
  private var animatedDrawableUtil: AnimatedDrawableUtil? = null
  private var animatedDrawableFactory: DrawableFactory? = null

  override fun getAnimatedDrawableFactory(context: Context?): DrawableFactory? {
    if (animatedDrawableFactory == null) {
      animatedDrawableFactory = createDrawableFactory()
    }
    return animatedDrawableFactory
  }

  private fun createDrawableFactory(): DefaultBitmapAnimationDrawableFactory {
    val cachingStrategySupplier: Supplier<Int> = Supplier {
      DefaultBitmapAnimationDrawableFactory.CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING
    }

    val finalSerialExecutorService =
        serialExecutorService ?: DefaultSerialExecutorService(executorSupplier.forDecode())

    val numberOfFramesToPrepareSupplier: Supplier<Int> = Supplier { NUMBER_OF_FRAMES_TO_PREPARE }

    val useDeepEquals = Suppliers.BOOLEAN_FALSE

    return DefaultBitmapAnimationDrawableFactory(
        getAnimatedDrawableBackendProvider(),
        UiThreadImmediateExecutorService.getInstance(),
        finalSerialExecutorService,
        RealtimeSinceBootClock.get(),
        platformBitmapFactory,
        backingCache,
        cachingStrategySupplier,
        numberOfFramesToPrepareSupplier,
        useDeepEquals,
        Suppliers.of(useBufferLoaderStrategy),
        Suppliers.of(downscaleFrameToDrawableDimensions),
        Suppliers.of(animationFpsLimit),
        Suppliers.of(bufferLengthMilliseconds),
        null,
        enableBufferFrameLoaderFix,
    )
  }

  private fun getAnimatedDrawableUtil(): AnimatedDrawableUtil {
    return animatedDrawableUtil ?: AnimatedDrawableUtil().also { animatedDrawableUtil = it }
  }

  private fun getAnimatedDrawableBackendProvider(): AnimatedDrawableBackendProvider {
    if (animatedDrawableBackendProvider == null) {
      animatedDrawableBackendProvider =
          object : AnimatedDrawableBackendProvider {
            override fun get(
                animatedImageResult: AnimatedImageResult,
                bounds: Rect?,
            ): AnimatedDrawableBackend {
              return AnimatedDrawableBackendImpl(
                  getAnimatedDrawableUtil(),
                  animatedImageResult,
                  bounds,
                  downscaleFrameToDrawableDimensions,
              )
            }
          }
    }
    return animatedDrawableBackendProvider as AnimatedDrawableBackendProvider
  }

  companion object {
    private const val NUMBER_OF_FRAMES_TO_PREPARE = 3
  }
}
