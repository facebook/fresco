/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat

import com.facebook.common.internal.ByteStreams
import com.facebook.common.internal.Closeables
import com.facebook.common.internal.Throwables
import com.facebook.imageformat.ImageFormat.FormatChecker
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** Detects the format of an encoded image. */
class ImageFormatChecker private constructor() {

  private var maxHeaderLength = 0
  private var customImageFormatCheckers: List<FormatChecker>? = null
  private val defaultFormatChecker = DefaultImageFormatChecker()
  private var binaryXmlEnabled = false

  init {
    updateMaxHeaderLength()
  }

  fun setCustomImageFormatCheckers(
      customImageFormatCheckers: List<FormatChecker>?
  ): ImageFormatChecker {
    this.customImageFormatCheckers = customImageFormatCheckers
    updateMaxHeaderLength()
    return this
  }

  fun setBinaryXmlEnabled(binaryXmlEnabled: Boolean): ImageFormatChecker {
    this.binaryXmlEnabled = binaryXmlEnabled
    return this
  }

  @Throws(IOException::class)
  fun determineImageFormat(`is`: InputStream): ImageFormat {
    val imageHeaderBytes = ByteArray(maxHeaderLength)
    val headerSize = readHeaderFromStream(maxHeaderLength, `is`, imageHeaderBytes)
    val format =
        defaultFormatChecker.determineFormat(imageHeaderBytes, headerSize).run {
          // Temporary block until binary XML support is fully enabled
          if (this == DefaultImageFormats.BINARY_XML && !binaryXmlEnabled) {
            ImageFormat.UNKNOWN
          } else {
            this
          }
        }
    if (format !== ImageFormat.UNKNOWN) {
      return format
    }
    customImageFormatCheckers?.forEach { formatChecker ->
      val format = formatChecker.determineFormat(imageHeaderBytes, headerSize)
      if (format !== ImageFormat.UNKNOWN) {
        return format
      }
    }
    return ImageFormat.UNKNOWN
  }

  private fun updateMaxHeaderLength() {
    maxHeaderLength = defaultFormatChecker.headerSize
    if (customImageFormatCheckers != null) {
      for (checker in customImageFormatCheckers!!) {
        maxHeaderLength = Math.max(maxHeaderLength, checker.headerSize)
      }
    }
  }

  companion object {
    /**
     * Reads up to maxHeaderLength bytes from is InputStream. If mark is supported by is, it is used
     * to restore content of the stream after appropriate amount of data is read. Read bytes are
     * stored in imageHeaderBytes, which should be capable of storing maxHeaderLength bytes.
     *
     * @param maxHeaderLength the maximum header length
     * @param is
     * @param imageHeaderBytes
     * @return number of bytes read from is
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun readHeaderFromStream(
        maxHeaderLength: Int,
        `is`: InputStream,
        imageHeaderBytes: ByteArray
    ): Int {
      check(imageHeaderBytes.size >= maxHeaderLength)

      // If mark is supported by the stream, use it to let the owner of the stream re-read the same
      // data. Otherwise, just consume some data.
      return if (`is`.markSupported()) {
        try {
          `is`.mark(maxHeaderLength)
          ByteStreams.read(`is`, imageHeaderBytes, 0, maxHeaderLength)
        } finally {
          `is`.reset()
        }
      } else {
        ByteStreams.read(`is`, imageHeaderBytes, 0, maxHeaderLength)
      }
    }

    /**
     * Get the currently used instance of the image format checker
     *
     * @return the image format checker to use
     */
    @get:JvmStatic
    val instance: ImageFormatChecker by
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ImageFormatChecker() }

    /**
     * Tries to read up to MAX_HEADER_LENGTH bytes from InputStream is and use read bytes to
     * determine type of the image contained in is. If provided input stream does not support mark,
     * then this method consumes data from is and it is not safe to read further bytes from is after
     * this method returns. Otherwise, if mark is supported, it will be used to preserve original
     * content of is.
     *
     * @param is
     * @return ImageFormat matching content of is InputStream or UNKNOWN if no type is suitable
     * @throws IOException if exception happens during read
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getImageFormat(`is`: InputStream): ImageFormat = instance.determineImageFormat(`is`)

    /*
     * A variant of getImageFormat that wraps IOException with RuntimeException.
     * This relieves clients of implementing dummy rethrow try-catch block.
     */
    @JvmStatic
    fun getImageFormat_WrapIOException(`is`: InputStream): ImageFormat =
        try {
          getImageFormat(`is`)
        } catch (ioe: IOException) {
          throw Throwables.propagate(ioe)
        }

    /**
     * Reads image header from a file indicated by provided filename and determines its format. This
     * method does not throw IOException if one occurs. In this case, [ImageFormat#UNKNOWN] will be
     * returned.
     *
     * @param filename
     * @return ImageFormat for image stored in filename
     */
    @JvmStatic
    fun getImageFormat(filename: String?): ImageFormat {
      var fileInputStream: FileInputStream? = null
      return try {
        fileInputStream = FileInputStream(filename)
        getImageFormat(fileInputStream)
      } catch (ioe: IOException) {
        ImageFormat.Companion.UNKNOWN
      } finally {
        Closeables.closeQuietly(fileInputStream)
      }
    }
  }
}
