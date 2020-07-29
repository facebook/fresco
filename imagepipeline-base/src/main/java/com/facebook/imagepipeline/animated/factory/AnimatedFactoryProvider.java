/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.core.ExecutorSupplier;
import com.facebook.imagepipeline.image.CloseableImage;
import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;

public class AnimatedFactoryProvider {

  private static boolean sImplLoaded;

  private static AnimatedFactory sImpl = null;

  public static AnimatedFactory getAnimatedFactory(
      PlatformBitmapFactory platformBitmapFactory,
      ExecutorSupplier executorSupplier,
      CountingMemoryCache<CacheKey, CloseableImage> backingCache,
      boolean downscaleFrameToDrawableDimensions,
      ExecutorService serialExecutorService) {
    if (!sImplLoaded) {
      try {
        final Class<?> clazz =
            Class.forName("com.facebook.fresco.animation.factory.AnimatedFactoryV2Impl");
        final Constructor<?> constructor =
            clazz.getConstructor(
                PlatformBitmapFactory.class,
                ExecutorSupplier.class,
                CountingMemoryCache.class,
                Boolean.TYPE,
                SerialExecutorService.class);
        sImpl =
            (AnimatedFactory)
                constructor.newInstance(
                    platformBitmapFactory,
                    executorSupplier,
                    backingCache,
                    downscaleFrameToDrawableDimensions,
                    serialExecutorService);
      } catch (Throwable e) {
        // Head in the sand
      }
      if (sImpl != null) {
        sImplLoaded = true;
      }
    }
    return sImpl;
  }
}
