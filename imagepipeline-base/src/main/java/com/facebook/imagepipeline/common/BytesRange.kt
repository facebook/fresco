/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common

import com.facebook.common.internal.Preconditions
import java.util.Locale
import java.util.regex.Pattern
import javax.annotation.concurrent.Immutable

/**
 * A representation of the range of bytes requested or contained in a piece of content.
 *
 * This is based on the spec of the HTTP "Content-Range" header so includes methods to parse and
 * output appropriate strings for the header.
 *
 * The header spec is at https://tools.ietf.org/html/rfc2616#section-14.16
 *
 * As per that spec, the from and to values are inclusive. Requesting the first 100 bytes of an
 * image can be achieved by calling `BytesRange.toMax(99)`.
 *
 * This might be useful because you want to limit the maximum size of a specific download or it
 * might be that you already have part of the image data and only want the remainder.
 *
 * These objects are currently only respected in image requests and only taken from responses if
 * `ImagePipelineExperiments.isPartialImageCachingEnabled()` is true in the image pipeline config.
 * It is also dependent on a `NetworkFetcher` which writes and reads these headers.
 */
@Suppress("KtDataClass")
@Immutable
data class BytesRange(
    @JvmField
    /** The first byte of the range. Values begin at 0. */
    val from: Int,
    @JvmField
    /**
     * The final byte of the range inclusive, or [.TO_END_OF_CONTENT] if it reaches to the end.
     *
     * If not TO_END_OF_CONTENT, values are inclusive. e.g. for the first 100 bytes this is 99.
     */
    val to: Int
) {

  fun toHttpRangeHeaderValue(): String {
    return String.format(null as Locale?, "bytes=%s-%s", valueOrEmpty(from), valueOrEmpty(to))
  }

  /**
   * Checks whether a provided range is within this one.
   *
   * @return true if the provided range is within this one, false if given null
   */
  operator fun contains(compare: BytesRange?): Boolean {
    return if (compare == null) false else from <= compare.from && compare.to <= to
  }

  override fun toString(): String {
    return String.format(null as Locale?, "%s-%s", valueOrEmpty(from), valueOrEmpty(to))
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherRange: BytesRange = other as BytesRange

    return from == otherRange.from && to == otherRange.to
  }

  override fun hashCode(): Int {
    var result = from
    result = 31 * result + to
    return result
  }

  companion object {
    const val TO_END_OF_CONTENT: Int = Int.MAX_VALUE
    private val headerParsingRegEx: Pattern by lazy { Pattern.compile("[-/ ]") }

    private fun valueOrEmpty(n: Int): String {
      return if (n == TO_END_OF_CONTENT) "" else n.toString()
    }

    /**
     * Specifies a bytes range to request only the bytes from a specified index up to the end of the
     * data.
     *
     * @param from the first byte to request, must be positive or zero
     */
    @JvmStatic
    fun from(from: Int): BytesRange {
      Preconditions.checkArgument(from >= 0)
      return BytesRange(from, TO_END_OF_CONTENT)
    }

    /**
     * Specifies a bytes range to request no more than a specified number of bytes. If the encoded
     * image is less than the requested maximum you may still get the entire result.
     *
     * @param to the maximum byte to be requested, must be positive
     */
    @JvmStatic
    fun toMax(to: Int): BytesRange {
      Preconditions.checkArgument(to > 0)
      return BytesRange(0, to)
    }

    /**
     * Creates an instance of BytesRange by parsing the value of a returned HTTP "Content-Range"
     * header.
     *
     * If the range runs to the end of the available content, the end of the range will be set to
     * TO_END_OF_CONTENT.
     *
     * The header spec is at https://tools.ietf.org/html/rfc2616#section-14.16
     *
     * @param header
     * @return the parsed range
     * @throws IllegalArgumentException if the header is non-null but fails to match the format per
     *   the spec
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun fromContentRangeHeader(header: String?): BytesRange? {
      if (header == null) {
        return null
      }
      try {
        val headerParts = headerParsingRegEx.split(header)
        Preconditions.checkArgument(headerParts.size == 4)
        Preconditions.checkArgument(headerParts[0] == "bytes")
        val from = headerParts[1].toInt()
        val to = headerParts[2].toInt()
        val length = headerParts[3].toInt()
        Preconditions.checkArgument(to > from)
        Preconditions.checkArgument(length > to)
        if (to < length - 1) {
          return BytesRange(from, to)
        } else {
          return BytesRange(from, TO_END_OF_CONTENT)
        }
      } catch (x: IllegalArgumentException) {
        throw IllegalArgumentException(
            String.format(null as Locale?, "Invalid Content-Range header value: \"%s\"", header), x)
      }
    }
  }
}
