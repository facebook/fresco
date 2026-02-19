/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.factory

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.cache.common.CacheKey
import com.facebook.common.internal.Preconditions
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.common.time.MonotonicClock
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.fresco.animation.bitmap.cache.AnimationFrameCacheKey
import com.facebook.fresco.animation.bitmap.cache.FrescoFrameCache
import com.facebook.fresco.animation.bitmap.cache.KeepLastFrameCache
import com.facebook.fresco.animation.bitmap.cache.NoOpCache
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparationStrategy
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparer
import com.facebook.fresco.animation.bitmap.preparation.DefaultBitmapFramePreparer
import com.facebook.fresco.animation.bitmap.preparation.FixedNumberBitmapFramePreparationStrategy
import com.facebook.fresco.animation.bitmap.preparation.FrameLoaderStrategy
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameLoaderFactory
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.ZeroFrameDimensionsListener
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendAnimationInformation
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendFrameRenderer
import com.facebook.fresco.animation.drawable.AnimatedDrawable2
import com.facebook.fresco.animation.drawable.KAnimatedDrawable2
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.fresco.vito.core.AnimatedImagePerfLoggingListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider
import com.facebook.imagepipeline.animated.impl.AnimatedFrameCache
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.cache.CountingMemoryCache
import com.facebook.imagepipeline.drawable.DrawableFactory
import com.facebook.imagepipeline.image.CloseableAnimatedImage
import com.facebook.imagepipeline.image.CloseableImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService

