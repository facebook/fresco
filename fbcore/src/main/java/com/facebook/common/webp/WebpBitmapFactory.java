/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.webp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import com.facebook.infer.annotation.Nullsafe;
import java.io.FileDescriptor;
import java.io.InputStream;
import javax.annotation.Nullable;

/**
 * Interface for a bitmap factory that can decode WebP images even on versions of Android that don't
 * support it.
 *
 * <p>Implementation is found in the optional static-webp library. To use, add the following to your
 * build.gradle file: <code>implementation 'com.facebook.fresco:static-webp:${FRESCO_VERSION}'
 * </code>
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface WebpBitmapFactory {

  /** We listen to events in Webp direct decoding */
  interface WebpErrorLogger {

    /**
     * Invoked to notify the logger about an error
     *
     * @param message The message to log
     * @param extra Extra message if any
     */
    void onWebpErrorLog(String message, @Nullable String extra);
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

  @Nullable
  Bitmap decodeFileDescriptor(
      FileDescriptor fd, @Nullable Rect outPadding, @Nullable BitmapFactory.Options opts);

  @Nullable
  Bitmap decodeStream(
      InputStream inputStream, @Nullable Rect outPadding, @Nullable BitmapFactory.Options opts);

  @Nullable
  Bitmap decodeFile(String pathName, @Nullable BitmapFactory.Options opts);

  @Nullable
  Bitmap decodeByteArray(
      byte[] array, int offset, int length, @Nullable BitmapFactory.Options opts);
}
