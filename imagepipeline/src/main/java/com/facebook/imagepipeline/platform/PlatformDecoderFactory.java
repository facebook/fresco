/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import android.os.Build;
import androidx.core.util.Pools;
import com.facebook.imagepipeline.core.NativeCodeSetup;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.memory.PoolFactory;
import java.lang.reflect.InvocationTargetException;

public class PlatformDecoderFactory {
  /**
   * Provide the implementation of the PlatformDecoder for the current platform using the provided
   * PoolFactory
   *
   * @param poolFactory The PoolFactory
   * @return The PlatformDecoder implementation
   */
  public static PlatformDecoder buildPlatformDecoder(
      PoolFactory poolFactory, boolean gingerbreadDecoderEnabled) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      int maxNumThreads = poolFactory.getFlexByteArrayPoolMaxNumThreads();
      return new OreoDecoder(
          poolFactory.getBitmapPool(), maxNumThreads, new Pools.SynchronizedPool<>(maxNumThreads));
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        || !NativeCodeSetup.getUseNativeCode()) {
      int maxNumThreads = poolFactory.getFlexByteArrayPoolMaxNumThreads();
      return new ArtDecoder(
          poolFactory.getBitmapPool(), maxNumThreads, new Pools.SynchronizedPool<>(maxNumThreads));
    } else {
      try {
        if (gingerbreadDecoderEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
          Class<?> clazz =
              Class.forName("com.facebook.imagepipeline.platform.GingerbreadPurgeableDecoder");
          return (PlatformDecoder) clazz.getConstructor().newInstance();
        } else {
          Class<?> clazz =
              Class.forName("com.facebook.imagepipeline.platform.KitKatPurgeableDecoder");
          return (PlatformDecoder)
              clazz
                  .getConstructor(FlexByteArrayPool.class)
                  .newInstance(poolFactory.getFlexByteArrayPool());
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Wrong Native code setup, reflection failed.", e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Wrong Native code setup, reflection failed.", e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("Wrong Native code setup, reflection failed.", e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException("Wrong Native code setup, reflection failed.", e);
      } catch (InstantiationException e) {
        throw new RuntimeException("Wrong Native code setup, reflection failed.", e);
      }
    }
  }
}
