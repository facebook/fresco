/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils

import java.io.IOException
import java.io.InputStream

/** This class contains utility method in order to manage the WebP format metadata */
object WebpUtil {

  /** Header for VP8 (lossy WebP). Take care of the space into the String */
  private const val VP8_HEADER: String = "VP8 "
  /** Header for Lossless WebP images */
  private const val VP8L_HEADER: String = "VP8L"
  /** Header for WebP enhanced */
  private const val VP8X_HEADER: String = "VP8X"

  /**
   * This method checks for the dimension of the WebP image from the given InputStream. We don't
   * support mark/reset and the Stream is always closed.
   *
   * @param stream The InputStream used for read WebP data
   * @return The Size of the WebP image if any or null if the size is not available
   */
  @JvmStatic
  fun getSize(stream: InputStream): Pair<Int, Int>? {
    // Here we have to parse the WebP data skipping all the information which are not
    // the size
    val headerBuffer = ByteArray(4)
    try {
      stream.read(headerBuffer)
      // These must be RIFF
      if (!compare(headerBuffer, "RIFF")) {
        return null
      }
      // Next there's the file size
      getInt(stream)
      // Next the WEBP header
      stream.read(headerBuffer)
      if (!compare(headerBuffer, "WEBP")) {
        return null
      }
      // Now we can have different headers
      stream.read(headerBuffer)
      when (getHeader(headerBuffer)) {
        VP8_HEADER -> return getVP8Dimension(stream)
        VP8L_HEADER -> return getVP8LDimension(stream)
        VP8X_HEADER -> return getVP8XDimension(stream)
      }
    } catch (e: IOException) {
      e.printStackTrace()
    } finally {
      try {
        stream.close()
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
    // In this case we don't have the dimension
    return null
  }

  /**
   * We manage the Simple WebP case
   *
   * @param stream The InputStream we're reading
   * @return The dimensions if any
   * @throws IOException In case or error reading from the InputStream
   */
  @Throws(IOException::class)
  private fun getVP8Dimension(stream: InputStream): Pair<Int, Int>? {
    // We need to skip 7 bytes
    stream.skip(7)
    // And then check the signature
    val sign1 = stream.getNextByteAsInt()
    val sign2 = stream.getNextByteAsInt()
    val sign3 = stream.getNextByteAsInt()
    return if (sign1 != 0x9D || sign2 != 0x01 || sign3 != 0x2A) {
      // Signature error
      null
    } else {
      // We read the dimensions
      Pair(get2BytesAsInt(stream), get2BytesAsInt(stream))
    }
  }

  /**
   * We manage the Lossless WebP case
   *
   * @param stream The InputStream we're reading
   * @return The dimensions if any
   * @throws IOException In case or error reading from the InputStream
   */
  @Throws(IOException::class)
  private fun getVP8LDimension(stream: InputStream): Pair<Int, Int>? {
    // Skip 4 bytes
    getInt(stream)
    // We have a check here
    val check = stream.getNextByteAsInt()
    if (check != 0x2F) {
      return null
    }
    val data1 = stream.read() and 0xFF
    val data2 = stream.read() and 0xFF
    val data3 = stream.read() and 0xFF
    val data4 = stream.read() and 0xFF
    // In this case the bits for size are 14!!! The sizes are -1!!!
    val width = (data2 and 0x3F shl 8 or data1) + 1
    val height = (data4 and 0x0F shl 10 or (data3 shl 2) or (data2 and 0xC0 shr 6)) + 1
    return Pair(width, height)
  }

  /**
   * We manage the Extended WebP case
   *
   * @param stream The InputStream we're reading
   * @return The dimensions if any
   * @throws IOException In case or error reading from the InputStream
   */
  @Throws(IOException::class)
  private fun getVP8XDimension(stream: InputStream): Pair<Int, Int> {
    // We have to skip 8 bytes
    stream.skip(8)
    // Read 3 bytes for width and height
    return Pair(read3Bytes(stream) + 1, read3Bytes(stream) + 1)
  }

  /**
   * Compares some bytes with the text we're expecting
   *
   * @param what The bytes to compare
   * @param with The string those bytes should contains
   * @return True if they match and false otherwise
   */
  private fun compare(what: ByteArray, with: String): Boolean {
    if (what.size != with.length) {
      return false
    }
    return what.indices.none { i -> with[i].code.toByte() != what[i] }
  }

  private fun getHeader(header: ByteArray): String {
    val str = StringBuilder()
    for (i in header.indices) {
      str.append(Char(header[i].toUShort()))
    }
    return str.toString()
  }

  @Throws(IOException::class)
  private fun getInt(stream: InputStream): Int {
    val byte1 = stream.getNextByteAsInt()
    val byte2 = stream.getNextByteAsInt()
    val byte3 = stream.getNextByteAsInt()
    val byte4 = stream.getNextByteAsInt()
    return (byte4 shl 24) or (byte3 shl 16) or (byte2 shl 8) or byte1
  }

  @JvmStatic
  @Throws(IOException::class)
  fun get2BytesAsInt(stream: InputStream): Int {
    val byte1 = stream.getNextByteAsInt()
    val byte2 = stream.getNextByteAsInt()
    return (byte2 shl 8) or byte1
  }

  @Throws(IOException::class)
  private fun read3Bytes(stream: InputStream): Int {
    val byte1 = stream.getNextByteAsInt()
    val byte2 = stream.getNextByteAsInt()
    val byte3 = stream.getNextByteAsInt()
    return (byte3 shl 16) or (byte2 shl 8) or byte1
  }

  @Throws(IOException::class) private fun InputStream.getNextByteAsInt(): Int = read() and 0xFF
}
