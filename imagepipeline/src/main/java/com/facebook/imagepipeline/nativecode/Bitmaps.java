/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import android.graphics.Bitmap;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import java.nio.ByteBuffer;

/**
 * Utility methods for handling Bitmaps.
 *
 * <p> Native code used by this class is shipped as part of libimagepipeline.so
 */
@DoNotStrip
public class Bitmaps {

  static {
    ImagePipelineNativeLoader.load();
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

  @DoNotStrip
  private static native void nativeCopyBitmap(
      Bitmap dest,
      int destStride,
      Bitmap src,
      int srcStride,
      int rows);
}
