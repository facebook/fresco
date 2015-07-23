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
import android.os.Build;

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

}
