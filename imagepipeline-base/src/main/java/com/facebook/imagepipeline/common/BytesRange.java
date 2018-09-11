/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.util.HashCodeUtil;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A representation of the range of bytes requested or contained in a piece of content.
 *
 * <p> This is based on the spec of the HTTP "Content-Range" header so includes methods to parse and
 * output appropriate strings for the header.
 *
 * <p> The header spec is at https://tools.ietf.org/html/rfc2616#section-14.16
 *
 *  <p> As per that spec, the from and to values are inclusive. Requesting the first 100 bytes of an
 * image can be achieved by calling <code>BytesRange.toMax(99)</code>.
 *
 * <p> This might be useful because you want to limit the maximum size of a specific download or it
 * might be that you already have part of the image data and only want the remainder.
 *
 * <p> These objects are currently only respected in image requests and only taken from responses if
 * {@code ImagePipelineExperiments.isPartialImageCachingEnabled()} is true in the image pipeline
 * config. It is also dependent on a {@code NetworkFetcher} which writes and reads these headers.
 */
@Immutable
public class BytesRange {

  public static final int TO_END_OF_CONTENT = Integer.MAX_VALUE;

  private static @Nullable Pattern sHeaderParsingRegEx;

  /**
   * The first byte of the range. Values begin at 0.
   */
  public final int from;
  /**
   * The final byte of the range inclusive, or {@link #TO_END_OF_CONTENT} if it reaches to the end.
   *
   * <p> If not TO_END_OF_CONTENT, values are inclusive. e.g. for the first 100 bytes this is 99.
   */
  public final int to;

  public BytesRange(int from, int to) {
    this.from = from;
    this.to = to;
  }

  public String toHttpRangeHeaderValue() {
    return String.format((Locale) null, "bytes=%s-%s", valueOrEmpty(from), valueOrEmpty(to));
  }

  /**
   * Checks whether a provided range is within this one.
   *
   * @return true if the provided range is within this one, false if given null
   */
  public boolean contains(@Nullable BytesRange compare) {
    if (compare == null) {
      return false;
    }

    return from <= compare.from && to >= compare.to;
  }

  @Override
  public String toString() {
    return String.format((Locale) null, "%s-%s", valueOrEmpty(from), valueOrEmpty(to));
  }

  private static String valueOrEmpty(int n) {
    if (n == TO_END_OF_CONTENT) {
      return "";
    }
    return Integer.toString(n);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof BytesRange)) {
      return false;
    }
    final BytesRange that = (BytesRange) other;
    return this.from == that.from && this.to == that.to;
  }

  @Override
  public int hashCode() {
    return HashCodeUtil.hashCode(from, to);
  }

  /**
   * Specifies a bytes range to request only the bytes from a specified index up to the end of the
   * data.
   *
   * @param from the first byte to request, must be positive or zero
   */
  public static BytesRange from(int from) {
    Preconditions.checkArgument(from >= 0);
    return new BytesRange(from, TO_END_OF_CONTENT);
  }

  /**
   * Specifies a bytes range to request no more than a specified number of bytes. If the encoded
   * image is less than the requested maximum you may still get the entire result.
   *
   * @param to the maximum byte to be requested, must be positive
   */
  public static BytesRange toMax(int to) {
    Preconditions.checkArgument(to > 0);
    return new BytesRange(0, to);
  }

  /**
   * Creates an instance of BytesRange by parsing the value of a returned HTTP "Content-Range"
   * header.
   *
   * <p> If the range runs to the end of the available content, the end of the range will be set to
   * TO_END_OF_CONTENT.
   *
   * <p> The header spec is at https://tools.ietf.org/html/rfc2616#section-14.16
   *
   * @param header
   * @throws IllegalArgumentException if the header is non-null but fails to match the format per
   * the spec
   * @return the parsed range
   */
  @Nullable
  public static BytesRange fromContentRangeHeader(@Nullable String header)
      throws IllegalArgumentException {
    if (header == null) {
      return null;
    }

    if (sHeaderParsingRegEx == null) {
      sHeaderParsingRegEx = Pattern.compile("[-/ ]");
    }

    try {
      final String[] headerParts = sHeaderParsingRegEx.split(header);
      Preconditions.checkArgument(headerParts.length == 4);
      Preconditions.checkArgument(headerParts[0].equals("bytes"));

      final int from = Integer.parseInt(headerParts[1]);
      final int to = Integer.parseInt(headerParts[2]);
      final int length = Integer.parseInt(headerParts[3]);
      Preconditions.checkArgument(to > from);
      Preconditions.checkArgument(length > to);

      if (to < length - 1) {
        return new BytesRange(from, to);
      } else {
        return new BytesRange(from, TO_END_OF_CONTENT);
      }
    } catch (IllegalArgumentException x) {
      throw new IllegalArgumentException(
          String.format((Locale) null, "Invalid Content-Range header value: \"%s\"", header),
          x);
    }
  }
}
