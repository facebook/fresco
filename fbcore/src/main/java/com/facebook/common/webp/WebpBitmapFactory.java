/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.webp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import java.io.FileDescriptor;
import java.io.InputStream;

/**
 * Interface for a bitmap factory that can decode WebP images even on versions of Android that
 * don't support it.
 * <p> Implementation is found in the optional static-webp library. To use, add the following to
 * your build.gradle file:
 * <code>implementation 'com.facebook.fresco:static-webp:${FRESCO_VERSION}'</code>
 */
public interface WebpBitmapFactory {

  /**
   * We listen to events in Webp direct decoding
   */
  interface WebpErrorLogger {

    /**
     * Invoked to notify the logger about an error
     *
     * @param message The message to log
     * @param extra Extra message if any
     */
    void onWebpErrorLog(String message, String extra);
  }

  /**
   * Register the given listener as observer of error
   *
   * @param logger The WebpErrorLogger in order to observe webp errors
   */
  void setWebpErrorLogger(WebpErrorLogger logger);

  /**
   * Set the object which should create the bg Bitmap
   *
   * @param bitmapCreator The BitmapCreator implementation
   */
  void setBitmapCreator(final BitmapCreator bitmapCreator);

  Bitmap decodeFileDescriptor(
      FileDescriptor fd,
      Rect outPadding,
      BitmapFactory.Options opts);

  Bitmap decodeStream(
      InputStream inputStream,
      Rect outPadding,
      BitmapFactory.Options opts);

  Bitmap decodeFile(
      String pathName,
      BitmapFactory.Options opts);

  Bitmap decodeByteArray(
      byte[] array,
      int offset,
      int length,
      BitmapFactory.Options opts);

}
