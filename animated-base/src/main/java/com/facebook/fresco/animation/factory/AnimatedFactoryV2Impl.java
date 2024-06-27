/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.factory;

import android.content.Context;
import android.graphics.Rect;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.executors.DefaultSerialExecutorService;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.factory.AnimatedFactory;
import com.facebook.imagepipeline.animated.factory.AnimatedImageFactory;
import com.facebook.imagepipeline.animated.factory.AnimatedImageFactoryImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.core.ExecutorSupplier;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/** {@link AnimatedFactory} for animations v2 that creates {@link AnimatedDrawable2} drawables. */
@Nullsafe(Nullsafe.Mode.LOCAL)
@NotThreadSafe
@DoNotStrip
public class AnimatedFactoryV2Impl implements AnimatedFactory {

  private static final int NUMBER_OF_FRAMES_TO_PREPARE = 3;
  private final PlatformBitmapFactory mPlatformBitmapFactory;
  private final ExecutorSupplier mExecutorSupplier;
  private final CountingMemoryCache<CacheKey, CloseableImage> mBackingCache;
  private final boolean mDownscaleFrameToDrawableDimensions;

  private @Nullable AnimatedImageFactory mAnimatedImageFactory;
  private @Nullable AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private @Nullable AnimatedDrawableUtil mAnimatedDrawableUtil;
  private @Nullable DrawableFactory mAnimatedDrawableFactory;
  private @Nullable SerialExecutorService mSerialExecutorService;
  private int mAnimationFpsLimit;
  private final boolean mUseBufferLoaderStrategy;
  private int mBufferLengthMilliseconds;

  @DoNotStrip
  public AnimatedFactoryV2Impl(
      PlatformBitmapFactory platformBitmapFactory,
      ExecutorSupplier executorSupplier,
      CountingMemoryCache<CacheKey, CloseableImage> backingCache,
      boolean downscaleFrameToDrawableDimensions,
      boolean useBufferLoaderStrategy,
      int animationFpsLimit,
      int bufferLengthMilliseconds,
      @Nullable SerialExecutorService serialExecutorServiceForFramePreparing) {
    mPlatformBitmapFactory = platformBitmapFactory;
    mExecutorSupplier = executorSupplier;
    mBackingCache = backingCache;
    mAnimationFpsLimit = animationFpsLimit;
    mUseBufferLoaderStrategy = useBufferLoaderStrategy;
    mDownscaleFrameToDrawableDimensions = downscaleFrameToDrawableDimensions;
    mSerialExecutorService = serialExecutorServiceForFramePreparing;
    mBufferLengthMilliseconds = bufferLengthMilliseconds;
  }

  @Nullable
  @Override
  public DrawableFactory getAnimatedDrawableFactory(@Nullable Context context) {
    if (mAnimatedDrawableFactory == null) {
      mAnimatedDrawableFactory = createDrawableFactory();
    }
    return mAnimatedDrawableFactory;
  }

  @Override
  public ImageDecoder getGifDecoder() {
    return new ImageDecoder() {
      @Override
      public @Nullable CloseableImage decode(
          EncodedImage encodedImage,
          int length,
          QualityInfo qualityInfo,
          ImageDecodeOptions options) {
        return getAnimatedImageFactory()
            .decodeGif(encodedImage, options, options.animatedBitmapConfig);
      }
    };
  }

  @Override
  public ImageDecoder getWebPDecoder() {
    return (encodedImage, length, qualityInfo, options) ->
        getAnimatedImageFactory().decodeWebP(encodedImage, options, options.animatedBitmapConfig);
  }

  private DefaultBitmapAnimationDrawableFactory createDrawableFactory() {
    Supplier<Integer> cachingStrategySupplier =
        () -> DefaultBitmapAnimationDrawableFactory.CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING;

    final SerialExecutorService serialExecutorServiceForFramePreparing =
        mSerialExecutorService == null
            ? new DefaultSerialExecutorService(mExecutorSupplier.forDecode())
            : mSerialExecutorService;

    Supplier<Integer> numberOfFramesToPrepareSupplier = () -> NUMBER_OF_FRAMES_TO_PREPARE;

    final Supplier<Boolean> useDeepEquals = Suppliers.BOOLEAN_FALSE;

    return new DefaultBitmapAnimationDrawableFactory(
        getAnimatedDrawableBackendProvider(),
        UiThreadImmediateExecutorService.getInstance(),
        serialExecutorServiceForFramePreparing,
        RealtimeSinceBootClock.get(),
        mPlatformBitmapFactory,
        mBackingCache,
        cachingStrategySupplier,
        numberOfFramesToPrepareSupplier,
        useDeepEquals,
        Suppliers.of(mUseBufferLoaderStrategy),
        Suppliers.of(mDownscaleFrameToDrawableDimensions),
        Suppliers.of(mAnimationFpsLimit),
        Suppliers.of(mBufferLengthMilliseconds));
  }

  private AnimatedDrawableUtil getAnimatedDrawableUtil() {
    if (mAnimatedDrawableUtil == null) {
      mAnimatedDrawableUtil = new AnimatedDrawableUtil();
    }
    return mAnimatedDrawableUtil;
  }

  private AnimatedImageFactory getAnimatedImageFactory() {
    if (mAnimatedImageFactory == null) {
      mAnimatedImageFactory = buildAnimatedImageFactory();
    }
    return mAnimatedImageFactory;
  }

  private AnimatedDrawableBackendProvider getAnimatedDrawableBackendProvider() {
    if (mAnimatedDrawableBackendProvider == null) {
      mAnimatedDrawableBackendProvider =
          new AnimatedDrawableBackendProvider() {
            @Override
            public AnimatedDrawableBackend get(
                AnimatedImageResult animatedImageResult, @Nullable Rect bounds) {
              return new AnimatedDrawableBackendImpl(
                  getAnimatedDrawableUtil(),
                  animatedImageResult,
                  bounds,
                  mDownscaleFrameToDrawableDimensions);
            }
          };
    }
    return mAnimatedDrawableBackendProvider;
  }

  private AnimatedImageFactory buildAnimatedImageFactory() {
    AnimatedDrawableBackendProvider animatedDrawableBackendProvider =
        new AnimatedDrawableBackendProvider() {
          @Override
          public AnimatedDrawableBackend get(
              AnimatedImageResult imageResult, @Nullable Rect bounds) {
            return new AnimatedDrawableBackendImpl(
                getAnimatedDrawableUtil(),
                imageResult,
                bounds,
                mDownscaleFrameToDrawableDimensions);
          }
        };
    return new AnimatedImageFactoryImpl(
        animatedDrawableBackendProvider, mPlatformBitmapFactory, mUseBufferLoaderStrategy);
  }
}
