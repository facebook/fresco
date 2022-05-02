/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import android.os.Build;
import androidx.core.util.Pools;
import com.facebook.common.memory.DecodeBufferHelper;
import com.facebook.imagepipeline.core.NativeCodeSetup;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class PlatformDecoderFactory {
  /**
   * Provide the implementation of the PlatformDecoder for the current platform using the provided
   * PoolFactory
   *
   * @param poolFactory The PoolFactory
   * @return The PlatformDecoder implementation
   */
  public static PlatformDecoder buildPlatformDecoder(
      PoolFactory poolFactory, boolean gingerbreadDecoderEnabled, boolean useDecodeBufferHelper) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new OreoDecoder(
          poolFactory.getBitmapPool(), createPool(poolFactory, useDecodeBufferHelper));
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        || !NativeCodeSetup.getUseNativeCode()) {
      return new ArtDecoder(
          poolFactory.getBitmapPool(), createPool(poolFactory, useDecodeBufferHelper));
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

  public static PlatformDecoder buildPlatformDecoder(
      PoolFactory poolFactory, boolean gingerbreadDecoderEnabled) {
    return buildPlatformDecoder(poolFactory, gingerbreadDecoderEnabled, false);
  }

  @NotNull
  public static Pools.Pool<ByteBuffer> createPool(
      PoolFactory poolFactory, boolean useDecodeBufferHelper) {
    if (useDecodeBufferHelper) {
      return DecodeBufferHelper.INSTANCE;
    }
    final int maxNumThreads = poolFactory.getFlexByteArrayPoolMaxNumThreads();
    final Pools.Pool<ByteBuffer> pool = new Pools.SynchronizedPool<>(maxNumThreads);
    for (int i = 0; i < maxNumThreads; i++) {
      pool.release(ByteBuffer.allocate(DecodeBufferHelper.getRecommendedDecodeBufferSize()));
    }
    return pool;
  }
}
