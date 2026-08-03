/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util

import com.facebook.common.internal.ByteStreams
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Utility method for dealing with Streams. */
object StreamUtil {

  /**
   * Efficiently fetch bytes from InputStream is by delegating to getBytesFromStream(is,
   * is.available())
   */
  @JvmStatic
  @Throws(IOException::class)
  fun getBytesFromStream(`is`: InputStream): ByteArray = getBytesFromStream(`is`, `is`.available())

  /**
   * Efficiently fetch the bytes from the InputStream, provided that caller can guess exact numbers
   * of bytes that can be read from inputStream. Avoids one extra byte[] allocation that
   * ByteStreams.toByteArray() performs.
   *
   * @param hint - size of inputStream's content in bytes
   */
  @JvmStatic
  @Throws(IOException::class)
  fun getBytesFromStream(inputStream: InputStream, hint: Int): ByteArray {
    // Subclass ByteArrayOutputStream to avoid an extra byte[] allocation and copy
    val byteOutput: ByteArrayOutputStream =
        object : ByteArrayOutputStream(hint) {
          override fun toByteArray(): ByteArray =
              // Can only use the raw buffer directly if the size is equal to the array we have.
              // Otherwise we have no choice but to copy.
              if (count == buf.size) {
                buf
              } else {
                super.toByteArray()
              }
        }
    ByteStreams.copy(inputStream, byteOutput)
    return byteOutput.toByteArray()
  }

  /**
   * Skips exactly bytesCount bytes in inputStream unless end of stream is reached first.
   *
   * @param inputStream input stream to skip bytes from
   * @param bytesCount number of bytes to skip
   * @return number of skipped bytes
   * @throws IOException
   */
  @JvmStatic
  @Throws(IOException::class)
  fun skip(inputStream: InputStream, bytesCount: Long): Long {
    checkNotNull(inputStream)
    require(bytesCount >= 0)

    var toSkip = bytesCount
    while (toSkip > 0) {
      val skipped = inputStream.skip(toSkip)
      if (skipped > 0) {
        toSkip -= skipped
        continue
      }

      if (inputStream.read() != -1) {
        toSkip--
        continue
      }
      return bytesCount - toSkip
    }

    return bytesCount
  }
}
