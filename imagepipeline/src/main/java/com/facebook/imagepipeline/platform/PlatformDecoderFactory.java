/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.platform;

import android.os.Build;
import androidx.core.util.Pools;
import com.facebook.imagepipeline.memory.PoolFactory;

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
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      int maxNumThreads = poolFactory.getFlexByteArrayPoolMaxNumThreads();
      return new ArtDecoder(
          poolFactory.getBitmapPool(), maxNumThreads, new Pools.SynchronizedPool<>(maxNumThreads));
    } else {
      if (gingerbreadDecoderEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
        return new GingerbreadPurgeableDecoder();
      } else {
        return new KitKatPurgeableDecoder(poolFactory.getFlexByteArrayPool());
      }
    }
  }
}
