/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.common;

import javax.annotation.concurrent.Immutable;

import java.util.Locale;

import com.facebook.common.internal.Preconditions;

/**
 * An optional part of image requests which limits the range of bytes which should be requested from
 * the network.
 *
 * <p> This might be useful because you want to limit the maximum size of a specific download or it
 * might be that you already have part of the image data.
 *
 * <p> Even without explicitly including the variants, by using the same media ID across multiple
 * image requests, the pipeline may build up a knowledge of these and fulfil requests accordingly.
 *
 * <p> This extra functionality is currently only enabled if
 * {@code ImagePipelineExperiments.isPartialImageCachingEnabled()} is true in the image pipeline
 * config. It is also dependent on a {@code NetworkFetcher} which applies the range using the HTTP
 * "Range" header.
 */
@Immutable
public class BytesRange {

  private static final int UNSPECIFIED_END = Integer.MIN_VALUE;

  private final int from;
  private final int to;

  private BytesRange(int from, int to) {
    this.from = from;
    this.to = to;
  }

  public String toHttpRangeHeaderValue() {
    return String.format((Locale) null, "bytes=%s-%s", valueOrEmpty(from), valueOrEmpty(to));
  }

  @Override
  public String toString() {
    return String.format((Locale) null, "%s-%s", valueOrEmpty(from), valueOrEmpty(to));
  }

  private static String valueOrEmpty(int n) {
    if (n == UNSPECIFIED_END) {
      return "";
    }
    return Integer.toString(n);
  }

  /**
   * Specifies a bytes range to request only the bytes from a specified index up to the end of the
   * data.
   *
   * @param from the first byte to request, must be positive or zero
   */
  public static BytesRange from(int from) {
    Preconditions.checkArgument(from >= 0);
    return new BytesRange(from, UNSPECIFIED_END);
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
}
