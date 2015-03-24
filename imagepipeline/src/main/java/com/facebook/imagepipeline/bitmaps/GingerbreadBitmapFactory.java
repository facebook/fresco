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
import com.facebook.common.references.ResourceReleaser;

/**
 * Bitmap factory for Gingerbread.
 */
public class GingerbreadBitmapFactory {

  private final ResourceReleaser<Bitmap> mBitmapResourceReleaser;

  public GingerbreadBitmapFactory() {
    mBitmapResourceReleaser = new ResourceReleaser<Bitmap>() {
      @Override
      public void release(Bitmap value) {
        value.recycle();
      }
    };
  }

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @return a reference to the bitmap
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  CloseableReference<Bitmap> createBitmap(int width, int height) {
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    return CloseableReference.of(bitmap, mBitmapResourceReleaser);
  }
}
