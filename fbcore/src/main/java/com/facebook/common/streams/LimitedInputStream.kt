/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.streams

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/** Reads the wrapped InputStream only until a specified number of bytes, the 'limit' is reached. */
class LimitedInputStream(inputStream: InputStream, limit: Int) : FilterInputStream(inputStream) {

  private var bytesToRead: Int
  private var bytesToReadWhenMarked: Int

  init {
    if (inputStream == null) {
      throw NullPointerException()
    }
    require(limit >= 0) { "limit must be >= 0" }
    bytesToRead = limit
    bytesToReadWhenMarked = -1
  }

  @Throws(IOException::class)
  override fun read(): Int {
    if (bytesToRead == 0) {
      return -1
    }

    val readByte = `in`.read()
    if (readByte != -1) {
      bytesToRead--
    }

    return readByte
  }

  @Throws(IOException::class)
  override fun read(buffer: ByteArray, byteOffset: Int, byteCount: Int): Int {
    if (bytesToRead == 0) {
      return -1
    }

    val maxBytesToRead = min(byteCount, bytesToRead)
    val bytesRead = `in`.read(buffer, byteOffset, maxBytesToRead)
    if (bytesRead > 0) {
      bytesToRead -= bytesRead
    }

    return bytesRead
  }

  @Throws(IOException::class)
  override fun skip(byteCount: Long): Long {
    val maxBytesToSkip = min(byteCount, bytesToRead.toLong())
    val bytesSkipped = `in`.skip(maxBytesToSkip)
    bytesToRead -= bytesSkipped.toInt()
    return bytesSkipped
  }

  @Throws(IOException::class) override fun available(): Int = min(`in`.available(), bytesToRead)

  override fun mark(readLimit: Int) {
    if (`in`.markSupported()) {
      `in`.mark(readLimit)
      bytesToReadWhenMarked = bytesToRead
    }
  }

  @Throws(IOException::class)
  override fun reset() {
    if (!`in`.markSupported()) {
      throw IOException("mark is not supported")
    }

    if (bytesToReadWhenMarked == -1) {
      throw IOException("mark not set")
    }

    `in`.reset()
    bytesToRead = bytesToReadWhenMarked
  }
}
