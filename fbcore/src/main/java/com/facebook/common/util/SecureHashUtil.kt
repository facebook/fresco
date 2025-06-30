/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util

import android.util.Base64
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/** Static methods for secure hashing. */
object SecureHashUtil {

  @JvmStatic
  fun makeSHA1Hash(text: String): String {
    try {
      return makeSHA1Hash(text.toByteArray(charset("utf-8")))
    } catch (e: UnsupportedEncodingException) {
      throw RuntimeException(e)
    }
  }

  @JvmStatic fun makeSHA1Hash(bytes: ByteArray): String = makeHash(bytes, "SHA-1")

  @JvmStatic fun makeSHA256Hash(bytes: ByteArray): String = makeHash(bytes, "SHA-256")

  @JvmStatic
  fun makeSHA1HashBase64(bytes: ByteArray): String {
    try {
      val md = MessageDigest.getInstance("SHA-1")
      md.update(bytes, 0, bytes.size)
      val sha1hash = md.digest()
      return Base64.encodeToString(sha1hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException(e)
    }
  }

  @JvmStatic
  fun makeMD5Hash(text: String): String {
    try {
      return makeMD5Hash(text.toByteArray(charset("utf-8")))
    } catch (e: UnsupportedEncodingException) {
      throw RuntimeException(e)
    }
  }

  @JvmStatic fun makeMD5Hash(bytes: ByteArray): String = makeHash(bytes, "MD5")

  @JvmStatic
  @Throws(IOException::class)
  fun makeMD5Hash(stream: InputStream): String = makeHash(stream, "MD5")

  @JvmField
  val HEX_CHAR_TABLE: ByteArray =
      byteArrayOf(
          '0'.code.toByte(),
          '1'.code.toByte(),
          '2'.code.toByte(),
          '3'.code.toByte(),
          '4'.code.toByte(),
          '5'.code.toByte(),
          '6'.code.toByte(),
          '7'.code.toByte(),
          '8'.code.toByte(),
          '9'.code.toByte(),
          'a'.code.toByte(),
          'b'.code.toByte(),
          'c'.code.toByte(),
          'd'.code.toByte(),
          'e'.code.toByte(),
          'f'.code.toByte())

  @JvmStatic
  @Throws(UnsupportedEncodingException::class)
  fun convertToHex(raw: ByteArray): String {
    val sb = StringBuilder(raw.size)
    for (b in raw) {
      val v = b.toInt() and 0xFF
      sb.append(Char(HEX_CHAR_TABLE[v ushr 4].toUShort()))
      sb.append(Char(HEX_CHAR_TABLE[v and 0xF].toUShort()))
    }
    return sb.toString()
  }

  private fun makeHash(bytes: ByteArray, algorithm: String): String {
    try {
      val md = MessageDigest.getInstance(algorithm)
      md.update(bytes, 0, bytes.size)
      val hash = md.digest()
      return convertToHex(hash)
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException(e)
    } catch (e: UnsupportedEncodingException) {
      throw RuntimeException(e)
    }
  }

  private const val BUFFER_SIZE = 4_096

  @Throws(IOException::class)
  private fun makeHash(stream: InputStream, algorithm: String): String {
    try {
      val md = MessageDigest.getInstance(algorithm)
      val buffer = ByteArray(BUFFER_SIZE)
      var read: Int
      while ((stream.read(buffer).also { read = it }) > 0) {
        md.update(buffer, 0, read)
      }
      val hash = md.digest()
      return convertToHex(hash)
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException(e)
    } catch (e: UnsupportedEncodingException) {
      throw RuntimeException(e)
    }
  }
}
