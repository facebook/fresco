/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import javax.annotation.concurrent.ThreadSafe;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.util.Pools.SynchronizedPool;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.streams.LimitedInputStream;
import com.facebook.common.streams.TailAppendingInputStream;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.imageutils.JfifUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;

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
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    int sizeInBytes = BitmapUtil.getSizeInByteForBitmap(width, height, bitmapConfig);
    Bitmap bitmap = mBitmapPool.get(sizeInBytes);
    Bitmaps.reconfigureBitmap(bitmap, width, height, bitmapConfig);
    return CloseableReference.of(bitmap, mBitmapPool);
  }
}
