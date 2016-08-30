/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import javax.annotation.Nullable;

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
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    return createBitmap(width, height, bitmapConfig, null);
  }

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

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the Bitmap.Config used to create the Bitmap
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig,
      @Nullable Object callerContext) {
    CloseableReference<Bitmap> reference = createBitmapInternal(width, height, bitmapConfig);
    addBitmapReference(reference.get(), callerContext);
    return reference;
  }

  /**
   * Creates a bitmap of the specified width and height.
   * The bitmap will be created with the default ARGB_8888 configuration
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      @Nullable Object callerContext) {
    return createBitmap(width, height, Bitmap.Config.ARGB_8888, callerContext);
  }

  /**
   * Creates a bitmap of the specified width and height. This is intended for ImagePipeline's
   * internal use only.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the Bitmap.Config used to create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public abstract CloseableReference<Bitmap> createBitmapInternal(
      int width,
      int height,
      Bitmap.Config bitmapConfig);

  private static BitmapCreationObserver sBitmapCreationObserver;

  public void setCreationListener(final BitmapCreationObserver bitmapCreationObserver) {
    if (sBitmapCreationObserver == null) {
      sBitmapCreationObserver = bitmapCreationObserver;
    }
  }

  public void addBitmapReference(
      Bitmap bitmap,
      @Nullable Object callerContext) {
    if (sBitmapCreationObserver != null) {
      sBitmapCreationObserver.onBitmapCreated(bitmap, callerContext);
    }
  }

  /**
   * Observer that notifies external creation of bitmap using
   * {@link PlatformBitmapFactory#createBitmap(int, int)} or
   * {@link PlatformBitmapFactory#createBitmap(int, int, Bitmap.Config)}.
   */
  public interface BitmapCreationObserver {

    void onBitmapCreated(Bitmap bitmap, @Nullable Object callerContext);
  }
}
