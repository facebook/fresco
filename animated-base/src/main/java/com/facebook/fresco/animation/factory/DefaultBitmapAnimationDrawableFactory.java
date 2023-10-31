/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.factory;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.time.MonotonicClock;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck;
import com.facebook.fresco.animation.backend.AnimationInformation;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer;
import com.facebook.fresco.animation.bitmap.cache.AnimationFrameCacheKey;
import com.facebook.fresco.animation.bitmap.cache.FrescoFrameCache;
import com.facebook.fresco.animation.bitmap.cache.KeepLastFrameCache;
import com.facebook.fresco.animation.bitmap.cache.NoOpCache;
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparationStrategy;
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparer;
import com.facebook.fresco.animation.bitmap.preparation.DefaultBitmapFramePreparer;
import com.facebook.fresco.animation.bitmap.preparation.FixedNumberBitmapFramePreparationStrategy;
import com.facebook.fresco.animation.bitmap.preparation.FrameLoaderStrategy;
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameLoaderFactory;
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendAnimationInformation;
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendFrameRenderer;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.fresco.animation.drawable.KAnimatedDrawable2;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedFrameCache;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;

/** Animation factory for {@link AnimatedDrawable2}. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultBitmapAnimationDrawableFactory
    implements DrawableFactory, ImageOptionsDrawableFactory {

  public static final int CACHING_STRATEGY_NO_CACHE = 0;
  public static final int CACHING_STRATEGY_FRESCO_CACHE = 1;
  public static final int CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING = 2;
  public static final int CACHING_STRATEGY_KEEP_LAST_CACHE = 3;

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
  private final ExecutorService mExecutorServiceForFramePreparing;

  private final MonotonicClock mMonotonicClock;
  private final PlatformBitmapFactory mPlatformBitmapFactory;
  private final CountingMemoryCache<CacheKey, CloseableImage> mBackingCache;
  private final Supplier<Integer> mCachingStrategySupplier;
  private final Supplier<Integer> mNumberOfFramesToPrepareSupplier;
  private final Supplier<Boolean> mUseDeepEqualsForCacheKey;
  private final Supplier<Boolean> mUseNewBitmapRender;
  private final Supplier<Boolean> mDownscaleFrameToDrawableDimensions;
  private final Supplier<Integer> mAnimationFpsLimit;

  // Change the value to true to use KAnimatedDrawable2.kt
  private final Supplier<Boolean> useRendererAnimatedDrawable = Suppliers.BOOLEAN_FALSE;

  public DefaultBitmapAnimationDrawableFactory(
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
      ScheduledExecutorService scheduledExecutorServiceForUiThread,
      ExecutorService executorServiceForFramePreparing,
      MonotonicClock monotonicClock,
      PlatformBitmapFactory platformBitmapFactory,
      CountingMemoryCache<CacheKey, CloseableImage> backingCache,
      Supplier<Integer> cachingStrategySupplier,
      Supplier<Integer> numberOfFramesToPrepareSupplier,
      Supplier<Boolean> useDeepEqualsForCacheKey,
      Supplier<Boolean> useNewBitmapRender,
      Supplier<Boolean> downscaleFrameToDrawableDimensions,
      Supplier<Integer> animationFpsLimit) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
    mScheduledExecutorServiceForUiThread = scheduledExecutorServiceForUiThread;
    mExecutorServiceForFramePreparing = executorServiceForFramePreparing;
    mMonotonicClock = monotonicClock;
    mPlatformBitmapFactory = platformBitmapFactory;
    mBackingCache = backingCache;
    mCachingStrategySupplier = cachingStrategySupplier;
    mNumberOfFramesToPrepareSupplier = numberOfFramesToPrepareSupplier;
    mUseDeepEqualsForCacheKey = useDeepEqualsForCacheKey;
    mUseNewBitmapRender = useNewBitmapRender;
    mAnimationFpsLimit = animationFpsLimit;
    mDownscaleFrameToDrawableDimensions = downscaleFrameToDrawableDimensions;
  }

  @Override
  public boolean supportsImageType(CloseableImage image) {
    return image instanceof CloseableAnimatedImage;
  }

  @Override
  public Drawable createDrawable(CloseableImage image) {
    CloseableAnimatedImage closeable = ((CloseableAnimatedImage) image);
    AnimatedImage animatedImage = closeable.getImage();
    AnimationBackend animationBackend =
        createAnimationBackend(
            Preconditions.checkNotNull(closeable.getImageResult()),
            animatedImage != null ? animatedImage.getAnimatedBitmapConfig() : null,
            null);
    if (useRendererAnimatedDrawable.get()) {
      return new KAnimatedDrawable2(animationBackend);
    } else {
      return new AnimatedDrawable2(animationBackend);
    }
  }

  @Override
  public Drawable createDrawable(
      Resources resources, CloseableImage closeableImage, ImageOptions imageOptions) {
    CloseableAnimatedImage closeable = ((CloseableAnimatedImage) closeableImage);
    AnimatedImage animatedImage = closeable.getImage();
    AnimationBackend animationBackend =
        createAnimationBackend(
            Preconditions.checkNotNull(closeable.getImageResult()),
            animatedImage != null ? animatedImage.getAnimatedBitmapConfig() : null,
            imageOptions);
    if (useRendererAnimatedDrawable.get()) {
      return new KAnimatedDrawable2(animationBackend);
    } else {
      return new AnimatedDrawable2(animationBackend);
    }
  }

  private AnimationBackend createAnimationBackend(
      AnimatedImageResult animatedImageResult,
      @Nullable Bitmap.Config animatedBitmapConfig,
      @Nullable ImageOptions imageOptions) {
    AnimatedDrawableBackend animatedDrawableBackend =
        createAnimatedDrawableBackend(animatedImageResult);
    AnimationInformation animationInfo =
        new AnimatedDrawableBackendAnimationInformation(animatedDrawableBackend);

    BitmapFrameCache bitmapFrameCache = createBitmapFrameCache(animatedImageResult);
    BitmapFrameRenderer bitmapFrameRenderer =
        new AnimatedDrawableBackendFrameRenderer(
            bitmapFrameCache, animatedDrawableBackend, mUseNewBitmapRender.get());

    int numberOfFramesToPrefetch = mNumberOfFramesToPrepareSupplier.get();
    BitmapFramePreparationStrategy bitmapFramePreparationStrategy = null;
    BitmapFramePreparer bitmapFramePreparer = null;
    if (numberOfFramesToPrefetch > 0) {
      bitmapFramePreparationStrategy =
          new FixedNumberBitmapFramePreparationStrategy(numberOfFramesToPrefetch);
      bitmapFramePreparer = createBitmapFramePreparer(bitmapFrameRenderer, animatedBitmapConfig);
    }

    RoundingOptions roundingOptions = null;
    if (imageOptions != null) {
      roundingOptions = imageOptions.getRoundingOptions();
    }

    if (mUseNewBitmapRender.get()) {
      bitmapFramePreparationStrategy =
          new FrameLoaderStrategy(
              animatedImageResult.getSource(),
              animationInfo,
              bitmapFrameRenderer,
              new FrameLoaderFactory(mPlatformBitmapFactory, mAnimationFpsLimit.get()),
              mDownscaleFrameToDrawableDimensions.get());
    }

    BitmapAnimationBackend bitmapAnimationBackend =
        new BitmapAnimationBackend(
            mPlatformBitmapFactory,
            bitmapFrameCache,
            animationInfo,
            bitmapFrameRenderer,
            mUseNewBitmapRender.get(),
            bitmapFramePreparationStrategy,
            bitmapFramePreparer,
            roundingOptions);

    return AnimationBackendDelegateWithInactivityCheck.createForBackend(
        bitmapAnimationBackend, mMonotonicClock, mScheduledExecutorServiceForUiThread);
  }

  private BitmapFramePreparer createBitmapFramePreparer(
      BitmapFrameRenderer bitmapFrameRenderer, @Nullable Bitmap.Config animatedBitmapConig) {
    return new DefaultBitmapFramePreparer(
        mPlatformBitmapFactory,
        bitmapFrameRenderer,
        animatedBitmapConig != null ? animatedBitmapConig : Bitmap.Config.ARGB_8888,
        mExecutorServiceForFramePreparing);
  }

  private AnimatedDrawableBackend createAnimatedDrawableBackend(
      AnimatedImageResult animatedImageResult) {
    AnimatedImage animatedImage = animatedImageResult.getImage();
    Rect initialBounds = new Rect(0, 0, animatedImage.getWidth(), animatedImage.getHeight());
    return mAnimatedDrawableBackendProvider.get(animatedImageResult, initialBounds);
  }

  private BitmapFrameCache createBitmapFrameCache(AnimatedImageResult animatedImageResult) {
    switch (mCachingStrategySupplier.get()) {
      case CACHING_STRATEGY_FRESCO_CACHE:
        return new FrescoFrameCache(createAnimatedFrameCache(animatedImageResult), true);
      case CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING:
        return new FrescoFrameCache(createAnimatedFrameCache(animatedImageResult), false);
      case CACHING_STRATEGY_KEEP_LAST_CACHE:
        return new KeepLastFrameCache();
      case CACHING_STRATEGY_NO_CACHE:
      default:
        return new NoOpCache();
    }
  }

  private AnimatedFrameCache createAnimatedFrameCache(
      final AnimatedImageResult animatedImageResult) {
    return new AnimatedFrameCache(
        new AnimationFrameCacheKey(animatedImageResult.hashCode(), mUseDeepEqualsForCacheKey.get()),
        mBackingCache);
  }
}
