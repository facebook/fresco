/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.animated.factory;

import java.lang.reflect.Constructor;

import android.content.Context;

import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.core.ExecutorSupplier;

public class AnimatedFactoryProvider {

  private static boolean sImplLoaded;

  private static AnimatedFactory sImpl = null;

  public static AnimatedFactory getAnimatedFactory(
      Context context,
      PlatformBitmapFactory platformBitmapFactory,
      ExecutorSupplier executorSupplier) {
    if (!sImplLoaded) {
      try {
        final Class<?> clazz =
            Class.forName("com.facebook.imagepipeline.animated.factory.AnimatedFactoryImplSupport");
        final Constructor<?> constructor = clazz.getConstructor(
            Context.class,
            PlatformBitmapFactory.class,
            ExecutorSupplier.class);
        sImpl = (AnimatedFactory) constructor.newInstance(
            context,
            platformBitmapFactory,
            executorSupplier);
      } catch (Throwable e) {
        // Head in the sand
      }
      if (sImpl != null) {
        sImplLoaded = true;
        return sImpl;
      }
      try {
        final Class<?> clazz =
            Class.forName("com.facebook.imagepipeline.animated.factory.AnimatedFactoryImpl");
        final Constructor<?> constructor = clazz.getConstructor(
            Context.class,
            PlatformBitmapFactory.class,
            ExecutorSupplier.class);
        sImpl = (AnimatedFactory) constructor.newInstance(
            context,
            platformBitmapFactory,
            executorSupplier);
      } catch (Throwable e) {
        // Head in the sand
      }
      sImplLoaded = true;
    }
    return sImpl;
  }

}
