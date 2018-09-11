/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imageutils.BitmapUtil;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Bitmap factory for ART VM (Lollipop and up).
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@ThreadSafe
public class ArtBitmapFactory extends PlatformBitmapFactory {

  private final BitmapPool mBitmapPool;

  public ArtBitmapFactory(BitmapPool bitmapPool) {
    mBitmapPool = bitmapPool;
  }

  /**
   * Creates a bitmap of the specified width and height.
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   * used to create the decoded Bitmap
   * @return a reference to the bitmap
   * @exception java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> createBitmapInternal(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    int sizeInBytes = BitmapUtil.getSizeInByteForBitmap(width, height, bitmapConfig);
    Bitmap bitmap = mBitmapPool.get(sizeInBytes);
    Preconditions.checkArgument(
        bitmap.getAllocationByteCount()
            >= width * height * BitmapUtil.getPixelSizeForBitmapConfig(bitmapConfig));
    bitmap.reconfigure(width, height, bitmapConfig);
    return CloseableReference.of(bitmap, mBitmapPool);
  }
}
