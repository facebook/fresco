/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import com.facebook.imageformat.ImageFormat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** The abstraction for WebpTranscoder */
interface WebpTranscoder {

  /** @return true if given type of WebP is supported natively by the framework */
  fun isWebpNativelySupported(webpFormat: ImageFormat): Boolean

  /** Transcodes webp image given by input stream into jpeg. */
  @Throws(IOException::class)
  fun transcodeWebpToJpeg(inputStream: InputStream, outputStream: OutputStream, quality: Int)

  /** Transcodes Webp image given by input stream into png. */
  @Throws(IOException::class)
  fun transcodeWebpToPng(inputStream: InputStream, outputStream: OutputStream)
}
