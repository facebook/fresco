/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat

import kotlin.jvm.JvmField

/** Class representing all used image formats. */
data class ImageFormat(
    val name: String,
    /**
     * Get the default file extension for the given image format.
     *
     * @return file extension for the image format
     */
    val fileExtension: String?
) {
  interface FormatChecker {
    /**
     * Get the number of header bytes the format checker requires
     *
     * @return the number of header bytes needed
     */
    val headerSize: Int

    /**
     * Returns an [ImageFormat] if the checker is able to determine the format or null otherwise.
     *
     * @param headerBytes the header bytes to check
     * @param headerSize the size of the header in bytes
     * @return the image format (ImageFormat.UNKNOWN if unknown)
     */
    fun determineFormat(headerBytes: ByteArray, headerSize: Int): ImageFormat
  }

  override fun toString(): String {
    return name
  }

  companion object {
    // Unknown image format
    @JvmField val UNKNOWN: ImageFormat = ImageFormat("UNKNOWN", null)
  }
}
