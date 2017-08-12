/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.animated.factory;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.core.ExecutorSupplier;
import com.facebook.imagepipeline.image.CloseableImage;
import java.lang.reflect.Constructor;

public class AnimatedFactoryProvider {

  private static boolean sImplLoaded;

  private static AnimatedFactory sImpl = null;

  public static AnimatedFactory getAnimatedFactory(
      PlatformBitmapFactory platformBitmapFactory,
      ExecutorSupplier executorSupplier,
      CountingMemoryCache<CacheKey, CloseableImage> backingCache) {
    if (!sImplLoaded) {
      try {
        final Class<?> clazz =
            Class.forName("com.facebook.fresco.animation.factory.AnimatedFactoryV2Impl");
        final Constructor<?> constructor = clazz.getConstructor(
            PlatformBitmapFactory.class,
            ExecutorSupplier.class,
            CountingMemoryCache.class);
        sImpl = (AnimatedFactory) constructor.newInstance(
            platformBitmapFactory,
            executorSupplier,
            backingCache);
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
