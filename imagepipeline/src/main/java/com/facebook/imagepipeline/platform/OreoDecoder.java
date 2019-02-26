/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.util.Pools.SynchronizedPool;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imageutils.BitmapUtil;
import javax.annotation.concurrent.ThreadSafe;

/** Bitmap decoder for ART VM (Android O and up). */
@TargetApi(Build.VERSION_CODES.O)
@ThreadSafe
public class OreoDecoder extends DefaultDecoder {

  public OreoDecoder(BitmapPool bitmapPool, int maxNumThreads, SynchronizedPool decodeBuffers) {
    super(bitmapPool, maxNumThreads, decodeBuffers);
  }

  @Override
  public int getBitmapSize(final int width, final int height, final BitmapFactory.Options options) {
    // If the color is wide gamut but the Bitmap Config doesn't use 8 bytes per pixel, the size of
    // the bitmap
    // needs to be computed manually to get the correct size.
    return hasColorGamutMismatch(options)
        ? width * height * 8
        : BitmapUtil.getSizeInByteForBitmap(width, height, options.inPreferredConfig);
  }

  /** Check if the color space has a wide color gamut and is consistent with the Bitmap config */
  private static boolean hasColorGamutMismatch(final BitmapFactory.Options options) {
    return options.outColorSpace != null
        && options.outColorSpace.isWideGamut()
        && options.inPreferredConfig != Bitmap.Config.RGBA_F16;
  }
}
