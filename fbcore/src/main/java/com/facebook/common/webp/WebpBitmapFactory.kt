/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.webp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import java.io.FileDescriptor
import java.io.InputStream

/**
 * Interface for a bitmap factory that can decode WebP images even on versions of Android that don't
 * support it.
 *
 * Implementation is found in the optional static-webp library. To use, add the following to your
 * build.gradle file: `implementation 'com.facebook.fresco:static-webp:${FRESCO_VERSION}' ` *
 */
interface WebpBitmapFactory {

  /** We listen to events in Webp direct decoding */
  fun interface WebpErrorLogger {
    /**
     * Invoked to notify the logger about an error
     *
     * @param message The message to log
     * @param extra Extra message if any
     */
    fun onWebpErrorLog(message: String, extra: String?)
  }

  /**
   * Register the given listener as observer of error
   *
   * @param logger The WebpErrorLogger in order to observe webp errors
   */
  fun setWebpErrorLogger(logger: WebpErrorLogger)

  /**
   * Set the object which should create the bg Bitmap
   *
   * @param bitmapCreator The BitmapCreator implementation
   */
  fun setBitmapCreator(bitmapCreator: BitmapCreator)

  fun decodeFileDescriptor(
      fd: FileDescriptor,
      outPadding: Rect?,
      opts: BitmapFactory.Options?,
  ): Bitmap?

  fun decodeStream(
      inputStream: InputStream,
      outPadding: Rect?,
      opts: BitmapFactory.Options?,
  ): Bitmap?

  fun decodeFile(pathName: String, opts: BitmapFactory.Options?): Bitmap?

  fun decodeByteArray(
      array: ByteArray,
      offset: Int,
      length: Int,
      opts: BitmapFactory.Options?,
  ): Bitmap?
}
