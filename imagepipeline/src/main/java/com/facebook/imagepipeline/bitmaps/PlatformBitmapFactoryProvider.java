/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps;

import android.os.Build;
import com.facebook.imagepipeline.core.CloseableReferenceFactory;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
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
      PoolFactory poolFactory,
      PlatformDecoder platformDecoder,
      CloseableReferenceFactory closeableReferenceFactory) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new ArtBitmapFactory(poolFactory.getBitmapPool(), closeableReferenceFactory);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      return new HoneycombBitmapFactory(
          new EmptyJpegGenerator(poolFactory.getPooledByteBufferFactory()),
          platformDecoder,
          closeableReferenceFactory);
    } else {
      return new GingerbreadBitmapFactory();
    }
  }
}
