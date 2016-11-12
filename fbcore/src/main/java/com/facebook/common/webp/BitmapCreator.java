/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.common.webp;

import android.graphics.Bitmap;

/**
 * This is a utility class we use in order to allocate a Bitmap that will be wrapped later
 * with a CloseableReference
 */
public interface BitmapCreator {

  /**
   * This creates a Bitmap with will be then wrapped with a CloseableReference
   *
   * @param width The width of the image
   * @param height The height of the image
   * @param bitmapConfig The Config object to use
   * @return The Bitmap
   */
  Bitmap createNakedBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig);
}
