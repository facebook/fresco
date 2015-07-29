/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imageutils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.annotation.Nullable;

/**
 * This class contains utility method for Bitmap
 */
public final class BitmapUtil {

  /**
   * @return size in bytes of the underlying bitmap
   */
  @SuppressLint("NewApi")
  public static int getSizeInBytes(@Nullable Bitmap bitmap) {
    if (bitmap == null) {
      return 0;
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return bitmap.getAllocationByteCount();
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      return bitmap.getByteCount();
    } else {
      // Estimate for earlier platforms.
      return bitmap.getWidth() * bitmap.getRowBytes();
    }
  }

  /**
   * Decodes only the bounds of an image and returns its width and height or null if the size can't
   * be determined
   * @param bytes the input byte array of the image
   * @return dimensions of the image
   */
  public static @Nullable Pair<Integer, Integer> decodeDimensions(byte[] bytes) {
    // wrapping with ByteArrayInputStream is cheap and we don't have duplicate implementation
    return decodeDimensions(new ByteArrayInputStream(bytes));
  }

  /**
   * Decodes only the bounds of an image and returns its width and height or null if the size can't
   * be determined
   * @param is the InputStream containing the image data
   * @return dimensions of the image
   */
  public static @Nullable Pair<Integer, Integer> decodeDimensions(InputStream is) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;

    BitmapFactory.decodeStream(is, null, options);
    return (options.outWidth == -1 || options.outHeight == -1) ?
        null : new Pair(options.outWidth, options.outHeight);
  }

}
