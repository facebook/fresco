/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

/*
 * Based on Android source com.android.providers.contacts.Hex
 */
package com.facebook.common.util

/**
 * Basic hex operations: from byte array to string and vice versa.
 *
 * TODO: move to the framework and consider implementing as native code.
 */
object Hex {

  private val HEX_DIGITS =
      charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

  private val FIRST_CHAR = CharArray(256)
  private val SECOND_CHAR = CharArray(256)

  init {
    for (i in 0..255) {
      FIRST_CHAR[i] = HEX_DIGITS[(i shr 4) and 0xF]
      SECOND_CHAR[i] = HEX_DIGITS[i and 0xF]
    }
  }

  private val DIGITS = ByteArray('f'.code + 1)

  init {
    run {
      var i = 0
      while (i <= 'F'.code) {
        DIGITS[i] = -1
        i++
      }
    }
    for (i in 0..9) {
      DIGITS['0'.code.toByte() + i] = i.toByte()
    }
    for (i in 0..5) {
      DIGITS['A'.code.toByte() + i] = (10 + i).toByte()
      DIGITS['a'.code.toByte() + i] = (10 + i).toByte()
    }
  }

  /**
   * Convert an int [0-255] to a hexadecimal string representation.
   *
   * @param value int value.
   */
  @JvmStatic
  fun byte2Hex(value: Int): String {
    require(!(value > 255 || value < 0)) { "The int converting to hex should be in range 0~255" }
    return FIRST_CHAR[value].toString() + SECOND_CHAR[value].toString()
  }

  /**
   * Quickly converts a byte array to a hexadecimal string representation.
   *
   * @param array byte array, possibly zero-terminated.
   */
  @JvmStatic
  fun encodeHex(array: ByteArray, zeroTerminated: Boolean): String {
    val cArray = CharArray(array.size * 2)

    var j = 0
    for (i in array.indices) {
      val index = array[i].toInt() and 0xFF
      if (index == 0 && zeroTerminated) {
        break
      }

      cArray[j++] = FIRST_CHAR[index]
      cArray[j++] = SECOND_CHAR[index]
    }

    return String(cArray, 0, j)
  }

  /** Quickly converts a hexadecimal string to a byte array. */
  @JvmStatic
  fun decodeHex(hexString: String): ByteArray {
    val length = hexString.length

    require((length and 0x01) == 0) { "Odd number of characters." }

    var badHex = false
    val out = ByteArray(length shr 1)
    var i = 0
    var j = 0
    while (j < length) {
      val c1 = hexString[j++].code
      if (c1 > 'f'.code) {
        badHex = true
        break
      }

      val d1 = DIGITS[c1]
      if (d1.toInt() == -1) {
        badHex = true
        break
      }

      val c2 = hexString[j++].code
      if (c2 > 'f'.code) {
        badHex = true
        break
      }

      val d2 = DIGITS[c2]
      if (d2.toInt() == -1) {
        badHex = true
        break
      }

      out[i] = (d1.toInt() shl 4 or d2.toInt()).toByte()
      i++
    }

    require(!badHex) { "Invalid hexadecimal digit: $hexString" }

    return out
  }

  @JvmStatic
  fun hexStringToByteArray(s: String): ByteArray {
    val noSpaceString = s.replace(" ".toRegex(), "")
    val data = decodeHex(noSpaceString)
    return data
  }
}
