/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;

/**
 * Bitmap factory for Gingerbread.
 */
public class GingerbreadBitmapFactory extends PlatformBitmapFactory {

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   * used to create the decoded Bitmap
   * @return a reference to the bitmap
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> createBitmapInternal(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    Bitmap bitmap = Bitmap.createBitmap(width, height, bitmapConfig);
    return CloseableReference.of(bitmap, SimpleBitmapReleaser.getInstance());
  }
}
