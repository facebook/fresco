/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.factory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.executors.DefaultSerialExecutorService;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Supplier;
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
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * {@link AnimatedFactory} for animations v2 that creates {@link AnimatedDrawable2} drawables.
 */
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

  @DoNotStrip
  public AnimatedFactoryV2Impl(
      PlatformBitmapFactory platformBitmapFactory,
      ExecutorSupplier executorSupplier,
      CountingMemoryCache<CacheKey, CloseableImage> backingCache,
      boolean downscaleFrameToDrawableDimensions) {
    mPlatformBitmapFactory = platformBitmapFactory;
    mExecutorSupplier = executorSupplier;
    mBackingCache = backingCache;
    mDownscaleFrameToDrawableDimensions = downscaleFrameToDrawableDimensions;
  }

  @Nullable
  @Override
  public DrawableFactory getAnimatedDrawableFactory(Context context) {
    if (mAnimatedDrawableFactory == null) {
      mAnimatedDrawableFactory = createDrawableFactory();
    }
    return mAnimatedDrawableFactory;
  }

  @Override
  public ImageDecoder getGifDecoder(final Bitmap.Config bitmapConfig) {
    return new ImageDecoder() {
      @Override
      public CloseableImage decode(
          EncodedImage encodedImage,
          int length,
          QualityInfo qualityInfo,
          ImageDecodeOptions options) {
        return getAnimatedImageFactory().decodeGif(encodedImage, options, bitmapConfig);
      }
    };
  }

  @Override
  public ImageDecoder getWebPDecoder(final Bitmap.Config bitmapConfig) {
    return new ImageDecoder() {
      @Override
      public CloseableImage decode(
          EncodedImage encodedImage,
          int length,
          QualityInfo qualityInfo,
          ImageDecodeOptions options) {
        return getAnimatedImageFactory().decodeWebP(encodedImage, options, bitmapConfig);
      }
    };
  }

  private ExperimentalBitmapAnimationDrawableFactory createDrawableFactory() {
    Supplier<Integer> cachingStrategySupplier = new Supplier<Integer>() {
      @Override
      public Integer get() {
        return ExperimentalBitmapAnimationDrawableFactory.CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING;
      }
    };

    final SerialExecutorService serialExecutorServiceForFramePreparing =
        new DefaultSerialExecutorService(mExecutorSupplier.forDecode());

    Supplier<Integer> numberOfFramesToPrepareSupplier = new Supplier<Integer>() {
      @Override
      public Integer get() {
        return NUMBER_OF_FRAMES_TO_PREPARE;
      }
    };

    return new ExperimentalBitmapAnimationDrawableFactory(
        getAnimatedDrawableBackendProvider(),
        UiThreadImmediateExecutorService.getInstance(),
        serialExecutorServiceForFramePreparing,
        RealtimeSinceBootClock.get(),
        mPlatformBitmapFactory,
        mBackingCache,
        cachingStrategySupplier,
        numberOfFramesToPrepareSupplier);
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
                AnimatedImageResult animatedImageResult, Rect bounds) {
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
          public AnimatedDrawableBackend get(AnimatedImageResult imageResult, Rect bounds) {
            return new AnimatedDrawableBackendImpl(
                getAnimatedDrawableUtil(),
                imageResult,
                bounds,
                mDownscaleFrameToDrawableDimensions);
          }
        };
    return new AnimatedImageFactoryImpl(animatedDrawableBackendProvider, mPlatformBitmapFactory);
  }
}
