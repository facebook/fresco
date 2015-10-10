/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import android.graphics.Bitmap;

import com.facebook.common.references.CloseableReference;

/**
 * Bitmap factory optimized for the platform.
 */
public abstract class PlatformBitmapFactory {

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the Bitmap.Config used to create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public abstract CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig);

  /**
   * Creates a bitmap of the specified width and height.
   * The bitmap will be created with the default ARGB_8888 configuration
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(int width, int height) {
    return createBitmap(width, height, Bitmap.Config.ARGB_8888);
  }
}