/** Animation factory for [AnimatedDrawable2]. */
class DefaultBitmapAnimationDrawableFactory(
    private val animatedDrawableBackendProvider: AnimatedDrawableBackendProvider,
    private val scheduledExecutorServiceForUiThread: ScheduledExecutorService,
    private val executorServiceForFramePreparing: ExecutorService,
    private val monotonicClock: MonotonicClock,
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val backingCache: CountingMemoryCache<CacheKey, CloseableImage>?,
    private val cachingStrategySupplier: Supplier<Int>,
    private val numberOfFramesToPrepareSupplier: Supplier<Int>,
    private val useDeepEqualsForCacheKey: Supplier<Boolean>,
    private val useNewBitmapRender: Supplier<Boolean>,
    private val downscaleFrameToDrawableDimensions: Supplier<Boolean>,
    private val animationFpsLimit: Supplier<Int>,
    private val bufferLengthMilliseconds: Supplier<Int>,
    private val animatedImagePerfLoggingListener: AnimatedImagePerfLoggingListener? = null,
    private val enableBufferFrameLoaderFix: Boolean = false,
    private val zeroFrameDimensionsListener: ZeroFrameDimensionsListener? = null,
) : DrawableFactory, ImageOptionsDrawableFactory {

  // Change the value to true to use KAnimatedDrawable2.kt
  private val useRendererAnimatedDrawable: Supplier<Boolean> = Suppliers.BOOLEAN_FALSE

  override fun supportsImageType(image: CloseableImage): Boolean {
    return image is CloseableAnimatedImage
  }

  override fun createDrawable(image: CloseableImage): Drawable? {
    if (!supportsImageType(image)) {
      return null
    }
    val closeable = image as CloseableAnimatedImage
    val animatedImage = closeable.image

    val animationBackend =
        createAnimationBackend(
            Preconditions.checkNotNull(closeable.imageResult),
            animatedImage?.animatedBitmapConfig,
            null,
        )
    return if (useRendererAnimatedDrawable.get()) {
      KAnimatedDrawable2(animationBackend)
    } else {
      AnimatedDrawable2(animationBackend)
    }
  }

  override fun createDrawable(
      resources: Resources,
      closeableImage: CloseableImage,
      imageOptions: ImageOptions,
  ): Drawable? {
    if (!supportsImageType(closeableImage)) {
      return null
    }
    val closeable = closeableImage as CloseableAnimatedImage
    val animatedImage = closeable.image

    // Log drawable creation start
    val imageId =
        closeable.imageResult?.source ?: "unknown_${System.identityHashCode(closeableImage)}"
    val startTime = System.nanoTime()
    animatedImagePerfLoggingListener?.onDrawableCreationStart(imageId, startTime)

    val animationBackend: AnimationBackend =
        runCatching {
              createAnimationBackend(
                  Preconditions.checkNotNull(closeable.imageResult),
                  animatedImage?.animatedBitmapConfig,
                  imageOptions,
              )
            }
            .getOrElse { e ->
              when (e) {
                is NullPointerException -> {
                  val uri = closeableImage.getExtra<Any?>(HasExtraData.KEY_URI_SOURCE)
                  if (uri != null) {
                    throw NullPointerException("${e.message} uri=${uri}")
                  } else {
                    throw e
                  }
                }
                else -> throw e
              }
            }

    val drawable =
        if (useRendererAnimatedDrawable.get()) {
          KAnimatedDrawable2(animationBackend)
        } else {
          AnimatedDrawable2(animationBackend)
        }

    // Log drawable creation success
    val endTime = System.nanoTime()
    animatedImagePerfLoggingListener?.onDrawableCreationEnd(imageId, endTime, true)

    return drawable
  }

  /**
   * Creates an animation backend for the given animated image result. * * @param
   * animatedImageResult The animated image result to create a backend for
   *
   * @param animatedBitmapConfig Optional bitmap configuration for the animation
   * @param imageOptions Optional image options for customizing the animation
   * @return An animation backend for the given parameters
   */
  private fun createAnimationBackend(
      animatedImageResult: AnimatedImageResult,
      animatedBitmapConfig: Bitmap.Config?,
      imageOptions: ImageOptions?,
  ): AnimationBackend {
    val animatedDrawableBackend = createAnimatedDrawableBackend(animatedImageResult)
    val animationInfo = AnimatedDrawableBackendAnimationInformation(animatedDrawableBackend)

    val bitmapFrameCache = createBitmapFrameCache(animatedImageResult)
    val bitmapFrameRenderer =
        AnimatedDrawableBackendFrameRenderer(
            bitmapFrameCache,
            animatedDrawableBackend,
            useNewBitmapRender.get(),
        )

    val numberOfFramesToPrefetch = numberOfFramesToPrepareSupplier.get()
    var bitmapFramePreparationStrategy: BitmapFramePreparationStrategy? = null
    var bitmapFramePreparer: BitmapFramePreparer? = null
    if (numberOfFramesToPrefetch > 0) {
      bitmapFramePreparationStrategy =
          FixedNumberBitmapFramePreparationStrategy(numberOfFramesToPrefetch)
      bitmapFramePreparer = createBitmapFramePreparer(bitmapFrameRenderer, animatedBitmapConfig)
    }

    val roundingOptions = imageOptions?.roundingOptions

    val animatedOptions = imageOptions?.animatedOptions

    if (useNewBitmapRender.get()) {
      bitmapFramePreparationStrategy =
          FrameLoaderStrategy(
              animatedImageResult.source,
              animationInfo,
              bitmapFrameRenderer,
              FrameLoaderFactory(
                  platformBitmapFactory,
                  animationFpsLimit.get(),
                  bufferLengthMilliseconds.get(),
                  enableBufferFrameLoaderFix,
                  zeroFrameDimensionsListener,
              ),
              downscaleFrameToDrawableDimensions.get(),
          )
    }

    val bitmapAnimationBackend =
        BitmapAnimationBackend(
            platformBitmapFactory,
            bitmapFrameCache,
            animationInfo,
            bitmapFrameRenderer,
            useNewBitmapRender.get(),
            bitmapFramePreparationStrategy,
            bitmapFramePreparer,
            roundingOptions,
            animatedOptions,
        )

    // Set the animated image performance logging listener
    bitmapAnimationBackend.setAnimatedImagePerfLoggingListener(animatedImagePerfLoggingListener)

    return AnimationBackendDelegateWithInactivityCheck.createForBackend(
        bitmapAnimationBackend,
        monotonicClock,
        scheduledExecutorServiceForUiThread,
    )
  }

  private fun createBitmapFramePreparer(
      bitmapFrameRenderer: BitmapFrameRenderer,
      animatedBitmapConfig: Bitmap.Config?,
  ): BitmapFramePreparer {
    return DefaultBitmapFramePreparer(
        platformBitmapFactory,
        bitmapFrameRenderer,
        animatedBitmapConfig ?: Bitmap.Config.ARGB_8888,
        executorServiceForFramePreparing,
    )
  }

  private fun createAnimatedDrawableBackend(
      animatedImageResult: AnimatedImageResult
  ): AnimatedDrawableBackend {
    val animatedImage = animatedImageResult.image
    val initialBounds = Rect(0, 0, animatedImage.width, animatedImage.height)
    return animatedDrawableBackendProvider.get(animatedImageResult, initialBounds)
  }

  private fun createBitmapFrameCache(animatedImageResult: AnimatedImageResult): BitmapFrameCache {
    return when (cachingStrategySupplier.get()) {
      CACHING_STRATEGY_FRESCO_CACHE ->
          FrescoFrameCache(createAnimatedFrameCache(animatedImageResult), true)
      CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING ->
          FrescoFrameCache(createAnimatedFrameCache(animatedImageResult), false)
      CACHING_STRATEGY_KEEP_LAST_CACHE -> KeepLastFrameCache()
      CACHING_STRATEGY_NO_CACHE -> NoOpCache()
      else -> NoOpCache()
    }
  }

  private fun createAnimatedFrameCache(
      animatedImageResult: AnimatedImageResult
  ): AnimatedFrameCache {
    return AnimatedFrameCache(
        AnimationFrameCacheKey(animatedImageResult.hashCode(), useDeepEqualsForCacheKey.get()),
        backingCache ?: throw IllegalStateException("backingCache is null"),
    )
  }

  companion object {
    const val CACHING_STRATEGY_NO_CACHE = 0
    const val CACHING_STRATEGY_FRESCO_CACHE = 1
    const val CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING = 2
    const val CACHING_STRATEGY_KEEP_LAST_CACHE = 3
  }
}
