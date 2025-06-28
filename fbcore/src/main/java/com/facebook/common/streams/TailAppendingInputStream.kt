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

/**
 * InputStream that returns all bytes from another stream, then appends the specified 'tail' bytes.
 */
class TailAppendingInputStream(inputStream: InputStream, tail: ByteArray) :
    FilterInputStream(inputStream) {

  private val tail: ByteArray
  private var tailOffset = 0
  private var markedTailOffset = 0

  init {
    if (inputStream == null) {
      throw NullPointerException()
    }
    if (tail == null) {
      throw NullPointerException()
    }
    this.tail = tail
  }

  @Throws(IOException::class)
  override fun read(): Int {
    val readResult = `in`.read()
    if (readResult != -1) {
      return readResult
    }
    return readNextTailByte()
  }

  @Throws(IOException::class)
  override fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)

  @Throws(IOException::class)
  override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
    val readResult = `in`.read(buffer, offset, count)
    if (readResult != -1) {
      return readResult
    }

    if (count == 0) {
      return 0
    }

    var bytesRead = 0
    while (bytesRead < count) {
      val nextByte = readNextTailByte()
      if (nextByte == -1) {
        break
      }
      buffer[offset + bytesRead] = nextByte.toByte()
      bytesRead++
    }
    return if (bytesRead > 0) bytesRead else -1
  }

  @Throws(IOException::class)
  override fun reset() {
    if (`in`.markSupported()) {
      `in`.reset()
      tailOffset = markedTailOffset
    } else {
      throw IOException("mark is not supported")
    }
  }

  override fun mark(readLimit: Int) {
    if (`in`.markSupported()) {
      super.mark(readLimit)
      markedTailOffset = tailOffset
    }
  }

  private fun readNextTailByte(): Int {
    if (tailOffset >= tail.size) {
      return -1
    }
    return (tail[tailOffset++].toInt()) and 0xFF
  }
}
