/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.nativecode;

import android.annotation.TargetApi;
import android.graphics.Bitmap;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imageutils.BitmapUtil;

import java.nio.ByteBuffer;

/**
 * Utility methods for handling Bitmaps.
 */
@DoNotStrip
public class Bitmaps {

  static {
    SoLoaderShim.loadLibrary("bitmaps");
  }

  /**
   * Pin the bitmap so that it cannot be 'purged'. Only makes sense for purgeable bitmaps
   * WARNING: Use with caution. Make sure that the pinned bitmap is recycled eventually. Otherwise,
   * this will simply eat up ashmem memory and eventually lead to unfortunate crashes.
   * We *may* eventually provide an unpin method - but we don't yet have a compelling use case for
   * that.
   * @param bitmap the purgeable bitmap to pin
   */
  public static void pinBitmap(Bitmap bitmap) {
    Preconditions.checkNotNull(bitmap);
    nativePinBitmap(bitmap);
  }

  public static ByteBuffer getByteBuffer(Bitmap bitmap, long start, long size) {
    Preconditions.checkNotNull(bitmap);
    return nativeGetByteBuffer(bitmap, start, size);
  }

  public static void releaseByteBuffer(Bitmap bitmap) {
    Preconditions.checkNotNull(bitmap);
    nativeReleaseByteBuffer(bitmap);
  }

  /**
   * This blits the pixel data from src to dest.
   * <p>The destination bitmap must have both a height and a width equal to the source. For maximum
   * speed stride should be equal as well.
   * <p>Both bitmaps must use the same {@link android.graphics.Bitmap.Config} format.
   * <p>If the src is purgeable, it will be decoded as part of this operation if it was purged.
   * The dest should not be purgeable. If it is, the copy will still take place,
   * but will be lost the next time the dest gets purged, without warning.
   * <p>The dest must be mutable.
   * @param dest Bitmap to copy into
   * @param src Bitmap to copy out of
   */
  public static void copyBitmap(Bitmap dest, Bitmap src) {
    Preconditions.checkArgument(src.getConfig() == dest.getConfig());
    Preconditions.checkArgument(dest.isMutable());
    Preconditions.checkArgument(dest.getWidth() == src.getWidth());
    Preconditions.checkArgument(dest.getHeight() == src.getHeight());
    nativeCopyBitmap(
        dest,
        dest.getRowBytes(),
        src,
        src.getRowBytes(),
        dest.getHeight());
  }

  /**
   * Reconfigures bitmap after checking its allocation size.
   *
   * <p> This method is here to overcome our testing framework limit. Robolectric does not provide
   * KitKat specific APIs: {@link Bitmap#reconfigure} and {@link Bitmap#getAllocationByteCount}
   * are part of that.
   */
  @TargetApi(19)
  public static void reconfigureBitmap(
      Bitmap bitmap,
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    Preconditions.checkArgument(
        bitmap.getAllocationByteCount() >=
            width * height * BitmapUtil.getPixelSizeForBitmapConfig(bitmapConfig));
    bitmap.reconfigure(width, height, bitmapConfig);
  }

  @DoNotStrip
  private static native ByteBuffer nativeGetByteBuffer(Bitmap bitmap, long start, long size);

  @DoNotStrip
  private static native void nativePinBitmap(Bitmap bitmap);

  @DoNotStrip
  private static native void nativeReleaseByteBuffer(Bitmap bitmap);

  @DoNotStrip
  private static native void nativeCopyBitmap(
      Bitmap dest,
      int destStride,
      Bitmap src,
      int srcStride,
      int rows);
}
