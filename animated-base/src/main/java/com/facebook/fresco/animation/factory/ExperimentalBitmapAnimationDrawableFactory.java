/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.factory;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.common.time.MonotonicClock;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer;
import com.facebook.fresco.animation.bitmap.cache.FrescoFrameCache;
import com.facebook.fresco.animation.bitmap.cache.KeepLastFrameCache;
import com.facebook.fresco.animation.bitmap.cache.NoOpCache;
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparationStrategy;
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparer;
import com.facebook.fresco.animation.bitmap.preparation.DefaultBitmapFramePreparer;
import com.facebook.fresco.animation.bitmap.preparation.DropFramePreparationStrategy;
import com.facebook.fresco.animation.bitmap.preparation.FixedNumberBitmapFramePreparationStrategy;
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendAnimationInformation;
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendFrameRenderer;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Animation factory for {@link AnimatedDrawable2}.
 *
 */
public class ExperimentalBitmapAnimationDrawableFactory implements DrawableFactory {

  public static final int CACHING_STRATEGY_NO_CACHE = 0;
  public static final int CACHING_STRATEGY_FRESCO_CACHE = 1;
  public static final int CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING = 2;
  public static final int CACHING_STRATEGY_KEEP_LAST_CACHE = 3;

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
  private final ExecutorService mExecutorServiceForFramePreparing;
  private final ExecutorService mDropServiceForFramePreparing;
  private final MonotonicClock mMonotonicClock;
  private final PlatformBitmapFactory mPlatformBitmapFactory;
  private final CountingMemoryCache<CacheKey, CloseableImage> mBackingCache;
  private final CountingMemoryCache<CacheKey, CloseableImage> mOtherFrameCache;
  private final Supplier<Integer> mCachingStrategySupplier;
  private final Supplier<Integer> mNumberOfFramesToPrepareSupplier;

  public ExperimentalBitmapAnimationDrawableFactory(
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
      ScheduledExecutorService scheduledExecutorServiceForUiThread,
      ExecutorService executorServiceForFramePreparing,
      MonotonicClock monotonicClock,
      PlatformBitmapFactory platformBitmapFactory,
      CountingMemoryCache<CacheKey, CloseableImage> backingCache,
      Supplier<Integer> cachingStrategySupplier,
      Supplier<Integer> numberOfFramesToPrepareSupplier,
      CountingMemoryCache<CacheKey, CloseableImage> otherCache,
      ExecutorService dropServiceForFramePreparing) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
    mScheduledExecutorServiceForUiThread = scheduledExecutorServiceForUiThread;
    mExecutorServiceForFramePreparing = executorServiceForFramePreparing;
    mMonotonicClock = monotonicClock;
    mPlatformBitmapFactory = platformBitmapFactory;
    mBackingCache = backingCache;
    mCachingStrategySupplier = cachingStrategySupplier;
    mNumberOfFramesToPrepareSupplier = numberOfFramesToPrepareSupplier;
    mOtherFrameCache = otherCache;
    mDropServiceForFramePreparing = dropServiceForFramePreparing;
  }

  @Override
  public boolean supportsImageType(CloseableImage image) {
    return image instanceof CloseableAnimatedImage;
  }

  @Override
  public AnimatedDrawable2 createDrawable(CloseableImage image) {
    CloseableAnimatedImage animatedImage = (CloseableAnimatedImage) image;
    AnimatedImageResult result = animatedImage.getImageResult();
    return new AnimatedDrawable2(
        createAnimationBackend(result),
            result.getEnableDropFrame());
  }

  private AnimationBackend createAnimationBackend(AnimatedImageResult animatedImageResult) {
    AnimatedDrawableBackend animatedDrawableBackend =
        createAnimatedDrawableBackend(animatedImageResult);

    BitmapFrameCache bitmapFrameCache = createBitmapFrameCache(animatedImageResult);
    BitmapFrameRenderer bitmapFrameRenderer =
        new AnimatedDrawableBackendFrameRenderer(bitmapFrameCache, animatedDrawableBackend);

    int numberOfFramesToPrefetch = mNumberOfFramesToPrepareSupplier.get();
    BitmapFramePreparationStrategy bitmapFramePreparationStrategy = null;
    BitmapFramePreparer bitmapFramePreparer = null;
    if (numberOfFramesToPrefetch > 0) {
      if (animatedImageResult.getEnableDropFrame()) {
        bitmapFramePreparationStrategy =
                new DropFramePreparationStrategy();
      } else {
        bitmapFramePreparationStrategy =
                new FixedNumberBitmapFramePreparationStrategy(numberOfFramesToPrefetch);
        }
      bitmapFramePreparer = createBitmapFramePreparer(bitmapFrameRenderer);
    }

    BitmapAnimationBackend bitmapAnimationBackend = new BitmapAnimationBackend(
        mPlatformBitmapFactory,
        bitmapFrameCache,
        new AnimatedDrawableBackendAnimationInformation(animatedDrawableBackend),
        bitmapFrameRenderer,
        bitmapFramePreparationStrategy,
        bitmapFramePreparer,
        animatedImageResult.getEnableDropFrame());

    return AnimationBackendDelegateWithInactivityCheck.createForBackend(
        bitmapAnimationBackend,
        mMonotonicClock,
        mScheduledExecutorServiceForUiThread);
  }

  private BitmapFramePreparer createBitmapFramePreparer(BitmapFrameRenderer bitmapFrameRenderer) {
    return new DefaultBitmapFramePreparer(
        mPlatformBitmapFactory,
        bitmapFrameRenderer,
        Bitmap.Config.ARGB_8888,
        mExecutorServiceForFramePreparing,
        mDropServiceForFramePreparing);
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
        return new FrescoFrameCache(createAnimatedFrameCache(animatedImageResult),
                true, createOtherFrameCache(animatedImageResult));
      case CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING:
        return new FrescoFrameCache(createAnimatedFrameCache(animatedImageResult),
                false, createOtherFrameCache(animatedImageResult));
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
        new AnimationFrameCacheKey(animatedImageResult.hashCode()),
        mBackingCache);
  }

  private AnimatedFrameCache createOtherFrameCache(
          final AnimatedImageResult animatedImageResult) {
    if (!animatedImageResult.getEnableDropFrame() || mOtherFrameCache == null) {
      return null;
    }
    return new AnimatedFrameCache(
            new AnimationFrameCacheKey(animatedImageResult.hashCode()),
            mOtherFrameCache);
  }

  public static class AnimationFrameCacheKey implements CacheKey {

    private static final String URI_PREFIX = "anim://";

    private final String mAnimationUriString;

    public AnimationFrameCacheKey(int imageId) {
      mAnimationUriString = URI_PREFIX + imageId;
    }

    @Override
    public boolean containsUri(Uri uri) {
      return uri.toString().startsWith(mAnimationUriString);
    }

    @Override
    public String getUriString() {
      return mAnimationUriString;
    }

    @Override
    public int hashCode() {
      return mAnimationUriString.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof AnimationFrameCacheKey) {
        return ((AnimationFrameCacheKey) obj).mAnimationUriString.equals(mAnimationUriString);
      }
      return false;
    }
  }
}
