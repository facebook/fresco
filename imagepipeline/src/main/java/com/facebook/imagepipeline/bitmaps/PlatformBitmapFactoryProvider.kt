/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps

import android.os.Build
import com.facebook.imagepipeline.core.CloseableReferenceFactory
import com.facebook.imagepipeline.memory.PoolFactory
import com.facebook.imagepipeline.platform.PlatformDecoder

object PlatformBitmapFactoryProvider {
  /**
   * Provide the implementation of the PlatformBitmapFactory for the current platform using the
   * provided PoolFactory
   *
   * @param poolFactory The PoolFactory
   * @param platformDecoder The PlatformDecoder
   * @return The PlatformBitmapFactory implementation
   */
  @JvmStatic
  fun buildPlatformBitmapFactory(
      poolFactory: PoolFactory,
      platformDecoder: PlatformDecoder,
      closeableReferenceFactory: CloseableReferenceFactory
  ): PlatformBitmapFactory =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ArtBitmapFactory(poolFactory.bitmapPool, closeableReferenceFactory)
      } else {
        HoneycombBitmapFactory(
            EmptyJpegGenerator(poolFactory.pooledByteBufferFactory),
            platformDecoder,
            closeableReferenceFactory)
      }
}
