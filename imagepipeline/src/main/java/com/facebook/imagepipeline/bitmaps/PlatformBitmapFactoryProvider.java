/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import android.os.Build;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.platform.PlatformDecoder;

public class PlatformBitmapFactoryProvider {

  /**
   * Provide the implementation of the PlatformBitmapFactory for the current platform using the
   * provided PoolFactory
   *
   * @param poolFactory The PoolFactory
   * @param platformDecoder The PlatformDecoder
   * @return The PlatformBitmapFactory implementation
   */
  public static PlatformBitmapFactory buildPlatformBitmapFactory(
      PoolFactory poolFactory, PlatformDecoder platformDecoder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new ArtBitmapFactory(poolFactory.getBitmapPool());
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      return new HoneycombBitmapFactory(
          new EmptyJpegGenerator(poolFactory.getPooledByteBufferFactory()), platformDecoder);
    } else {
      return new GingerbreadBitmapFactory();
    }
  }
}
